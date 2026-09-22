package org.bp.songbaobao.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.bp.songbaobao.data.local.entity.Medication
import org.bp.songbaobao.util.parseTimes
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 用药提醒调度：为每个药品的每个服药时间点注册一个精准闹钟。
 * 使用 setExactAndAllowWhileIdle，尽量在低电耗模式下也能触发。
 */
object AlarmScheduler {

    private fun requestCode(medId: Long, slotIndex: Int): Int =
        (medId * 100 + slotIndex).toInt().let { if (it < 0) -it else it }

    private fun pendingIntent(context: Context, med: Medication, slotIndex: Int, slot: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("medId", med.id)
            putExtra("slot", slot)
            putExtra("name", med.name)
            putExtra("dosage", "${med.dosage}${med.unit}")
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(med.id, slotIndex),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
        )
    }

    private fun immutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0

    /** 注册某药品的全部提醒 */
    fun schedule(context: Context, med: Medication) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) return
        }
        if (!med.active) return

        parseTimes(med.times).forEachIndexed { index, slot ->
            val trigger = nextTriggerMillis(slot)
            val pi = pendingIntent(context, med, index, slot)
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } catch (e: SecurityException) {
                // 没有精准闹钟权限时退化为普通闹钟
                am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        }
    }

    fun cancel(context: Context, med: Medication) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        parseTimes(med.times).forEachIndexed { index, slot ->
            am.cancel(pendingIntent(context, med, index, slot))
        }
    }

    /** 重新注册全部药品的提醒（App 启动或数据变更后调用） */
    fun rescheduleAll(context: Context, meds: List<Medication>) {
        meds.forEach { schedule(context, it) }
    }

    /** 计算某个时间点下一次触发的时间戳（今天已过则顺延到明天） */
    private fun nextTriggerMillis(slot: String): Long {
        val (h, m) = slot.split(":").let { it[0].toInt() to it[1].toInt() }
        var trigger = LocalDate.now()
            .atTime(h, m)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        if (trigger <= System.currentTimeMillis()) {
            trigger += 24 * 60 * 60 * 1000
        }
        return trigger
    }

    fun nowMillis() = System.currentTimeMillis()

    fun nowDateTime(): LocalDateTime = LocalDateTime.now()
}
