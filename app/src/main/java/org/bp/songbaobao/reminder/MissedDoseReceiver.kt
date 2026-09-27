package org.bp.songbaobao.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bp.songbaobao.data.repository.MedRepository
import org.bp.songbaobao.util.todayStr
import javax.inject.Inject

/**
 * 每天 21:00 触发：扫描当天已过服药时间却未打卡的时段，
 * 写入「漏服」记录并提醒用户补服。
 * 完全本地、离线可用。
 */
@AndroidEntryPoint
class MissedDoseReceiver : BroadcastReceiver() {

    @Inject lateinit var medRepository: MedRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val missed = medRepository.checkMissedDoses(todayStr())
                if (missed.isNotEmpty()) {
                    NotificationHelper.notifyMissed(context, missed)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
