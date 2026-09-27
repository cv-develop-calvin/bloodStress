package org.bp.songbaobao.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import org.bp.songbaobao.ui.payment.PaymentConfig

/**
 * 微信支付回调入口。**包名必须是「应用包名.wxapi.WXPayEntryActivity」**，
 * 微信 App 支付完成后会按此固定类名拉起本 Activity。路径不对则收不到回调。
 *
 * 本类只解析微信回调做提示；真正的「已收款」判定以后端轮询结果为准，
 * 因此这里不负责发货/解锁。
 */
class WXPayEntryActivity : Activity(), IWXAPIEventHandler {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val api: IWXAPI = WXAPIFactory.createWXAPI(this, PaymentConfig.WECHAT_APP_ID, false)
        if (!api.handleIntent(intent, this)) finish()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val api = WXAPIFactory.createWXAPI(this, PaymentConfig.WECHAT_APP_ID, false)
        api.handleIntent(intent, this)
    }

    override fun onReq(req: BaseReq?) {
        // 本应用不需要处理微信发起的 req
    }

    override fun onResp(resp: BaseResp) {
        val msg = when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> "微信支付完成，正在确认收款…"
            BaseResp.ErrCode.ERR_USER_CANCEL -> "已取消微信支付"
            BaseResp.ErrCode.ERR_AUTH_DENIED -> "微信授权被拒绝"
            else -> "微信支付异常(${resp.errCode})"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        finish()
    }
}
