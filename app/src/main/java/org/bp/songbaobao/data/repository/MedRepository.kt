package org.bp.songbaobao.data.repository

import kotlinx.coroutines.flow.Flow
import org.bp.songbaobao.data.local.dao.MedDao
import org.bp.songbaobao.data.local.dao.MedLogWithMed
import org.bp.songbaobao.data.local.entity.MedLog
import org.bp.songbaobao.data.local.entity.Medication
import org.bp.songbaobao.util.minutesBetween
import org.bp.songbaobao.util.nowStamp
import org.bp.songbaobao.util.parseTimes
import org.bp.songbaobao.util.todayStr
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 当前时刻应服但还没打卡的药 */
data class PendingMed(
    val med: Medication,
    val slot: String,
    val delayMinutes: Int
)

/** 近 N 天服药依从性 */
data class Adherence(val expected: Int, val taken: Int, val rate: Int?)

@Singleton
class MedRepository @Inject constructor(private val dao: MedDao) {

    fun all(): Flow<List<Medication>> = dao.all()

    suspend fun get(id: Long): Medication? = dao.getById(id)

    suspend fun save(med: Medication): Long =
        if (med.id == 0L) dao.insert(med) else {
            dao.update(med)
            med.id
        }

    suspend fun delete(id: Long) {
        dao.deleteLogsOf(id)
        dao.delete(id)
    }

    suspend fun setActive(id: Long, active: Boolean) = dao.setActive(id, active)

    // ---------------- 打卡 ----------------
    fun recentLogs(limit: Int = 20): Flow<List<MedLogWithMed>> = dao.recentLogs(limit)

    /** 打卡；若该时段已打卡则撤销，返回 true 表示变为「已服」 */
    suspend fun toggleTaken(medId: Long, slot: String, date: String = todayStr()): Boolean {
        val existing = dao.logsOfDate(date).firstOrNull { it.medId == medId && it.time == slot }
        if (existing != null) {
            dao.deleteLog(existing.id)
            return false
        }
        dao.insertLog(
            MedLog(medId = medId, date = date, time = slot, status = "taken", createdAt = nowStamp())
        )
        return true
    }

    suspend fun takenSlots(date: String = todayStr()): Set<Pair<Long, String>> =
        dao.logsOfDate(date).map { it.medId to it.time }.toSet()

    /** 当前到点未服的药 */
    suspend fun pending(now: java.time.LocalDateTime = java.time.LocalDateTime.now()): List<PendingMed> {
        val today = LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val hhmm = String.format("%02d:%02d", now.hour, now.minute)
        val done = takenSlots(today)
        val out = mutableListOf<PendingMed>()
        for (med in dao.activeList()) {
            if (med.startDate.isNotBlank() && today < med.startDate) continue
            if (med.endDate.isNotBlank() && today > med.endDate) continue
            for (slot in parseTimes(med.times)) {
                if (slot <= hhmm && (med.id to slot) !in done) {
                    out.add(PendingMed(med, slot, minutesBetween(slot, hhmm)))
                }
            }
        }
        return out.sortedBy { it.delayMinutes }
    }

    suspend fun adherence(days: Int = 7): Adherence {
        val meds = dao.activeList().filter { it.times.isNotBlank() }
        val today = LocalDate.now()
        val from = today.minusDays(days.toLong() - 1)
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val logs = dao.logsSince(from)
        val done = logs.map { Triple(it.medId, it.date, it.time) }.toSet()

        var expected = 0
        var taken = 0
        val todayStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        for (med in meds) {
            val slots = parseTimes(med.times)
            for (i in 0 until days) {
                val d = from.let {
                    LocalDate.parse(it).plusDays(i.toLong())
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                }
                if (med.startDate.isNotBlank() && d < med.startDate) continue
                if (med.endDate.isNotBlank() && d > med.endDate) continue
                if (d > todayStr) continue
                expected += slots.size
                taken += slots.count { Triple(med.id, d, it) in done }
            }
        }
        return Adherence(expected, taken, if (expected > 0) (taken * 100 / expected) else null)
    }
}
