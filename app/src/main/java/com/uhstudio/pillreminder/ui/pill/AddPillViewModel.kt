package com.uhstudio.pillreminder.ui.pill

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uhstudio.pillreminder.ads.AdManager
import com.uhstudio.pillreminder.data.database.PillReminderDatabase
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.util.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import timber.log.Timber
import java.util.UUID
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import com.uhstudio.pillreminder.data.model.PillAlarm
import com.uhstudio.pillreminder.data.model.ScheduleConfig
import com.uhstudio.pillreminder.data.model.ScheduleType
import com.uhstudio.pillreminder.util.AlarmManagerUtil
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class AddPillViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()
    private val alarmDao = database.pillAlarmDao()
    private val alarmManager = AlarmManagerUtil(application)
    private val adManager = AdManager.getInstance(application)

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _editingPill = MutableStateFlow<Pill?>(null)
    val editingPill: StateFlow<Pill?> = _editingPill

    private val _imageUri = MutableStateFlow<String?>(null)
    val imageUri: StateFlow<String?> = _imageUri
    
    // 로딩 상태 (광고 로드 중 등)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // 중복 저장 방지 (thread-safe)
    private val _isSaving = MutableStateFlow(false)

    fun loadPill(pillId: String) {
        viewModelScope.launch {
            val pill = pillDao.getPillById(pillId)
            _editingPill.value = pill
            _imageUri.value = pill?.imageUri
        }
    }

    fun setImageUri(uri: String) {
        _imageUri.value = uri
    }

    /**
     * 갤러리/카메라에서 선택한 이미지를 앱 내부 저장소로 복사
     * (권한 유지 목적)
     */
    fun processSelectedImage(contentUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(contentUri)
                
                if (inputStream != null) {
                    val imagesDir = File(context.filesDir, "images")
                    if (!imagesDir.exists()) {
                        imagesDir.mkdirs()
                    }
                    val file = File(imagesDir, "pill_${UUID.randomUUID()}.jpg")
                    val outputStream = FileOutputStream(file)
                    
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    
                    val newUri = Uri.fromFile(file).toString()
                    Timber.d("Image copied to internal storage: $newUri")
                    _imageUri.value = newUri
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to copy image to internal storage")
            }
        }
    }

    /**
     * 약 이름 입력값 검증
     */
    private fun validatePillName(name: String): ValidationResult<String> {
        val trimmedName = name.trim()

        return when {
            trimmedName.isEmpty() -> {
                Timber.w("Pill name is empty")
                ValidationResult.Invalid("약 이름을 입력해주세요.")
            }
            trimmedName.length < 2 -> {
                Timber.w("Pill name too short: ${trimmedName.length}")
                ValidationResult.Invalid("약 이름은 최소 2자 이상이어야 합니다.")
            }
            trimmedName.length > 50 -> {
                Timber.w("Pill name too long: ${trimmedName.length}")
                ValidationResult.Invalid("약 이름은 50자를 초과할 수 없습니다.")
            }
            else -> ValidationResult.Valid(trimmedName)
        }
    }

    fun savePill(
        name: String,
        memo: String,
        type: String = "Capsule",
        dosage: String = "",
        quantity: Int = 30,
        // Alarm parameters (optional)
        isAlarmEnabled: Boolean = false,
        hour: Int = 8,
        minute: Int = 0,
        scheduleType: ScheduleType = ScheduleType.DAILY,
        scheduleConfig: ScheduleConfig = ScheduleConfig.Daily,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        alarmSoundUri: String? = null,
        onComplete: () -> Unit,
        onShowAd: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        // 중복 저장 방지
        if (_isSaving.value) {
            Timber.w("이미 저장 중입니다")
            return
        }

        // Pill Validation
        val nameValidation = validatePillName(name)
        if (nameValidation.isInvalid) {
             val errorMessage = nameValidation.errorMessages().joinToString("\n")
             Timber.e("Pill validation failed: $errorMessage")
             onError(errorMessage)
             return
        }

        // Alarm Validation (only if enabled)
        if (isAlarmEnabled) {
            val alarmErrors = mutableListOf<String>()
            if (hour !in 0..23) alarmErrors.add("시간은 0~23 사이여야 합니다.")
            if (minute !in 0..59) alarmErrors.add("분은 0~59 사이여야 합니다.")
            if (scheduleType == ScheduleType.WEEKLY && scheduleConfig is ScheduleConfig.Weekly) {
                 if (scheduleConfig.toDayOfWeekSet().isEmpty()) {
                     alarmErrors.add("최소 하나의 요일을 선택해야 합니다.")
                 }
            }
            // Add other specific validations if needed

            if (alarmErrors.isNotEmpty()) {
                val errorMessage = alarmErrors.joinToString("\n")
                Timber.e("Alarm validation failed: $errorMessage")
                onError(errorMessage)
                return
            }
        }

        val validatedName = nameValidation.getOrNull() ?: return
        _isSaving.value = true
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val existingPill = _editingPill.value
                val isEditMode = existingPill != null
                
                // 1. Save Pill
                val pillId = existingPill?.id ?: UUID.randomUUID().toString()
                
                val pill = if (isEditMode) {
                    existingPill!!.copy(
                        name = validatedName,
                        imageUri = _imageUri.value,
                        memo = memo.trim().takeIf { it.isNotBlank() },
                        type = type,
                        dosage = dosage
                    )
                } else {
                    Pill(
                        id = pillId,
                        name = validatedName,
                        imageUri = _imageUri.value,
                        memo = memo.trim().takeIf { it.isNotBlank() },
                        type = type,
                        dosage = dosage,
                        quantity = quantity
                    )
                }

                if (isEditMode) {
                    pillDao.updatePill(pill)
                } else {
                    pillDao.insertPill(pill)
                }
                
                // 2. Save Alarm (if enabled)
                // Note: We currently only support adding a NEW alarm during Pill creation.
                // Editing existing alarms is done via EditAlarmScreen.
                // However, if we want to support adding an alarm to an existing pill that doesn't have one here...
                // For now, let's assume this flow is primarily for NEW pills or simple edits.
                if (isAlarmEnabled) {
                     val now = LocalDateTime.now()
                     val scheduleConfigJson = json.encodeToString(scheduleConfig)
                     
                     val alarm = PillAlarm(
                        id = UUID.randomUUID().toString(),
                        pillId = pillId,
                        hour = hour,
                        minute = minute,
                        scheduleType = scheduleType,
                        scheduleConfig = scheduleConfigJson,
                        startDate = startDate,
                        endDate = endDate,
                        repeatDays = emptySet(), // Legacy
                        enabled = true,
                        alarmSoundUri = alarmSoundUri,
                        createdAt = now,
                        updatedAt = now
                    )
                    
                    alarmDao.insertAlarm(alarm)
                    alarmManager.scheduleAlarm(alarm)
                    Timber.d("Initial alarm saved for pill $pillId")
                }

                // 3. Ad Logic & Cleanup
                if (!isEditMode) {
                    _isLoading.value = false
                    onComplete()
                    return@launch
                }

                val shouldShow = adManager.incrementAndCheckAlarmRegistration()
                if (shouldShow) {
                    adManager.loadInterstitialAd(
                        onAdLoaded = {
                            _isLoading.value = false
                            onShowAd()
                        },
                        onAdFailed = {
                            _isLoading.value = false
                            onComplete()
                        }
                    )
                } else {
                    _isLoading.value = false
                    onComplete()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving pill and alarm")
                _isLoading.value = false
                onError("저장 중 오류가 발생했습니다: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deletePill(pill: Pill, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                // Cancel associated alarms
                val alarms = alarmDao.getAlarmsForPillOnce(pill.id)
                alarms.forEach { alarm ->
                    alarmManager.cancelAlarm(alarm)
                }

                // Delete from DB (alarms will be deleted via Cascade)
                pillDao.deletePill(pill)
                onComplete()
            } catch (e: Exception) {
                Timber.e(e, "Error deleting pill")
            }
        }
    }
}
