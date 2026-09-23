@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.bp.songbaobao.ui.screen.bp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.bp.songbaobao.data.local.dao.BpStats
import org.bp.songbaobao.data.local.entity.BpRecord
import org.bp.songbaobao.data.repository.*
import org.bp.songbaobao.util.nowStamp
import org.bp.songbaobao.util.nowTimeStr
import org.bp.songbaobao.util.todayStr
import javax.inject.Inject

data class BpUiState(
    val days: Int? = 30,
    val trend: List<BpRecord> = emptyList(),
    val stats: BpStats? = null,
    val recent: List<BpRecord> = emptyList(),
    val latest: BpRecord? = null,
    val adherence: Adherence? = null,
    val pending: List<PendingMed> = emptyList(),
    val showSys: Boolean = true,
    val showDia: Boolean = true,
    val showPulse: Boolean = true
)

@HiltViewModel
class BpViewModel @Inject constructor(
    private val bpRepo: BpRepository,
    private val medRepo: MedRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 编辑页从导航参数拿到的记录 id（新增页为 null） */
    val editId: Long? = savedStateHandle.get<Long>("id")?.takeIf { it != 0L }

    private val _days = MutableStateFlow<Int?>(30)
    private val _series = MutableStateFlow(listOf(true, true, true))
    // 依从性/待服药是 suspend 查询，这里定期刷新后并入 uiState
    private val _adherence = MutableStateFlow<Adherence?>(null)
    private val _pending = MutableStateFlow<List<PendingMed>>(emptyList())

    init {
        viewModelScope.launch {
            while (true) {
                runCatching {
                    _adherence.value = medRepo.adherence(7)
                    _pending.value = medRepo.pending()
                }
                delay(60_000)
            }
        }
    }

    val uiState: StateFlow<BpUiState> = combine(
        _days, _series, _adherence, _pending
    ) { days, series, adherence, pending ->
        Params(days, series, adherence, pending)
    }.flatMapLatest { p ->
        combine(bpRepo.trend(p.days), bpRepo.stats(p.days), bpRepo.recent(5)) { trend, stats, recent ->
            BpUiState(
                days = p.days,
                trend = trend,
                stats = stats,
                recent = recent,
                latest = recent.firstOrNull(),
                adherence = p.adherence,
                pending = p.pending,
                showSys = p.series[0],
                showDia = p.series[1],
                showPulse = p.series[2]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BpUiState())

    private data class Params(
        val days: Int?,
        val series: List<Boolean>,
        val adherence: Adherence?,
        val pending: List<PendingMed>
    )

    fun setDays(days: Int?) { _days.value = days }

    fun toggleSeries(index: Int) {
        _series.value = _series.value.toMutableList().also { it[index] = !it[index] }
    }

    fun refreshMeds() {
        viewModelScope.launch {
            _adherence.value = medRepo.adherence(7)
            _pending.value = medRepo.pending()
        }
    }

    /** 列表页按日期区间过滤 */
    fun recordsFlow(from: String, to: String): Flow<List<BpRecord>> = bpRepo.list(from, to)

    // ---------------- 表单 ----------------
    fun emptyRecord(): BpRecord = BpRecord(
        date = todayStr(), time = nowTimeStr(),
        systolic = 120, diastolic = 80, pulse = null,
        note = "", createdAt = nowStamp()
    )

    suspend fun load(id: Long): BpRecord? = bpRepo.get(id)

    fun save(record: BpRecord, onDone: () -> Unit) {
        viewModelScope.launch {
            bpRepo.save(record)
            onDone()
        }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            bpRepo.delete(id)
            onDone()
        }
    }
}
