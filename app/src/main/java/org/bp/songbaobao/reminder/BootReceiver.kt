package org.bp.songbaobao.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.bp.songbaobao.data.repository.MedRepository
import javax.inject.Inject

/** 开机后重新注册全部提醒（闹钟在重启后会被清空） */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var medRepository: MedRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val meds = medRepository.all().first()
                AlarmScheduler.rescheduleAll(context, meds)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
