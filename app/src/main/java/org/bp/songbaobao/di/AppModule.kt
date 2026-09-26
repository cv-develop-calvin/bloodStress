package org.bp.songbaobao.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.bp.songbaobao.data.local.AppDatabase
import org.bp.songbaobao.data.local.dao.BpDao
import org.bp.songbaobao.data.local.dao.GlucoseDao
import org.bp.songbaobao.data.local.dao.LabDao
import org.bp.songbaobao.data.local.dao.MedDao
import org.bp.songbaobao.data.local.dao.NoteDao
import org.bp.songbaobao.data.repository.GlucoseRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "songbaobao.db")
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBpDao(db: AppDatabase): BpDao = db.bpDao()
    @Provides fun provideMedDao(db: AppDatabase): MedDao = db.medDao()
    @Provides fun provideLabDao(db: AppDatabase): LabDao = db.labDao()
    @Provides fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
    @Provides fun provideGlucoseDao(db: AppDatabase): GlucoseDao = db.glucoseDao()
    @Provides fun provideGlucoseRepository(dao: GlucoseDao): GlucoseRepository = GlucoseRepository(dao)
}

/**
 * 从 v1 升级到 v2：新增血糖记录表，不破坏已有数据（血压 / 用药 / 血常规 / 笔记）。
 * 表结构与 [org.bp.songbaobao.data.local.entity.GlucoseRecord] 严格一致，
 * 否则 Room 在运行时做 schema 校验会抛异常。
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS glucose_records (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "date TEXT NOT NULL, " +
                "time TEXT NOT NULL DEFAULT '', " +
                "value REAL NOT NULL, " +
                "context TEXT NOT NULL, " +
                "note TEXT NOT NULL DEFAULT '', " +
                "createdAt TEXT NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_glucose_records_date ON glucose_records (date)")
    }
}
