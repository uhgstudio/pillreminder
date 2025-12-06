package com.uhstudio.pillreminder.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 기기 부팅 완료 후 모든 활성화된 알람을 다시 스케줄링하는 BroadcastReceiver
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    rescheduleAllAlarms(context)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun rescheduleAllAlarms(context: Context) {
        val database = PillReminderDatabase.getDatabase(context)
        val alarmDao = database.pillAlarmDao()
        val alarmManagerUtil = AlarmManagerUtil(context)

        // 활성화된 모든 알람 가져오기
        val enabledAlarms = alarmDao.getEnabledAlarmsOnce()

        // 각 알람 다시 스케줄링
        enabledAlarms.forEach { alarm ->
            alarmManagerUtil.scheduleAlarm(alarm)
        }
    }
}
