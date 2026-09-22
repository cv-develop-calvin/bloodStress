package org.bp.songbaobao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 血压记录 */
@Entity(tableName = "records", indices = [Index("date")])
data class BpRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // yyyy-MM-dd
    val time: String = "",       // HH:mm
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int? = null,
    val note: String = "",
    val createdAt: String
)

/** 药品 */
@Entity(tableName = "meds")
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String = "",
    val unit: String = "",
    val freq: String = "",        // 每日 1 次 / 按需服用 ...
    val times: String = "",       // 逗号分隔，如 "08:00,20:00"
    val startDate: String = "",
    val endDate: String = "",
    val note: String = "",
    val active: Boolean = true,
    val createdAt: String
)

/** 服药打卡记录 */
@Entity(
    tableName = "med_logs",
    indices = [Index("date"), Index("medId")],
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MedLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medId: Long,
    val date: String,
    val time: String = "",
    val status: String = "taken",
    val note: String = "",
    val createdAt: String
)

/**
 * 血常规报告（宽表：每个指标一列）。
 * 指标字段见 domain.CbcItems。
 */
@Entity(tableName = "lab_reports", indices = [Index("date")])
data class LabReport(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val time: String = "",
    val hospital: String = "",
    val source: String = "manual",   // manual / photo
    val photo: String = "",
    val rawText: String = "",
    val note: String = "",
    val createdAt: String,

    // 血常规指标（宽表，字段与 domain.CbcItems 的 key 一一对应）
    val wbc: Double? = null,
    val rbc: Double? = null,
    val hgb: Double? = null,
    val hct: Double? = null,
    val mcv: Double? = null,
    val mch: Double? = null,
    val mchc: Double? = null,
    val plt: Double? = null,
    val lymPct: Double? = null,
    val neutPct: Double? = null,
    val monoPct: Double? = null,
    val eosPct: Double? = null,
    val crp: Double? = null
) {
    /** 按 CbcItems 的 key 取指标值 */
    fun valueOf(key: String): Double? = when (key) {
        "wbc" -> wbc
        "rbc" -> rbc
        "hgb" -> hgb
        "hct" -> hct
        "mcv" -> mcv
        "mch" -> mch
        "mchc" -> mchc
        "plt" -> plt
        "lymPct" -> lymPct
        "neutPct" -> neutPct
        "monoPct" -> monoPct
        "eosPct" -> eosPct
        "crp" -> crp
        else -> null
    }

    /** 全部指标值，供异常统计使用 */
    fun values(): Map<String, Double?> = mapOf(
        "wbc" to wbc, "rbc" to rbc, "hgb" to hgb, "hct" to hct,
        "mcv" to mcv, "mch" to mch, "mchc" to mchc, "plt" to plt,
        "lymPct" to lymPct, "neutPct" to neutPct, "monoPct" to monoPct,
        "eosPct" to eosPct, "crp" to crp
    )
}

/** 笔记（留言） */
@Entity(tableName = "notes", indices = [Index("date")])
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val time: String = "",
    val title: String = "",
    val content: String = "",
    val mood: String = "",
    val tags: String = "",       // 逗号分隔
    val createdAt: String,
    val updatedAt: String
)

/** 笔记照片 */
@Entity(
    tableName = "note_photos",
    indices = [Index("noteId")],
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NotePhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val filename: String,
    val thumb: String = "",
    val caption: String = "",
    val size: Long = 0,
    val createdAt: String
)
