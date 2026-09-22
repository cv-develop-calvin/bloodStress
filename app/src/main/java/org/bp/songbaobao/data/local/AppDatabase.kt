package org.bp.songbaobao.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.bp.songbaobao.data.local.dao.BpDao
import org.bp.songbaobao.data.local.dao.LabDao
import org.bp.songbaobao.data.local.dao.MedDao
import org.bp.songbaobao.data.local.dao.NoteDao
import org.bp.songbaobao.data.local.entity.*

@Database(
    entities = [
        BpRecord::class,
        Medication::class,
        MedLog::class,
        LabReport::class,
        Note::class,
        NotePhoto::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bpDao(): BpDao
    abstract fun medDao(): MedDao
    abstract fun labDao(): LabDao
    abstract fun noteDao(): NoteDao
}
