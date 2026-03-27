package com.uhstudio.pillreminder.ui.addAlarm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import com.uhstudio.pillreminder.util.ValidationResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class AddAlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val alarmDao = database.pillAlarmDao()
    private val alarmManager = AlarmManagerUtil(application)
    private val adManager = AdManager.getInstance(application)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // 로딩 상태 (광고 로드 중 등)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 중복 저장 방지 (thread-safe)
    private val _isSaving = MutableStateFlow(false)

    suspend fun getAlarm(alarmId: String): PillAlarm? {
        return alarmDao.getAlarmById(alarmId)
    }

    /**
     * 알람 입력값 검증
     */
    private fun validateAlarmInput(
        hour: Int,
        minute: Int,
        repeatDays: Set<DayOfWeek>
    ): ValidationResult<Unit> {
        val errors = mutableListOf<String>()

        // 시간 검증 (0-23)
        if (hour !in 0..23) {
            errors.add("시간은 0~23 사이여야 합니다. (입력값: $hour)")
            Timber.w("Invalid hour: $hour")
        }

        // 분 검증 (0-59)
        if (minute !in 0..59) {
            errors.add("분은 0~59 사이여야 합니다. (입력값: $minute)")
            Timber.w("Invalid minute: $minute")
        }

        // 반복 요일 검증 (빈 Set 아님)
        if (repeatDays.isEmpty()) {
            errors.add("최소 하나의 요일을 선택해야 합니다.")
            Timber.w("Empty repeatDays")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid(Unit)
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    /**
     * 새로운 스케줄 시스템으로 알람 저장
     */
    fun saveAlarmWithSchedule(
        pillId: String,
        hour: Int,
        minute: Int,
        scheduleType: ScheduleType,
        scheduleConfig: ScheduleConfig,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        alarmSoundUri: String? = null,
        alarmId: String? = null,
        onComplete: () -> Unit,
        onShowAd: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 중복 저장 방지
        if (_isSaving.value) {
            Timber.w("이미 저장 중입니다")
            return
        }
        _isSaving.value = true

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 시간 검증
                if (hour !in 0..23 || minute !in 0..59) {
                    _isSaving.value = false
                    _isLoading.value = false
                    onError("유효하지 않은 시간입니다.")
                    return@launch
                }

                // ScheduleConfig를 JSON으로 직렬화
                val scheduleConfigJson = json.encodeToString(scheduleConfig)

                val now = LocalDateTime.now()
                val isEditMode = alarmId != null
                
                val alarm = PillAlarm(
                    id = alarmId ?: UUID.randomUUID().toString(),
                    pillId = pillId,
                    hour = hour,
                    minute = minute,
                    scheduleType = scheduleType,
                    scheduleConfig = scheduleConfigJson,
                    startDate = startDate,
                    endDate = endDate,
                    repeatDays = emptySet(), // 레거시 필드, 빈 Set
                    enabled = true,
                    alarmSoundUri = alarmSoundUri,
                    createdAt = now,
                    updatedAt = now
                )

                Timber.d("Saving alarm with schedule: type=$scheduleType, config=$scheduleConfigJson")

                // 기존 알람이 있으면 먼저 취소
                if (isEditMode) {
                    alarmDao.getAlarmById(alarmId!!)?.let { oldAlarm ->
                        alarmManager.cancelAlarm(oldAlarm)
                    }
                }

                alarmDao.insertAlarm(alarm)
                val scheduled = alarmManager.scheduleAlarm(alarm)

                if (!scheduled) {
                    Timber.w("Failed to schedule alarm, may have no next occurrence")
                }
                
                // 생성 모드일 때는 광고 체크 없이 바로 종료 (Speed Optimization)
                if (!isEditMode) {
                   _isLoading.value = false
                   onComplete()
                   return@launch
                }

                // 수정 모드일 때만 광고 체크
                val shouldShow = adManager.incrementAndCheckAlarmRegistration()
                if (shouldShow) {
                    // 광고 로드 후 표시
                    adManager.loadInterstitialAd(
                        onAdLoaded = {
                            _isLoading.value = false
                            onShowAd()
                        },
                        onAdFailed = {
                            // 광고 로드 실패 시 바로 화면 종료
                            _isLoading.value = false
                            onComplete()
                        }
                    )
                } else {
                    // 광고 표시 안함 - 바로 화면 종료
                    _isLoading.value = false
                    onComplete()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save alarm")
                _isLoading.value = false
                onError("알람 저장 실패: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * 기존 방식의 알람 저장 (하위 호환성)
     * 내부적으로는 새 스케줄 시스템 사용
     */
    fun saveAlarm(
        pillId: String,
        hour: Int,
        minute: Int,
        repeatDays: Set<DayOfWeek>,
        alarmSoundUri: String? = null,
        alarmId: String? = null,
        onComplete: () -> Unit,
        onShowAd: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 입력 검증
        val validationResult = validateAlarmInput(hour, minute, repeatDays)
        if (validationResult.isInvalid) {
            val errorMessage = validationResult.errorMessages().joinToString("\n")
            Timber.e("Alarm validation failed: $errorMessage")
            onError(errorMessage)
            return
        }

        // repeatDays를 새 스케줄 시스템으로 변환
        val scheduleConfig = ScheduleConfig.Weekly.from(repeatDays)

        // 새 스케줄 시스템 사용
        saveAlarmWithSchedule(
            pillId = pillId,
            hour = hour,
            minute = minute,
            scheduleType = ScheduleType.WEEKLY,
            scheduleConfig = scheduleConfig,
            alarmSoundUri = alarmSoundUri,
            alarmId = alarmId,
            onComplete = onComplete,
            onShowAd = onShowAd,
            onError = onError
        )
    }
} 