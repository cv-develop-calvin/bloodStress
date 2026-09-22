package org.bp.songbaobao.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.bp.songbaobao.data.local.dao.BpDao
import org.bp.songbaobao.data.local.dao.BpStats
import org.bp.songbaobao.data.local.entity.BpRecord
import org.bp.songbaobao.util.startOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BpRepository @Inject constructor(private val dao: BpDao) {

    fun trend(days: Int?): Flow<List<BpRecord>> =
        if (days == null) dao.allAsc() else dao.sinceAsc(startOf(days))

    fun stats(days: Int?): Flow<BpStats> =
        dao.stats(if (days == null) null else startOf(days))

    fun recent(limit: Int = 5): Flow<List<BpRecord>> = dao.recent(limit)

    fun list(from: String = "", to: String = ""): Flow<List<BpRecord>> = dao.paged(from, to)

    suspend fun latest(): BpRecord? = dao.latest()

    suspend fun get(id: Long): BpRecord? = dao.getById(id)

    suspend fun save(record: BpRecord): Long =
        if (record.id == 0L) dao.insert(record) else {
            dao.update(record)
            record.id
        }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun allForExport(): List<BpRecord> = dao.allForExport()

    /** 空态判断用 */
    fun countFlow(): Flow<Int> = dao.allDesc().map { it.size }
}
