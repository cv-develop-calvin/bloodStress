@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.bp.songbaobao.ui.screen.glucose

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
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
import org.bp.songbaobao.R
import org.bp.songbaobao.data.local.dao.GlucoseStats
import org.bp.songbaobao.data.local.entity.GlucoseRecord
import org.bp.songbaobao.data.repository.GlucoseRepository
import org.bp.songbaobao.domain.GlucoseOcrParser
import org.bp.songbaobao.domain.GlucoseOcrResultHolder
import org.bp.songbaobao.domain.OcrImagePreprocessor
import org.bp.songbaobao.domain.TextRecognizerProvider
import org.bp.songbaobao.ui.components.GlucoseContext
import org.bp.songbaobao.util.nowStamp
import org.bp.songbaobao.util.nowTimeStr
import org.bp.songbaobao.util.todayStr
import javax.inject.Inject

data class GlucoseUiState(
    val days: Int? = 30,
    val trend: List<GlucoseRecord> = emptyList(),
    val stats: GlucoseStats? = null,
    val recent: List<GlucoseRecord> = emptyList(),
    val latest: GlucoseRecord? = null
)

@HiltViewModel
class GlucoseViewModel @Inject constructor(
    private val repo: GlucoseRepository,
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** 编辑页从导航参数拿到的记录 id（新增页为 null） */
    val editId: Long? = savedStateHandle.get<Long>("id")?.takeIf { it != 0L }

    private val _days = MutableStateFlow<Int?>(30)

    val uiState: StateFlow<GlucoseUiState> = _days.flatMapLatest { days ->
        combine(repo.trend(days), repo.stats(days), repo.recent(5)) { trend, stats, recent ->
            GlucoseUiState(
                days = days,
                trend = trend,
                stats = stats,
                recent = recent,
                latest = recent.firstOrNull()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GlucoseUiState())

    fun setDays(days: Int?) { _days.value = days }

    /** 列表页按日期区间过滤 */
    fun recordsFlow(from: String, to: String): Flow<List<GlucoseRecord>> = repo.list(from, to)

    // ---------------- 表单 ----------------
    fun emptyRecord(): GlucoseRecord = GlucoseRecord(
        date = todayStr(), time = nowTimeStr(),
        value = 5.5f, context = GlucoseContext.FASTING.key,
        note = "", createdAt = nowStamp()
    )

    suspend fun load(id: Long): GlucoseRecord? = repo.get(id)

    fun save(record: GlucoseRecord, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.save(record)
            onDone()
        }
    }

    fun delete(id: Long, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(id)
            onDone()
        }
    }

    // ---------------- 拍照识别（OCR） ----------------
    sealed class OcrState {
        object Idle : OcrState()
        object Loading : OcrState()
        data class Error(val message: String) : OcrState()
        data class Done(val result: GlucoseOcrParser.GlucoseOcrResult) : OcrState()
    }

    private val _ocrState = MutableStateFlow<OcrState>(OcrState.Idle)
    val ocrState: StateFlow<OcrState> = _ocrState.asStateFlow()

    /** 识别照片中的血糖仪读数，解析为血糖值（mmol/L）。 */
    fun recognize(uri: Uri) {
        _ocrState.value = OcrState.Loading
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val image = OcrImagePreprocessor.preprocess(appContext, uri)
                    val vision = Tasks.await(TextRecognizerProvider.client.process(image))
                    GlucoseOcrParser.parse(vision.text)
                }
                _ocrState.value = OcrState.Done(result)
            } catch (e: Exception) {
                _ocrState.value = OcrState.Error(TextRecognizerProvider.errorMessage(e))
            }
        }
    }

    fun resetOcr() { _ocrState.value = OcrState.Idle }

    /** 从 OCR 结果预填表单（仅一次）。 */
    fun consumeOcrPrefill(): GlucoseRecord? {
        val r = GlucoseOcrResultHolder.consume() ?: return null
        return emptyRecord().copy(value = r.value ?: 5.5f)
    }

    /** 创建供系统相机写入的临时图片 Uri（与血压一致，使用 FileProvider）。 */
    fun createTempImageUri(): Uri? = try {
        val file = File(appContext.cacheDir, "glucose_${nowStamp()}.jpg")
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
    } catch (e: Exception) { null }
}
