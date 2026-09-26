package org.bp.songbaobao.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.bp.songbaobao.data.local.dao.GlucoseDao
import org.bp.songbaobao.data.local.dao.GlucoseStats
import org.bp.songbaobao.data.local.entity.GlucoseRecord
import org.bp.songbaobao.util.startOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlucoseRepository @Inject constructor(private val dao: GlucoseDao) {

    fun trend(days: Int?): Flow<List<GlucoseRecord>> =
        if (days == null) dao.allAsc() else dao.sinceAsc(startOf(days))

    fun stats(days: Int?): Flow<GlucoseStats> =
        dao.stats(if (days == null) null else startOf(days))

    fun recent(limit: Int = 5): Flow<List<GlucoseRecord>> = dao.recent(limit)

    fun list(from: String = "", to: String = ""): Flow<List<GlucoseRecord>> = dao.paged(from, to)

    suspend fun latest(): GlucoseRecord? = dao.latest()

    suspend fun get(id: Long): GlucoseRecord? = dao.getById(id)

    suspend fun save(record: GlucoseRecord): Long =
        if (record.id == 0L) dao.insert(record) else {
            dao.update(record)
            record.id
        }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun allForExport(): List<GlucoseRecord> = dao.allForExport()

    /** 空态判断用 */
    fun countFlow(): Flow<Int> = dao.allDesc().map { it.size }
}
