package org.bp.songbaobao.ui.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bp.songbaobao.data.repository.OrderStatus
import org.bp.songbaobao.data.repository.PaymentRepository
import javax.inject.Inject

enum class PayStatus { Idle, Creating, Ready, Polling, Paid, Failed }

data class PaymentUiState(
    val status: PayStatus = PayStatus.Idle,
    val channel: String = "",
    val amountFen: Int = 0,
    val outTradeNo: String = "",
    val payParams: Map<String, String> = emptyMap(),
    val error: String = "",
    val message: String = ""
)

/**
 * 支付状态机。流程：
 *  createOrder -> Ready（拿到 payParams）
 *  -> 界面拉起对应收银台 -> startConfirm（轮询后端确认收款）
 *  -> Paid / Failed
 * 注意：微信/支付宝/PayPal 的前端回调只做提示，是否「已收款」一律由后端轮询结果决定。
 */
@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val repo: PaymentRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(PaymentUiState())
    val ui: StateFlow<PaymentUiState> = _ui.asStateFlow()

    private var pollJob: Job? = null

    fun createOrder(channel: String, productId: String, amountFen: Int, title: String) {
        if (_ui.value.status == PayStatus.Creating || _ui.value.status == PayStatus.Ready) return
        _ui.value = _ui.value.copy(
            status = PayStatus.Creating,
            channel = channel,
            amountFen = amountFen,
            error = "",
            message = ""
        )
        viewModelScope.launch {
            repo.createOrder(channel, productId, amountFen, title)
                .onSuccess { resp ->
                    _ui.value = _ui.value.copy(
                        status = PayStatus.Ready,
                        outTradeNo = resp.outTradeNo,
                        payParams = resp.payParams
                    )
                }
                .onFailure { t ->
                    _ui.value = _ui.value.copy(status = PayStatus.Failed, error = t.message ?: "下单失败")
                }
        }
    }

    /** 拉起收银台后调用：开始轮询后端，直到确认 PAID（前端结果不可信）。 */
    fun startConfirm(outTradeNo: String) {
        _ui.value = _ui.value.copy(status = PayStatus.Polling)
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            repeat(POLL_MAX) {
                delay(POLL_INTERVAL_MS)
                if (!isActive) return@launch
                repo.queryOrder(outTradeNo)
                    .onSuccess { if (handleStatus(it)) return@launch }
                // 网络抖动不中断，继续轮询
            }
            if (_ui.value.status == PayStatus.Polling) {
                _ui.value = _ui.value.copy(
                    status = PayStatus.Failed,
                    error = "支付确认超时，请稍后在订单中查看，或联系开发者"
                )
            }
        }
    }

    /** 微信回调（WXPayEntryActivity）可主动触发一次即时查询，加速确认。 */
    fun refreshNow(outTradeNo: String) {
        viewModelScope.launch {
            repo.queryOrder(outTradeNo).onSuccess { handleStatus(it) }
        }
    }

    private fun handleStatus(s: OrderStatus): Boolean = when (s.status) {
        "PAID" -> {
            _ui.value = _ui.value.copy(status = PayStatus.Paid, message = "支付成功，感谢支持！")
            true
        }
        "FAILED", "CLOSED" -> {
            _ui.value = _ui.value.copy(status = PayStatus.Failed, error = "订单未支付或已关闭")
            true
        }
        else -> false
    }

    fun reset() {
        pollJob?.cancel()
        _ui.value = PaymentUiState()
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val POLL_MAX = 40            // 最多轮询次数
        private const val POLL_INTERVAL_MS = 2500L // 轮询间隔
    }
}
