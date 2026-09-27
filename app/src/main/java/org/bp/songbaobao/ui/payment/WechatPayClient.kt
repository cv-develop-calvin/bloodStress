package org.bp.songbaobao.ui.payment

import android.content.Context
import com.tencent.mm.opensdk.modelpay.PayReq
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

/**
 * 微信支付客户端（单例）。持有 IWXAPI 用于拉起微信收银台。
 * 注意：APPID 必须在微信开放平台登记，且包名 + 签名与商户平台一致，否则拉起失败。
 */
object WechatPayClient {
    private var api: IWXAPI? = null

    fun ensureInitialized(context: Context) {
        if (api == null) {
            api = WXAPIFactory.createWXAPI(context.applicationContext, PaymentConfig.WECHAT_APP_ID, true)
            api?.registerApp(PaymentConfig.WECHAT_APP_ID)
        }
    }

    /** 用后端返回的 payParams 拉起微信支付。返回 false 表示未初始化/参数缺失。 */
    fun pay(payParams: Map<String, String>): Boolean {
        val a = api ?: return false
        val req = PayReq().apply {
            appId = payParams["appId"].orEmpty()
            partnerId = payParams["partnerId"].orEmpty()
            prepayId = payParams["prepayId"].orEmpty()
            nonceStr = payParams["nonceStr"].orEmpty()
            timeStamp = payParams["timeStamp"].orEmpty()
            packageValue = payParams["packageValue"].orEmpty() // 字段名就是 packageValue
            sign = payParams["sign"].orEmpty()
            extData = payParams["extData"].orEmpty()
        }
        return a.sendReq(req)
    }
}
