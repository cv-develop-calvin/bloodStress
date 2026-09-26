package org.bp.songbaobao.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bp.songbaobao.R
import org.bp.songbaobao.data.local.AppDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** 备份结果 */
data class BackupResult(
    val ok: Boolean,
    val message: String,
    val counts: String = ""
)

/** 导入模式 */
enum class ImportMode {
    /** 合并：保留原有数据，备份数据作为新记录追加 */
    MERGE,
    /** 覆盖：先清空全部数据再写入备份 */
    REPLACE
}

/**
 * 数据备份与恢复。
 *
 * 导出为单个 JSON 文件（含全部 6 张表），导入时按所选模式写入。
 * 用 org.json 手写序列化，不引入额外依赖。
 */
@Singleton
class BackupRepository @Inject constructor(
    private val db: AppDatabase
) {

    companion object {
        const val MIME = "application/json"
        private const val MAGIC = "songbaobao-backup"
        /** 备份格式版本，后续结构变更时用于兼容判断 */
        private const val FORMAT_VERSION = 1
    }

    /** 默认备份文件名，如 songbaobao-backup-2026-09-23-1530.json */
    fun defaultFileName(): String {
        val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"))
        return "songbaobao-backup-$ts.json"
    }

    // ---------------- 导出 ----------------

    /** 导出全部数据到指定 Uri（由 SAF 的 CreateDocument 提供） */
    suspend fun export(context: Context, target: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            val records = db.bpDao().allForExport()
            val meds = db.medDao().allForExport()
            val logs = db.medDao().allLogsForExport()
            val labs = db.labDao().allForExport()
            val notes = db.noteDao().allForExport()
            val photos = db.noteDao().allPhotosForExport()

            val root = JSONObject().apply {
                put("magic", MAGIC)
                put("version", FORMAT_VERSION)
                put(
                    "exportedAt",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                put("records", records.toJsonArray { it.toJson() })
                put("meds", meds.toJsonArray { it.toJson() })
                put("medLogs", logs.toJsonArray { it.toJson() })
                put("labReports", labs.toJsonArray { it.toJson() })
                put("notes", notes.toJsonArray { it.toJson() })
                put("notePhotos", photos.toJsonArray { it.toJson() })
            }

            val out = context.contentResolver.openOutputStream(target, "wt")
                ?: return@withContext BackupResult(
                    false, context.getString(R.string.msg_write_failed)
                )
            out.use {
                it.write(root.toString(2).toByteArray(Charsets.UTF_8))
                it.flush()
            }

            BackupResult(
                ok = true,
                message = context.getString(R.string.msg_export_ok),
                counts = countsText(
                    context,
                    records.size, meds.size, logs.size, labs.size, notes.size, photos.size
                )
            )
        } catch (t: Throwable) {
            BackupResult(
                false,
                context.getString(
                    R.string.msg_export_failed,
                    t.message ?: t.javaClass.simpleName
                )
            )
        }
    }

    // ---------------- 导入 ----------------

    suspend fun import(context: Context, source: Uri, mode: ImportMode): BackupResult =
        withContext(Dispatchers.IO) {
            try {
                val text = context.contentResolver.openInputStream(source)?.use {
                    it.readBytes().toString(Charsets.UTF_8)
                } ?: return@withContext BackupResult(
                    false, context.getString(R.string.msg_read_failed)
                )

                val root = JSONObject(text)
                if (root.optString("magic") != MAGIC) {
                    return@withContext BackupResult(
                        false, context.getString(R.string.msg_not_backup_file)
                    )
                }
                val ver = root.optInt("version", 0)
                if (ver > FORMAT_VERSION) {
                    return@withContext BackupResult(
                        false,
                        context.getString(
                            R.string.msg_backup_too_new, ver, FORMAT_VERSION
                        )
                    )
                }

                val records = root.optJSONArray("records").toBpList(mode)
                val meds = root.optJSONArray("meds").toMedList(mode)
                val logs = root.optJSONArray("medLogs").toMedLogList(mode)
                val labs = root.optJSONArray("labReports").toLabList(mode)
                val notes = root.optJSONArray("notes").toNoteList(mode)
                val photos = root.optJSONArray("notePhotos").toNotePhotoList(mode)

                // 事务保证「覆盖」模式不会写到一半失败。
                // 必须用 withTransaction：runInTransaction 的 lambda 不是挂起上下文。
                db.withTransaction {
                    if (mode == ImportMode.REPLACE) {
                        db.clearAllTables()
                    }
                    records.forEach { db.bpDao().insert(it) }
                    meds.forEach { db.medDao().insert(it) }
                    logs.forEach { db.medDao().insertLog(it) }
                    labs.forEach { db.labDao().insert(it) }
                    notes.forEach { db.noteDao().insert(it) }
                    photos.forEach { db.noteDao().insertPhoto(it) }
                }

                BackupResult(
                    ok = true,
                    message = context.getString(
                        if (mode == ImportMode.MERGE) R.string.msg_import_ok_merge
                        else R.string.msg_import_ok_replace
                    ),
                    counts = countsText(context, records.size, meds.size, logs.size, labs.size, notes.size, photos.size)
                )
            } catch (t: Throwable) {
                BackupResult(false, context.getString(R.string.msg_import_failed, t.message ?: t.javaClass.simpleName))
            }
        }

    private fun countsText(
        context: Context,
        r: Int, m: Int, l: Int, lab: Int, n: Int, p: Int
    ) = context.getString(R.string.msg_backup_summary, r, m, l, lab, n, p)

    /** 合并模式下把主键置 0，交给自增重新分配，避免与既有记录 id 冲突 */
    private fun Long.forMode(mode: ImportMode): Long = if (mode == ImportMode.MERGE) 0L else this

    private fun <T> List<T>.toJsonArray(build: (T) -> JSONObject): JSONArray =
        JSONArray().also { arr -> forEach { arr.put(build(it)) } }

    // ---------------- 读取辅助 ----------------

    private fun JSONArray?.items(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (isNull(key)) null else optInt(key)

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (isNull(key)) null else optDouble(key)

    private fun JSONObject.optStr(key: String, def: String = ""): String =
        if (isNull(key)) def else optString(key, def)

    private fun JSONObject.optBool(key: String, def: Boolean): Boolean =
        if (isNull(key)) def else optBoolean(key, def)

    private fun JSONObject.optLong(key: String, def: Long = 0L): Long =
        if (isNull(key)) def else optLong(key, def)

    // ---------------- 实体 → JSON ----------------

    private fun org.bp.songbaobao.data.local.entity.BpRecord.toJson() = JSONObject().apply {
        put("id", id); put("date", date); put("time", time)
        put("systolic", systolic); put("diastolic", diastolic)
        put("pulse", pulse ?: JSONObject.NULL); put("note", note); put("createdAt", createdAt)
    }

    private fun org.bp.songbaobao.data.local.entity.Medication.toJson() = JSONObject().apply {
        put("id", id); put("name", name); put("dosage", dosage); put("unit", unit)
        put("freq", freq); put("times", times); put("startDate", startDate)
        put("endDate", endDate); put("note", note); put("active", active); put("createdAt", createdAt)
    }

    private fun org.bp.songbaobao.data.local.entity.MedLog.toJson() = JSONObject().apply {
        put("id", id); put("medId", medId); put("date", date); put("time", time)
        put("status", status); put("note", note); put("createdAt", createdAt)
    }

    private fun org.bp.songbaobao.data.local.entity.LabReport.toJson() = JSONObject().apply {
        put("id", id); put("date", date); put("time", time)
        put("hospital", hospital); put("source", source); put("photo", photo)
        put("rawText", rawText); put("note", note); put("createdAt", createdAt)
        values().forEach { (k, v) -> put(k, v ?: JSONObject.NULL) }
    }

    private fun org.bp.songbaobao.data.local.entity.Note.toJson() = JSONObject().apply {
        put("id", id); put("date", date); put("time", time); put("title", title)
        put("content", content); put("mood", mood); put("tags", tags)
        put("createdAt", createdAt); put("updatedAt", updatedAt)
    }

    private fun org.bp.songbaobao.data.local.entity.NotePhoto.toJson() = JSONObject().apply {
        put("id", id); put("noteId", noteId); put("filename", filename)
        put("thumb", thumb); put("caption", caption); put("size", size); put("createdAt", createdAt)
    }

    // ---------------- JSON → 实体 ----------------

    private fun JSONArray?.toBpList(mode: ImportMode) = items().map {
        org.bp.songbaobao.data.local.entity.BpRecord(
            id = it.optLong("id").forMode(mode),
            date = it.optStr("date"),
            time = it.optStr("time"),
            systolic = it.optInt("systolic"),
            diastolic = it.optInt("diastolic"),
            pulse = it.optIntOrNull("pulse"),
            note = it.optStr("note"),
            createdAt = it.optStr("createdAt")
        )
    }

    private fun JSONArray?.toMedList(mode: ImportMode) = items().map {
        org.bp.songbaobao.data.local.entity.Medication(
            id = it.optLong("id").forMode(mode),
            name = it.optStr("name"),
            dosage = it.optStr("dosage"),
            unit = it.optStr("unit"),
            freq = it.optStr("freq"),
            times = it.optStr("times"),
            startDate = it.optStr("startDate"),
            endDate = it.optStr("endDate"),
            note = it.optStr("note"),
            active = it.optBool("active", true),
            createdAt = it.optStr("createdAt")
        )
    }

    private fun JSONArray?.toMedLogList(mode: ImportMode) = items().map {
        org.bp.songbaobao.data.local.entity.MedLog(
            id = it.optLong("id").forMode(mode),
            medId = it.optLong("medId"),
            date = it.optStr("date"),
            time = it.optStr("time"),
            status = it.optStr("status", "taken"),
            note = it.optStr("note"),
            createdAt = it.optStr("createdAt")
        )
    }

    private fun JSONArray?.toLabList(mode: ImportMode) = items().map {
        org.bp.songbaobao.data.local.entity.LabReport(
            id = it.optLong("id").forMode(mode),
            date = it.optStr("date"),
            time = it.optStr("time"),
            hospital = it.optStr("hospital"),
            source = it.optStr("source", "manual"),
            photo = it.optStr("photo"),
            rawText = it.optStr("rawText"),
            note = it.optStr("note"),
            createdAt = it.optStr("createdAt"),
            wbc = it.optDoubleOrNull("wbc"),
            rbc = it.optDoubleOrNull("rbc"),
            hgb = it.optDoubleOrNull("hgb"),
            hct = it.optDoubleOrNull("hct"),
            mcv = it.optDoubleOrNull("mcv"),
            mch = it.optDoubleOrNull("mch"),
            mchc = it.optDoubleOrNull("mchc"),
            plt = it.optDoubleOrNull("plt"),
            lymPct = it.optDoubleOrNull("lymPct"),
            neutPct = it.optDoubleOrNull("neutPct"),
            monoPct = it.optDoubleOrNull("monoPct"),
            eosPct = it.optDoubleOrNull("eosPct"),
            crp = it.optDoubleOrNull("crp")
        )
    }

    private fun JSONArray?.toNoteList(mode: ImportMode) = items().map {
        org.bp.songbaobao.data.local.entity.Note(
            id = it.optLong("id").forMode(mode),
            date = it.optStr("date"),
            time = it.optStr("time"),
            title = it.optStr("title"),
            content = it.optStr("content"),
            mood = it.optStr("mood"),
            tags = it.optStr("tags"),
            createdAt = it.optStr("createdAt"),
            updatedAt = it.optStr("updatedAt")
        )
    }

    private fun JSONArray?.toNotePhotoList(mode: ImportMode) = items().map {
        org.bp.songbaobao.data.local.entity.NotePhoto(
            id = it.optLong("id").forMode(mode),
            noteId = it.optLong("noteId"),
            filename = it.optStr("filename"),
            thumb = it.optStr("thumb"),
            caption = it.optStr("caption"),
            size = it.optLong("size"),
            createdAt = it.optStr("createdAt")
        )
    }
}
