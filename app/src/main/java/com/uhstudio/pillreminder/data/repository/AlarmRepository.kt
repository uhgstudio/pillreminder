package com.uhstudio.pillreminder.data.repository

import android.content.Context
import com.uhstudio.pillreminder.data.dao.PillAlarmDao
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import com.uhstudio.pillreminder.util.Result
import com.uhstudio.pillreminder.util.ValidationResult
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.DayOfWeek

/**
 * 알람 관리를 위한 Repository
 * 알람 데이터베이스 접근과 시스템 알람 스케줄링을 담당
 */
class AlarmRepository(
    private val pillAlarmDao: PillAlarmDao,
    private val alarmManager: AlarmManagerUtil
) {
    /**
     * 모든 알람 목록을 Flow로 반환
     */
    fun getAllAlarms(): Flow<List<PillAlarm>> {
        return pillAlarmDao.getAllAlarms()
    }

    /**
     * 활성화된 알람 목록을 Flow로 반환
     */
    fun getEnabledAlarms(): Flow<List<PillAlarm>> {
        return pillAlarmDao.getEnabledAlarms()
    }

    /**
     * 특정 약의 알람 목록을 Flow로 반환
     */
    fun getAlarmsForPill(pillId: String): Flow<List<PillAlarm>> {
        return pillAlarmDao.getAlarmsForPill(pillId)
    }

    /**
     * 특정 요일의 알람 목록을 Flow로 반환
     */
    fun getAlarmsForDay(dayOfWeek: DayOfWeek): Flow<List<PillAlarm>> {
        return pillAlarmDao.getAlarmsForDay(dayOfWeek)
    }

    /**
     * ID로 알람 조회
     */
    suspend fun getAlarmById(alarmId: String): Result<PillAlarm> {
        return try {
            val alarm = pillAlarmDao.getAlarmById(alarmId)
            if (alarm != null) {
                Timber.d("getAlarmById: alarm found - $alarmId")
                Result.Success(alarm)
            } else {
                Timber.w("getAlarmById: alarm not found - $alarmId")
                Result.Error(
                    Exception("Alarm not found"),
                    "알람 정보를 찾을 수 없습니다."
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get alarm by id: $alarmId")
            Result.Error(e, "알람 정보를 불러오는데 실패했습니다.")
        }
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
     * 알람 추가 및 스케줄링
     */
    suspend fun addAlarm(alarm: PillAlarm): Result<Unit> {
        // 입력 검증
        val validation = validateAlarmInput(alarm.hour, alarm.minute, alarm.repeatDays)
        if (validation.isInvalid) {
            val errorMessage = validation.errorMessages().joinToString("\n")
            Timber.e("Alarm validation failed: $errorMessage")
            return Result.Error(
                Exception("Validation failed"),
                errorMessage
            )
        }

        return try {
            // 데이터베이스에 저장
            pillAlarmDao.insertAlarm(alarm)
            Timber.d("addAlarm: alarm saved to DB - ${alarm.id}")

            // 시스템 알람 스케줄링
            if (alarm.enabled) {
                val scheduled = alarmManager.scheduleAlarm(alarm)
                if (!scheduled) {
                    Timber.w("addAlarm: failed to schedule alarm - ${alarm.id}")
                    return Result.Error(
                        Exception("Failed to schedule alarm"),
                        "알람 권한이 필요합니다. 설정에서 알람 권한을 허용해주세요."
                    )
                }
                Timber.d("addAlarm: alarm scheduled - ${alarm.id}")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add alarm: ${alarm.id}")
            Result.Error(e, "알람을 저장하는데 실패했습니다.")
        }
    }

    /**
     * 알람 수정 및 재스케줄링
     */
    suspend fun updateAlarm(alarm: PillAlarm): Result<Unit> {
        // 입력 검증
        val validation = validateAlarmInput(alarm.hour, alarm.minute, alarm.repeatDays)
        if (validation.isInvalid) {
            val errorMessage = validation.errorMessages().joinToString("\n")
            Timber.e("Alarm validation failed: $errorMessage")
            return Result.Error(
                Exception("Validation failed"),
                errorMessage
            )
        }

        return try {
            // 기존 알람 취소
            alarmManager.cancelAlarm(alarm)
            Timber.d("updateAlarm: old alarm cancelled - ${alarm.id}")

            // 데이터베이스 업데이트
            pillAlarmDao.updateAlarm(alarm)
            Timber.d("updateAlarm: alarm updated in DB - ${alarm.id}")

            // 새 알람 스케줄링
            if (alarm.enabled) {
                val scheduled = alarmManager.scheduleAlarm(alarm)
                if (!scheduled) {
                    Timber.w("updateAlarm: failed to schedule alarm - ${alarm.id}")
                    return Result.Error(
                        Exception("Failed to schedule alarm"),
                        "알람 권한이 필요합니다. 설정에서 알람 권한을 허용해주세요."
                    )
                }
                Timber.d("updateAlarm: alarm rescheduled - ${alarm.id}")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update alarm: ${alarm.id}")
            Result.Error(e, "알람을 수정하는데 실패했습니다.")
        }
    }

    /**
     * 알람 활성화/비활성화
     */
    suspend fun toggleAlarmEnabled(alarmId: String, enabled: Boolean): Result<Unit> {
        return try {
            val alarm = pillAlarmDao.getAlarmById(alarmId)
            if (alarm == null) {
                Timber.w("toggleAlarmEnabled: alarm not found - $alarmId")
                return Result.Error(
                    Exception("Alarm not found"),
                    "알람 정보를 찾을 수 없습니다."
                )
            }

            // 데이터베이스 업데이트
            pillAlarmDao.updateAlarmEnabled(alarmId, enabled)
            Timber.d("toggleAlarmEnabled: alarm enabled=$enabled - $alarmId")

            // 시스템 알람 처리
            if (enabled) {
                val scheduled = alarmManager.scheduleAlarm(alarm.copy(enabled = true))
                if (!scheduled) {
                    Timber.w("toggleAlarmEnabled: failed to schedule alarm - $alarmId")
                    return Result.Error(
                        Exception("Failed to schedule alarm"),
                        "알람 권한이 필요합니다."
                    )
                }
            } else {
                alarmManager.cancelAlarm(alarm)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle alarm enabled: $alarmId")
            Result.Error(e, "알람 상태를 변경하는데 실패했습니다.")
        }
    }

    /**
     * 알람 삭제
     */
    suspend fun deleteAlarm(alarm: PillAlarm): Result<Unit> {
        return try {
            // 시스템 알람 취소
            alarmManager.cancelAlarm(alarm)
            Timber.d("deleteAlarm: system alarm cancelled - ${alarm.id}")

            // 데이터베이스에서 삭제
            pillAlarmDao.deleteAlarm(alarm)
            Timber.d("deleteAlarm: alarm deleted from DB - ${alarm.id}")

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete alarm: ${alarm.id}")
            Result.Error(e, "알람을 삭제하는데 실패했습니다.")
        }
    }

    /**
     * 모든 알람 재스케줄링 (재부팅 후 등)
     */
    suspend fun rescheduleAllAlarms(): Result<Int> {
        return try {
            val alarms = pillAlarmDao.getEnabledAlarmsOnce()
            var successCount = 0

            for (alarm in alarms) {
                val scheduled = alarmManager.scheduleAlarm(alarm)
                if (scheduled) {
                    successCount++
                } else {
                    Timber.w("rescheduleAllAlarms: failed to schedule - ${alarm.id}")
                }
            }

            Timber.d("rescheduleAllAlarms: $successCount/${alarms.size} alarms rescheduled")
            Result.Success(successCount)
        } catch (e: Exception) {
            Timber.e(e, "Failed to reschedule all alarms")
            Result.Error(e, "알람 재설정에 실패했습니다.")
        }
    }
}
