package org.bp.songbaobao.data.repository

import org.bp.songbaobao.ui.payment.PaymentConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** 后端返回的下单结果：拉起支付所需的参数都在 payParams 里。 */
data class CreateOrderResponse(
    val outTradeNo: String,
    val channel: String,
    val payParams: Map<String, String>
)

/** 后端返回的订单状态：以 PAID 为「已收款」唯一权威判定。 */
data class OrderStatus(
    val outTradeNo: String,
    val status: String // UNPAID | PAID | FAILED | CLOSED
)

/**
 * 支付仓储：只跟「你们自己的后端」打交道，绝不直接持有微信/支付宝密钥。
 * 后端负责调用各支付网关下单，并把拉起收银台所需的参数回传。
 */
@Singleton
class PaymentRepository @Inject constructor() {

    private val executor = Executors.newSingleThreadExecutor()

    /** 创建订单：App -> 你们后端 -> 微信/支付宝/PayPal 网关。 */
    suspend fun createOrder(
        channel: String,
        productId: String,
        amountFen: Int,
        title: String
    ): Result<CreateOrderResponse> = kotlin.runCatching {
        // 金额以「分」为单位传给后端，后端据此调网关，杜绝客户端篡改金额。
        val body = JSONObject().apply {
            put("channel", channel)
            put("productId", productId)
            put("amount", amountFen)
            put("title", title)
        }.toString()

        val json = postJson(PaymentConfig.BASE_URL + PaymentConfig.ENDPOINT_CREATE_ORDER, body)
        val root = JSONObject(json)
        val params = root.optJSONObject("payParams") ?: JSONObject()
        val map = mutableMapOf<String, String>()
        params.keys().forEach { map[it] = params.getString(it) }
        CreateOrderResponse(
            outTradeNo = root.getString("outTradeNo"),
            channel = root.optString("channel", channel),
            payParams = map
        )
    }

    /** 查询订单状态：轮询用。前端 SDK 回调不可信，以这里为准。 */
    suspend fun queryOrder(outTradeNo: String): Result<OrderStatus> = kotlin.runCatching {
        val json = getJson(PaymentConfig.BASE_URL + PaymentConfig.ENDPOINT_QUERY_ORDER + outTradeNo)
        val root = JSONObject(json)
        OrderStatus(
            outTradeNo = root.getString("outTradeNo"),
            status = root.getString("status")
        )
    }

    private fun postJson(url: String, body: String): String = withTimeout(15_000) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            doOutput = true
            outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }
        readResponse(conn)
    }

    private fun getJson(url: String): String = withTimeout(15_000) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        readResponse(conn)
    }

    private fun readResponse(conn: HttpURLConnection): String {
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (code !in 200..299) throw RuntimeException("HTTP $code: $text")
            return text
        } finally {
            conn.disconnect()
        }
    }

    /** 阻塞式 I/O 无法被协程超时打断，用独立线程 + Future 强制限时。 */
    private fun <T> withTimeout(timeoutMs: Long, block: () -> T): T {
        val future = executor.submit<T> { block() }
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } finally {
            future.cancel(true)
        }
    }
}
