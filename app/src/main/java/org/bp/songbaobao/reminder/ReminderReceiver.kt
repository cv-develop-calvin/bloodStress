package org.bp.songbaobao.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bp.songbaobao.data.repository.MedRepository
import org.bp.songbaobao.util.parseTimes
import org.bp.songbaobao.util.todayStr
import javax.inject.Inject

/**
 * 闹钟触发：校验该药仍启用、且该时段未打卡，然后发通知。
 * 使用 goAsync 保证协程能执行完。
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var medRepository: MedRepository

    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getLongExtra("medId", -1L)
        val slot = intent.getStringExtra("slot") ?: return
        val name = intent.getStringExtra("name") ?: "药品"
        val dosage = intent.getStringExtra("dosage") ?: ""

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val med = medRepository.get(medId)
                // 药品已删除或停用则不提醒
                if (med == null || !med.active) return@launch
                // 该时间点不在当前计划里（已修改时间）则不提醒
                if (slot !in parseTimes(med.times)) return@launch
                // 今天已打卡则不再提醒
                val taken = medRepository.takenSlots(todayStr())
                if ((med.id to slot) in taken) return@launch

                val text = buildString {
                    append(name)
                    if (dosage.isNotBlank()) append("  $dosage")
                    append("  （$slot）")
                }
                NotificationHelper.showReminder(context, text, medId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
