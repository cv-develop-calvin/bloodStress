package org.bp.songbaobao.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.bp.songbaobao.data.local.AppDatabase
import org.bp.songbaobao.data.local.dao.BpDao
import org.bp.songbaobao.data.local.dao.LabDao
import org.bp.songbaobao.data.local.dao.MedDao
import org.bp.songbaobao.data.local.dao.NoteDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "songbaobao.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideBpDao(db: AppDatabase): BpDao = db.bpDao()
    @Provides fun provideMedDao(db: AppDatabase): MedDao = db.medDao()
    @Provides fun provideLabDao(db: AppDatabase): LabDao = db.labDao()
    @Provides fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
}
