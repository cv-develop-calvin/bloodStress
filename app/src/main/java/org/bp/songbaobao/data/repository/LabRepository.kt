package org.bp.songbaobao.data.repository

import kotlinx.coroutines.flow.Flow
import org.bp.songbaobao.data.local.dao.LabDao
import org.bp.songbaobao.data.local.entity.LabReport
import org.bp.songbaobao.domain.CbcItems
import org.bp.songbaobao.util.startOf
import javax.inject.Inject
import javax.inject.Singleton

data class LabPoint(val date: String, val value: Double)

@Singleton
class LabRepository @Inject constructor(private val dao: LabDao) {

    fun recent(limit: Int = 50): Flow<List<LabReport>> = dao.recent(limit)

    fun latest(): Flow<LabReport?> = dao.latest()

    suspend fun get(id: Long): LabReport? = dao.getById(id)

    suspend fun save(report: LabReport): Long =
        if (report.id == 0L) dao.insert(report) else {
            dao.update(report)
            report.id
        }

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun allForExport(): List<LabReport> = dao.allForExport()

    /** 某指标的曲线数据（升序），days 为空表示全部 */
    suspend fun series(itemKey: String, days: Int?): List<LabPoint> {
        if (itemKey !in CbcItems.ORDER) return emptyList()
        val from = days?.let { startOf(it) }
        return dao.allForExport()
            .filter { from == null || it.date >= from }
            .sortedWith(compareBy({ it.date }, { it.id }))
            .mapNotNull { r -> r.valueOf(itemKey)?.let { LabPoint(r.date, it) } }
    }
}
