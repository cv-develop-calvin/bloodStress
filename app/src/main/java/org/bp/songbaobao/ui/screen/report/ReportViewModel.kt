package org.bp.songbaobao.ui.screen.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.bp.songbaobao.data.local.dao.MedLogWithMed
import org.bp.songbaobao.data.repository.Adherence
import org.bp.songbaobao.data.repository.BpRepository
import org.bp.songbaobao.data.repository.GlucoseRepository
import org.bp.songbaobao.data.repository.LabRepository
import org.bp.songbaobao.data.repository.MedRepository
import org.bp.songbaobao.domain.CbcItems
import org.bp.songbaobao.ui.components.BpLevel
import org.bp.songbaobao.ui.components.GlucoseContext
import org.bp.songbaobao.ui.components.GlucoseLevel
import org.bp.songbaobao.ui.components.classifyBp
import org.bp.songbaobao.ui.components.classifyGlucose
import org.bp.songbaobao.util.startOf
import org.bp.songbaobao.util.todayStr
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val bpRepo: BpRepository,
    private val glucoRepo: GlucoseRepository,
    private val labRepo: LabRepository,
    private val medRepo: MedRepository
) : ViewModel() {

    private val _data = MutableStateFlow<ReportData?>(null)
    val data: StateFlow<ReportData?> = _data.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _loading.value = true
            runCatching {
                val from = startOf(90)
                val to = todayStr()
                val bp = bpRepo.trend(90).first()
                val glucose = glucoRepo.trend(90).first()
                val lab = labRepo.latest().first()
                val adh: Adherence? = runCatching { medRepo.adherence(30) }.getOrNull()
                val medLogs: List<MedLogWithMed> =
                    runCatching { medRepo.recentLogs(60).first() }.getOrDefault(emptyList())
                val bpLevelCount = bp.groupingBy { classifyBp(it.systolic, it.diastolic) }.eachCount()
                val glucoseOk = glucose.count {
                    classifyGlucose(it.value, GlucoseContext.fromKey(it.context)) == GlucoseLevel.NORMAL
                }
                val labAbnormal = buildLabAbnormal(lab)
                _data.value = ReportData(
                    rangeFrom = from,
                    rangeTo = to,
                    bp = bp,
                    glucose = glucose,
                    lab = lab,
                    adherence = adh,
                    medLogs = medLogs,
                    bpLevelCount = bpLevelCount,
                    glucoseOkCount = glucoseOk,
                    glucoseTotal = glucose.size,
                    labAbnormal = labAbnormal
                )
            }
            _loading.value = false
        }
    }

    private fun buildLabAbnormal(lab: org.bp.songbaobao.data.local.entity.LabReport?): List<Pair<String, String>> {
        if (lab == null) return emptyList()
        val out = mutableListOf<Pair<String, String>>()
        for (key in CbcItems.ORDER) {
            val v = lab.valueOf(key) ?: continue
            if (CbcItems.isAbnormal(key, v)) {
                val item = CbcItems.get(key)
                val dir = if (v < item.low) "偏低" else "偏高"
                out.add(key to "${v} ${item.unit}（$dir）")
            }
        }
        return out
    }
}
