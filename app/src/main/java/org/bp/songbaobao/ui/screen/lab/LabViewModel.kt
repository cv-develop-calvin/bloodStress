package org.bp.songbaobao.ui.screen.lab

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bp.songbaobao.data.local.entity.LabReport
import org.bp.songbaobao.data.repository.LabPoint
import org.bp.songbaobao.data.repository.LabRepository
import org.bp.songbaobao.R
import org.bp.songbaobao.domain.CbcItems
import org.bp.songbaobao.domain.OcrImagePreprocessor
import org.bp.songbaobao.domain.OcrParser
import org.bp.songbaobao.domain.TextRecognizerProvider
import org.bp.songbaobao.util.nowStamp
import org.bp.songbaobao.util.nowTimeStr
import org.bp.songbaobao.util.todayStr
import javax.inject.Inject

data class LabUiState(
    val reports: List<LabReport> = emptyList(),
    val latest: LabReport? = null,
    val seriesItem: String = "wbc",
    val series: List<LabPoint> = emptyList(),
    val days: Int? = null
)

@HiltViewModel
class LabViewModel @Inject constructor(
    private val repo: LabRepository,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val editId: Long? = savedStateHandle.get<Long>("id")?.takeIf { it != 0L }

    private val _seriesItem = MutableStateFlow("wbc")
    private val _days = MutableStateFlow<Int?>(null)
    private val _series = MutableStateFlow<List<LabPoint>>(emptyList())

    val uiState: StateFlow<LabUiState> = combine(
        repo.recent(50), repo.latest(), _seriesItem, _days, _series
    ) { reports, latest, item, days, series ->
        LabUiState(reports, latest, item, series, days)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LabUiState())

    init {
        viewModelScope.launch { refreshSeries() }
    }

    fun setSeriesItem(key: String) {
        _seriesItem.value = key
        viewModelScope.launch { refreshSeries() }
    }

    fun setDays(days: Int?) {
        _days.value = days
        viewModelScope.launch { refreshSeries() }
    }

    private suspend fun refreshSeries() {
        _series.value = repo.series(_seriesItem.value, _days.value)
    }

    // ---------------- OCR ----------------
    sealed class OcrState {
        object Idle : OcrState()
        object Loading : OcrState()
        data class Done(val text: String, val values: Map<String, Double>) : OcrState()
        data class Error(val message: String) : OcrState()
    }

    private val _ocrState = MutableStateFlow<OcrState>(OcrState.Idle)
    val ocrState: StateFlow<OcrState> = _ocrState.asStateFlow()

    /** 识别化验单照片；识别结果只解析不入厙，需用户在表单确认 */
    fun recognize(uri: Uri) {
        viewModelScope.launch {
            _ocrState.value = OcrState.Loading
            runCatching {
                val image = OcrImagePreprocessor.preprocess(appContext, uri)
                val result = withContext(Dispatchers.IO) {
                    Tasks.await(TextRecognizerProvider.client.process(image))
                }
                val text = result.text
                val values = OcrParser.parse(text)
                _ocrState.value = OcrState.Done(text, values)
            }.onFailure { e ->
                _ocrState.value = OcrState.Error(TextRecognizerProvider.errorMessage(e))
            }
        }
    }

    fun resetOcr() { _ocrState.value = OcrState.Idle }

    /** 拍照用临时 Uri（经 FileProvider 授权给系统相机） */
    fun createTempImageUri(): Uri? = try {
        val dir = java.io.File(appContext.cacheDir, "images").apply { mkdirs() }
        val file = java.io.File(dir, "lab_${System.currentTimeMillis()}.jpg")
        androidx.core.content.FileProvider.getUriForFile(
            appContext, "${appContext.packageName}.fileprovider", file
        )
    } catch (e: Exception) {
        null
    }

    // ---------------- 表单 ----------------
    /** 新报告；带 OCR 原文时标记为拍照来源 */
    fun newReport(rawText: String = ""): LabReport = LabReport(
        date = todayStr(),
        time = nowTimeStr(),
        hospital = "",
        source = if (rawText.isNotBlank()) "photo" else "manual",
        photo = "",
        rawText = rawText,
        note = "",
        createdAt = nowStamp()
    )

    suspend fun load(id: Long): LabReport? = repo.get(id)

    fun save(report: LabReport, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.save(report)
            onDone()
        }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }

    fun items() = CbcItems.items()
    fun item(key: String) = CbcItems.get(key)
}
