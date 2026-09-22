package org.bp.songbaobao.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.bp.songbaobao.data.local.entity.*

@Dao
interface BpDao {
    @Insert
    suspend fun insert(record: BpRecord): Long

    @Update
    suspend fun update(record: BpRecord)

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM records WHERE id = :id")
    suspend fun getById(id: Long): BpRecord?

    @Query("SELECT * FROM records ORDER BY date DESC, time DESC, id DESC LIMIT 1")
    suspend fun latest(): BpRecord?

    @Query("SELECT * FROM records ORDER BY date DESC, time DESC, id DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<BpRecord>>

    /** 供曲线使用：按日期升序；days 为空表示全部 */
    @Query("SELECT * FROM records ORDER BY date ASC, time ASC, id ASC")
    fun allAsc(): Flow<List<BpRecord>>

    @Query("SELECT * FROM records WHERE date >= :from ORDER BY date ASC, time ASC, id ASC")
    fun sinceAsc(from: String): Flow<List<BpRecord>>

    @Query("SELECT * FROM records ORDER BY date DESC, time DESC, id DESC")
    fun allDesc(): Flow<List<BpRecord>>

    @Query(
        """SELECT COUNT(*) AS n, AVG(systolic) AS avgSys, AVG(diastolic) AS avgDia,
           AVG(pulse) AS avgPulse, MAX(systolic) AS maxSys, MIN(systolic) AS minSys,
           MAX(diastolic) AS maxDia, MIN(diastolic) AS minDia FROM records
           WHERE (:from IS NULL OR date >= :from)"""
    )
    fun stats(from: String?): Flow<BpStats>

    @Query("SELECT * FROM records WHERE (:from='' OR date>=:from) AND (:to='' OR date<=:to) ORDER BY date DESC, time DESC, id DESC")
    fun paged(from: String, to: String): Flow<List<BpRecord>>

    @Query("SELECT * FROM records ORDER BY date ASC, time ASC")
    suspend fun allForExport(): List<BpRecord>
}

data class BpStats(
    val n: Int,
    val avgSys: Double?,
    val avgDia: Double?,
    val avgPulse: Double?,
    val maxSys: Int?,
    val minSys: Int?,
    val maxDia: Int?,
    val minDia: Int?
)

@Dao
interface MedDao {
    @Insert
    suspend fun insert(med: Medication): Long

    @Update
    suspend fun update(med: Medication)

    @Query("DELETE FROM meds WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM meds WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Query("SELECT * FROM meds ORDER BY active DESC, startDate DESC, id DESC")
    fun all(): Flow<List<Medication>>

    @Query("SELECT * FROM meds WHERE active = 1 ORDER BY startDate DESC, id DESC")
    suspend fun activeList(): List<Medication>

    @Query("UPDATE meds SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    // ---- 服药打卡 ----
    @Insert
    suspend fun insertLog(log: MedLog): Long

    @Query("DELETE FROM med_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("DELETE FROM med_logs WHERE medId = :medId")
    suspend fun deleteLogsOf(medId: Long)

    @Query("SELECT * FROM med_logs WHERE date = :date")
    suspend fun logsOfDate(date: String): List<MedLog>

    @Query(
        """SELECT l.*, m.name AS medName, m.dosage AS medDosage, m.unit AS medUnit
           FROM med_logs l LEFT JOIN meds m ON m.id = l.medId
           ORDER BY l.date DESC, l.time DESC, l.id DESC LIMIT :limit"""
    )
    fun recentLogs(limit: Int): Flow<List<MedLogWithMed>>

    @Query("SELECT * FROM med_logs WHERE date >= :from")
    suspend fun logsSince(from: String): List<MedLog>
}

data class MedLogWithMed(
    val id: Long,
    val medId: Long,
    val date: String,
    val time: String,
    val status: String,
    val note: String,
    val createdAt: String,
    val medName: String?,
    val medDosage: String?,
    val medUnit: String?
)

@Dao
interface LabDao {
    @Insert
    suspend fun insert(report: LabReport): Long

    @Update
    suspend fun update(report: LabReport)

    @Query("DELETE FROM lab_reports WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM lab_reports WHERE id = :id")
    suspend fun getById(id: Long): LabReport?

    @Query("SELECT * FROM lab_reports ORDER BY date DESC, id DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<LabReport>>

    @Query("SELECT * FROM lab_reports ORDER BY date DESC, id DESC LIMIT 1")
    fun latest(): Flow<LabReport?>

    @Query("SELECT * FROM lab_reports ORDER BY date DESC, id DESC")
    suspend fun allForExport(): List<LabReport>
}

@Dao
interface NoteDao {
    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    /** 关键字搜索 + 标签筛选 */
    @Query(
        """SELECT n.*,
           (SELECT COUNT(*) FROM note_photos p WHERE p.noteId = n.id) AS photoCount,
           (SELECT p.filename FROM note_photos p WHERE p.noteId = n.id ORDER BY p.id ASC LIMIT 1) AS cover
           FROM notes n
           WHERE (:kw = '' OR n.title LIKE '%'||:kw||'%' OR n.content LIKE '%'||:kw||'%' OR n.tags LIKE '%'||:kw||'%')
             AND (:tag = '' OR (','||n.tags||',') LIKE '%,'||:tag||',%')
           ORDER BY n.date DESC, n.id DESC LIMIT :limit"""
    )
    fun query(kw: String, tag: String, limit: Int = 200): Flow<List<NoteWithMeta>>

    @Query("SELECT id, date, time, title, content, mood, tags, createdAt, updatedAt FROM notes")
    suspend fun allForExport(): List<Note>

    @Query("SELECT tags FROM notes WHERE tags <> ''")
    suspend fun allTagStrings(): List<String>

    @Query("SELECT COUNT(*) FROM notes")
    fun noteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM note_photos")
    fun photoCount(): Flow<Int>

    @Query("SELECT MIN(date) FROM notes")
    fun firstDate(): Flow<String?>

    // ---- 照片 ----
    @Insert
    suspend fun insertPhoto(photo: NotePhoto): Long

    @Query("DELETE FROM note_photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("SELECT * FROM note_photos WHERE noteId = :noteId ORDER BY id ASC")
    fun photosOf(noteId: Long): Flow<List<NotePhoto>>

    @Query("SELECT * FROM note_photos WHERE noteId = :noteId ORDER BY id ASC")
    suspend fun photosOfSync(noteId: Long): List<NotePhoto>

    @Query("SELECT * FROM note_photos WHERE id = :id")
    suspend fun photoById(id: Long): NotePhoto?

    @Query("SELECT filename FROM note_photos WHERE noteId = :noteId")
    suspend fun filenamesOf(noteId: Long): List<String>

    @Query(
        """SELECT p.*, n.date AS noteDate, n.title AS noteTitle
           FROM note_photos p LEFT JOIN notes n ON n.id = p.noteId
           ORDER BY p.id DESC LIMIT :limit"""
    )
    fun recentPhotos(limit: Int): Flow<List<NotePhotoWithNote>>
}

data class NoteWithMeta(
    val id: Long,
    val date: String,
    val time: String,
    val title: String,
    val content: String,
    val mood: String,
    val tags: String,
    val createdAt: String,
    val updatedAt: String,
    val photoCount: Int,
    val cover: String?
)

data class NotePhotoWithNote(
    val id: Long,
    val noteId: Long,
    val filename: String,
    val thumb: String,
    val caption: String,
    val size: Long,
    val createdAt: String,
    val noteDate: String?,
    val noteTitle: String?
)
