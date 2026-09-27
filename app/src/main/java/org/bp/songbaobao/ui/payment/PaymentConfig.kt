package org.bp.songbaobao.ui.payment

/**
 * 支付相关常量。部署前把 BASE_URL 改成你们真实后端域名（需 HTTPS），
 * WECHAT_APP_ID 换成微信开放平台为这个 App 注册的 AppID。
 */
object PaymentConfig {
    /** 你们后端地址（下单 / 查询订单都走这里，绝不直接连微信/支付宝网关）。 */
    const val BASE_URL = "https://your-backend.example.com"

    /** 微信开放平台注册的 AppID（替换成你们自己的）。 */
    const val WECHAT_APP_ID = "wxYOUR_APPID"

    const val CHANNEL_WECHAT = "wechat"
    const val CHANNEL_ALIPAY = "alipay"
    const val CHANNEL_PAYPAL = "paypal"

    const val ENDPOINT_CREATE_ORDER = "/api/orders"
    const val ENDPOINT_QUERY_ORDER = "/api/orders/" // + outTradeNo

    /** 打赏商品固定 SKU，后端据此识别。 */
    const val PRODUCT_TIP = "tip"
}
