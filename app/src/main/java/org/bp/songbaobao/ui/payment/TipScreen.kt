package org.bp.songbaobao.ui.payment

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alipay.sdk.app.PayTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipScreen(viewModel: PaymentViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()

    var selectedChannel by remember { mutableStateOf(PaymentConfig.CHANNEL_WECHAT) }
    var amountFen by remember { mutableStateOf(600) }

    val amounts = listOf(600, 1800, 6600, 18800, 66000)
    val yuan = amountFen / 100

    Scaffold(topBar = { TopAppBar(title = { Text("打赏开发者") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("如果这个应用对你有帮助，请请我喝杯咖啡 ☕", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))

            Text("金额", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                amounts.forEach { fen ->
                    FilterChip(
                        selected = amountFen == fen,
                        onClick = { amountFen = fen },
                        label = { Text("¥${fen / 100}") }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Text("支付方式", style = MaterialTheme.typography.titleMedium)
            ChannelRow("微信支付", PaymentConfig.CHANNEL_WECHAT, selectedChannel) { selectedChannel = it }
            ChannelRow("支付宝", PaymentConfig.CHANNEL_ALIPAY, selectedChannel) { selectedChannel = it }
            ChannelRow("PayPal", PaymentConfig.CHANNEL_PAYPAL, selectedChannel) { selectedChannel = it }
            Spacer(Modifier.height(24.dp))

            Button(
                enabled = ui.status != PayStatus.Creating && ui.status != PayStatus.Polling,
                onClick = {
                    viewModel.createOrder(
                        selectedChannel,
                        PaymentConfig.PRODUCT_TIP,
                        amountFen,
                        "打赏 ¥$yuan"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("打赏 ¥$yuan") }

            Spacer(Modifier.height(12.dp))

            when (ui.status) {
                PayStatus.Creating -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(8.dp))
                        Text("正在创建订单…")
                    }
                }
                PayStatus.Ready -> {
                    // 拿到拉起参数，拉起对应收银台并立即开始轮询确认
                    LaunchedEffect(ui.outTradeNo) {
                        launchPay(selectedChannel, ui, context, activity, viewModel, scope)
                    }
                }
                PayStatus.Polling -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(8.dp))
                        Text("正在确认支付结果…", color = MaterialTheme.colorScheme.primary)
                    }
                }
                PayStatus.Paid -> Text("✅ ${ui.message}", color = MaterialTheme.colorScheme.primary)
                PayStatus.Failed -> Text("❌ ${ui.error}", color = MaterialTheme.colorScheme.error)
                else -> {}
            }
        }
    }
}

@Composable
private fun ChannelRow(name: String, value: String, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected == value, onClick = { onSelect(value) })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected == value, onClick = { onSelect(value) })
        Spacer(Modifier.width(8.dp))
        Text(name)
    }
}

/**
 * 根据渠道拉起对应收银台：
 *  - 微信：WXAPI.sendReq
 *  - 支付宝：PayTask.payV2（必须在非 UI 线程调用）
 *  - PayPal：用 Custom Tabs 打开后端给的 approvalUrl
 * 拉起后统一调用 startConfirm 轮询后端，以后端结果作为「已收款」唯一依据。
 */
private suspend fun launchPay(
    channel: String,
    ui: PaymentUiState,
    context: android.content.Context,
    activity: ComponentActivity?,
    viewModel: PaymentViewModel,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val outTradeNo = ui.outTradeNo
    when (channel) {
        PaymentConfig.CHANNEL_WECHAT -> {
            WechatPayClient.ensureInitialized(context)
            WechatPayClient.pay(ui.payParams)
            viewModel.startConfirm(outTradeNo)
        }
        PaymentConfig.CHANNEL_ALIPAY -> {
            val orderInfo = ui.payParams["orderInfo"].orEmpty()
            activity?.let {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        // payV2 内部会联网并弹出支付宝收银台，须在非 UI 线程调用
                        PayTask(it).payV2(orderInfo, true)
                    }
                    viewModel.startConfirm(outTradeNo)
                }
            } ?: viewModel.startConfirm(outTradeNo)
        }
        PaymentConfig.CHANNEL_PAYPAL -> {
            val url = ui.payParams["approvalUrl"].orEmpty()
            runCatching { CustomTabsIntent.Builder().build().launchUrl(context, url.toUri()) }
            viewModel.startConfirm(outTradeNo)
        }
    }
}

/** 支付宝回调需要的 Activity 透传（保留以便后续处理 payV2 结果解析）。 */
@Suppress("unused")
private fun alipayResult(intent: Intent?): String? = intent?.getStringExtra("result")
