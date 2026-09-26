package org.bp.songbaobao.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.bp.songbaobao.data.local.dao.BpDao
import org.bp.songbaobao.data.local.dao.GlucoseDao
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
        NotePhoto::class,
        GlucoseRecord::class
    ],
    version = 2,
    // 本项目不使用 schema 迁移文件（升级走 fallbackToDestructiveMigration），
    // 设为 false 避免 Room 要求提供 schemaLocation。
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bpDao(): BpDao
    abstract fun medDao(): MedDao
    abstract fun labDao(): LabDao
    abstract fun noteDao(): NoteDao
    abstract fun glucoseDao(): GlucoseDao
}
