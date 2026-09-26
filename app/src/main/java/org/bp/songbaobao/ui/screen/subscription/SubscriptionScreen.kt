package org.bp.songbaobao.ui.screen.subscription

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bp.songbaobao.R
import org.bp.songbaobao.ui.components.GoldButton
import org.bp.songbaobao.ui.components.HeroCard
import org.bp.songbaobao.ui.components.PanelCard
import org.bp.songbaobao.ui.theme.*
import org.bp.songbaobao.util.UrlLauncher
import org.bp.songbaobao.util.UserPrefs
import java.text.SimpleDateFormat
import java.util.*

/** PayPal 无代码收款链接（NCP 托管支付页）。 */
private const val PAYPAL_URL = "https://www.paypal.com/ncp/payment/W69K9AXU73S7C"

private data class Plan(
    val id: String,
    val titleRes: Int,
    val priceRes: Int,
    val periodRes: Int,
    val usdRes: Int? = null,
    val recommended: Boolean = false,
    /** 订阅时长（月）；-1 表示终身/一次性永久。 */
    val months: Int
)

/** 固定一次性会员：¥70（约 $10），永久有效。 */
private val PLANS = listOf(
    Plan(
        "onetime",
        R.string.sub_plan_onetime,
        R.string.sub_plan_onetime_price,
        R.string.sub_plan_onetime_period,
        usdRes = R.string.sub_plan_usd,
        recommended = true,
        months = -1
    )
)

@Composable
fun SubscriptionScreen() {
    val context = LocalContext.current
    val active by UserPrefs.subscriptionActive.collectAsState()
    val planId by UserPrefs.subscriptionPlan.collectAsState()
    val expiry by UserPrefs.subscriptionExpiry.collectAsState()

    var selectedId by rememberSaveable { mutableStateOf(PLANS.first().id) }

    val selectedPlan = PLANS.firstOrNull { it.id == selectedId }
    val currentPlan = PLANS.firstOrNull { it.id == planId }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavySoft, Ink)))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // 顶部会员卡
            HeroCard {
                Column(
                    Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        stringResource(R.string.sub_chip),
                        fontSize = 11.sp,
                        color = GoldBright,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text("👑", fontSize = 40.sp)
                    Text(
                        stringResource(R.string.sub_title),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMain
                    )
                    Text(
                        stringResource(R.string.sub_subtitle),
                        fontSize = 12.sp,
                        color = TextDim,
                        letterSpacing = 2.sp
                    )
                }
            }

            // 会员权益
            PanelCard {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        stringResource(R.string.sub_benefits_title),
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        fontSize = 15.sp
                    )
                    listOf(
                        R.string.sub_benefit_1,
                        R.string.sub_benefit_2,
                        R.string.sub_benefit_3,
                        R.string.sub_benefit_4
                    ).forEach { res ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            Text(stringResource(res), color = TextMain, fontSize = 13.sp)
                        }
                    }
                }
            }

            // 套餐选择
            Text(
                stringResource(R.string.sub_select_hint),
                color = TextMain,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            PLANS.forEach { plan ->
                PlanCard(
                    plan = plan,
                    selected = selectedId == plan.id,
                    onClick = { selectedId = plan.id }
                )
            }

            // 支付
            GoldButton(
                onClick = {
                    if (selectedPlan == null) {
                        Toast.makeText(context, context.getString(R.string.sub_please_select), Toast.LENGTH_SHORT).show()
                    } else {
                        UrlLauncher.openOrCopy(
                            context,
                            PAYPAL_URL,
                            context.getString(R.string.sub_open_failed),
                            context.getString(R.string.sub_copied)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sub_pay_paypal), fontWeight = FontWeight.Bold)
            }
            Text(
                stringResource(R.string.sub_paypal_hint),
                color = TextDim,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            // 完成支付后标记
            if (!active) {
                OutlinedButton(
                    onClick = {
                        val plan = selectedPlan
                        if (plan == null) {
                            Toast.makeText(context, context.getString(R.string.sub_please_select), Toast.LENGTH_SHORT).show()
                        } else {
                            UserPrefs.setSubscription(plan.id, true, computeExpiry(plan))
                            Toast.makeText(context, context.getString(R.string.sub_status_active), Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.sub_paid))
                }
            }

            // 订阅状态
            PanelCard {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        stringResource(R.string.sub_status_title),
                        fontWeight = FontWeight.Bold,
                        color = TextMain,
                        fontSize = 15.sp
                    )
                    if (active && currentPlan != null) {
                        Text(
                            stringResource(R.string.sub_status_active),
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.sub_plan_label, stringResource(currentPlan.titleRes)),
                            color = TextMain
                        )
                        Text(
                            stringResource(R.string.sub_expiry, formatExpiry(expiry, stringResource(R.string.sub_forever))),
                            color = TextDim,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = { UserPrefs.clearSubscription() },
                            border = BorderStroke(1.dp, DangerRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                        ) {
                            Text(stringResource(R.string.sub_cancel))
                        }
                    } else {
                        Text(stringResource(R.string.sub_status_inactive), color = TextDim)
                    }
                }
            }

            Text(
                stringResource(R.string.sub_note),
                color = TextDim,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlanCard(plan: Plan, selected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) primary.copy(alpha = 0.12f) else PanelBg
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) primary else PanelBorder)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(plan.titleRes),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextMain
                    )
                    if (plan.recommended) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.sub_recommended),
                            fontSize = 10.sp,
                            color = primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        stringResource(plan.priceRes),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(plan.periodRes), fontSize = 12.sp, color = TextDim)
                }
                if (plan.usdRes != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(plan.usdRes!!),
                        fontSize = 12.sp,
                        color = TextDim
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(BorderStroke(1.5.dp, if (selected) primary else TextDim), RoundedCornerShape(50))
                    .background(
                        if (selected) primary else Color.Transparent,
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun computeExpiry(plan: Plan): Long {
    if (plan.months < 0) return Long.MAX_VALUE
    val c = Calendar.getInstance()
    c.add(Calendar.MONTH, plan.months)
    return c.timeInMillis
}

private fun formatExpiry(expiry: Long, forever: String): String {
    if (expiry >= Long.MAX_VALUE / 2) return forever
    return try {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date(expiry))
    } catch (e: Exception) {
        forever
    }
}
