package org.bp.songbaobao.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import org.bp.songbaobao.data.local.dao.NoteDao
import org.bp.songbaobao.data.local.dao.NotePhotoWithNote
import org.bp.songbaobao.data.local.dao.NoteWithMeta
import org.bp.songbaobao.data.local.entity.Note
import org.bp.songbaobao.data.local.entity.NotePhoto
import org.bp.songbaobao.util.nowStamp
import org.bp.songbaobao.util.todayStr
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class NoteStats(val notes: Int, val photos: Int, val since: String)

@Singleton
class NoteRepository @Inject constructor(
    private val dao: NoteDao,
    @ApplicationContext private val context: Context
) {

    /** 照片存放目录：应用私有 filesDir/photos */
    private fun photosDir(): File =
        File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    fun photoFile(name: String): File = File(photosDir(), name)

    fun query(keyword: String = "", tag: String = ""): Flow<List<NoteWithMeta>> =
        dao.query(keyword.trim(), tag.trim())

    suspend fun get(id: Long): Note? = dao.getById(id)

    suspend fun save(note: Note): Long =
        if (note.id == 0L) dao.insert(note) else {
            dao.update(note)
            note.id
        }

    /** 删除笔记并清理磁盘照片 */
    suspend fun delete(id: Long) {
        val names = dao.filenamesOf(id)
        dao.delete(id)
        names.forEach { deleteFiles(it) }
    }

    fun photosOf(noteId: Long): Flow<List<NotePhoto>> = dao.photosOf(noteId)

    suspend fun photosOfSync(noteId: Long): List<NotePhoto> = dao.photosOfSync(noteId)

    fun recentPhotos(limit: Int = 12): Flow<List<NotePhotoWithNote>> = dao.recentPhotos(limit)

    /** 笔记数 / 照片数 / 最早日期，三个 Flow 合并 */
    fun statsFlow(): Flow<NoteStats> = combine(dao.noteCount(), dao.photoCount(), dao.firstDate()) { n, p, d ->
        NoteStats(notes = n, photos = p, since = d ?: "")
    }

    suspend fun allTags(): List<Pair<String, Int>> {
        val counter = LinkedHashMap<String, Int>()
        dao.allTagStrings().forEach { raw ->
            raw.replace("，", ",").split(",").map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { counter[it] = (counter[it] ?: 0) + 1 }
        }
        return counter.toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
    }

    suspend fun allForExport(): List<Note> = dao.allForExport()

    // ---------------- 照片处理 ----------------
    /**
     * 保存图片：压缩到最长边 1600，生成 320 缩略图，返回 (原图名, 缩略图名, 大小)。
     * 在 IO 线程执行。
     */
    suspend fun savePhoto(uri: Uri): Triple<String, String, Long>? = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val bitmap = BitmapFactory.decodeStream(input, null, BitmapFactory.Options())
        input.close()
        if (bitmap == null) return@withContext null

        val stamp = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val base = "${stamp}_${java.util.UUID.randomUUID().toString().take(8)}"
        val bigName = "$base.jpg"
        val thumbName = "${base}_thumb.jpg"

        val scaled = scaleBitmap(bitmap, 1600)
        val bigFile = File(photosDir(), bigName)
        FileOutputStream(bigFile).use {
            scaled.compress(Bitmap.CompressFormat.JPEG, 86, it)
        }

        val thumb = scaleBitmap(scaled, 320)
        FileOutputStream(File(photosDir(), thumbName)).use {
            thumb.compress(Bitmap.CompressFormat.JPEG, 82, it)
        }

        Triple(bigName, thumbName, bigFile.length())
    }

    private fun scaleBitmap(src: Bitmap, maxSide: Int): Bitmap {
        val w = src.width
        val h = src.height
        if (maxOf(w, h) <= maxSide) return src
        val ratio = maxSide.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(src, (w * ratio).toInt(), (h * ratio).toInt(), true)
    }

    suspend fun addPhoto(noteId: Long, uri: Uri) {
        val triple = savePhoto(uri) ?: return
        dao.insertPhoto(
            NotePhoto(
                noteId = noteId,
                filename = triple.first,
                thumb = triple.second,
                caption = "",
                size = triple.third,
                createdAt = nowStamp()
            )
        )
    }

    suspend fun deletePhoto(id: Long) {
        val photo = dao.photoById(id) ?: return
        dao.deletePhoto(id)
        deleteFiles(photo.filename)
    }

    private suspend fun deleteFiles(filename: String) = withContext(Dispatchers.IO) {
        if (filename.isBlank()) return@withContext
        File(photosDir(), filename).takeIf { it.exists() }?.delete()
        val thumb = "${filename.substringBeforeLast('.')}_thumb.jpg"
        File(photosDir(), thumb).takeIf { it.exists() }?.delete()
    }

    fun todayNote(): Note = Note(
        date = todayStr(), time = "", title = "", content = "", mood = "😊",
        tags = "", createdAt = nowStamp(), updatedAt = nowStamp()
    )
}
