package com.uhstudio.pillreminder.ui.alarms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AlarmsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()
    private val pillDao = database.pillDao()
    private val alarmManagerUtil = AlarmManagerUtil(application)
    private val adManager = AdManager.getInstance(application)

    val allAlarms: Flow<List<PillAlarm>> = alarmDao.getAllAlarms()
    val allPills = pillDao.getAllPills()

    fun getEnabledAlarms(): Flow<List<PillAlarm>> {
        return alarmDao.getEnabledAlarms()
    }

    fun toggleAlarm(alarm: PillAlarm, enabled: Boolean) {
        viewModelScope.launch {
            // 데이터베이스 업데이트
            alarmDao.updateAlarmEnabled(alarm.id, enabled)

            // 알람 스케줄/취소
            if (enabled) {
                val updatedAlarm = alarm.copy(enabled = true)
                alarmManagerUtil.scheduleAlarm(updatedAlarm)
            } else {
                alarmManagerUtil.cancelAlarm(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: PillAlarm, onShowAd: () -> Unit = {}) {
        viewModelScope.launch {
            alarmManagerUtil.cancelAlarm(alarm)
            alarmDao.deleteAlarm(alarm)

            // 삭제 완료 후 광고 표시 체크
            val shouldShow = adManager.incrementAndCheckAlarmRegistration()
            if (shouldShow) {
                adManager.loadInterstitialAd {
                    onShowAd()
                }
            }
        }
    }
} 