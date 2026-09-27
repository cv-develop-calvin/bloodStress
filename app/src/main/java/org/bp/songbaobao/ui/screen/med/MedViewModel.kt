package org.bp.songbaobao.ui.screen.med

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.bp.songbaobao.data.local.dao.MedLogWithMed
import org.bp.songbaobao.data.local.entity.Medication
import org.bp.songbaobao.data.repository.Adherence
import org.bp.songbaobao.data.repository.MedRepository
import org.bp.songbaobao.data.repository.PendingMed
import org.bp.songbaobao.R
import org.bp.songbaobao.util.nowStamp
import org.bp.songbaobao.util.todayStr
import javax.inject.Inject

data class MedUiState(
    val meds: List<Medication> = emptyList(),
    val logs: List<MedLogWithMed> = emptyList(),
    val adherence: Adherence? = null,
    val pending: List<PendingMed> = emptyList(),
    val takenSlots: Set<Pair<Long, String>> = emptySet(),
    val missedSlots: Set<Pair<Long, String>> = emptySet(),
    val todayTotal: Int = 0,
    val todayTaken: Int = 0
)

@HiltViewModel
class MedViewModel @Inject constructor(
    private val repo: MedRepository,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    /** 编辑页的药品 id（新增为 null） */
    val editId: Long? = savedStateHandle.get<Long>("id")?.takeIf { it != 0L }

    private val _ui = MutableStateFlow(MedUiState())

    init {
        viewModelScope.launch {
            while (true) {
                runCatching { refresh() }
                delay(30_000)
            }
        }
    }

    val uiState: StateFlow<MedUiState> = _ui.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val meds = runCatching { repo.all().first() }.getOrDefault(emptyList())
            val logs = runCatching { repo.recentLogs(20).first() }.getOrDefault(emptyList())
            val taken = runCatching { repo.takenSlots(todayStr()) }.getOrDefault(emptySet())
            val missed = runCatching { repo.missedSlots(todayStr()) }.getOrDefault(emptySet())
            val adh = runCatching { repo.adherence(30) }.getOrNull()
            val pending = runCatching { repo.pending() }.getOrDefault(emptyList())
            val (total, takenCount) = runCatching { repo.todayProgress(todayStr()) }
                .getOrDefault(0 to 0)
            _ui.value = MedUiState(
                meds = meds,
                logs = logs,
                adherence = adh,
                pending = pending,
                takenSlots = taken,
                missedSlots = missed,
                todayTotal = total,
                todayTaken = takenCount
            )
        }
    }

    fun toggleTaken(medId: Long, slot: String) {
        viewModelScope.launch {
            repo.toggleTaken(medId, slot)
            refresh()
        }
    }

    fun setActive(med: Medication, active: Boolean) {
        viewModelScope.launch {
            repo.setActive(med.id, active)
            scheduleAll()
        }
    }

    fun delete(med: Medication) {
        viewModelScope.launch {
            repo.delete(med.id)
            scheduleAll()
        }
    }

    suspend fun load(id: Long): Medication? = repo.get(id)

    fun save(med: Medication, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.save(med)
            scheduleAll()
            onDone()
        }
    }

    /** 保存 / 启停后重新注册全部提醒 */
    private fun scheduleAll() {
        viewModelScope.launch {
            runCatching {
                val meds = repo.all().first()
                org.bp.songbaobao.reminder.AlarmScheduler.rescheduleAll(appContext, meds)
            }
        }
    }

    fun newMed(): Medication = Medication(
        name = "", dosage = "", unit = "mg", freq = appContext.getString(R.string.freq_daily_1),
        times = "08:00", startDate = todayStr(), endDate = "",
        note = "", active = true, createdAt = nowStamp()
    )
}
