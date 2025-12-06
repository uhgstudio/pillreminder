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
import timber.log.Timber
import java.util.UUID

class AddPillViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PillReminderDatabase.getDatabase(application)
    private val pillDao = database.pillDao()
    private val adManager = AdManager.getInstance(application)

    private val _editingPill = MutableStateFlow<Pill?>(null)
    val editingPill: StateFlow<Pill?> = _editingPill

    private val _imageUri = MutableStateFlow<String?>(null)
    val imageUri: StateFlow<String?> = _imageUri

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
        onComplete: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        // 입력 검증
        val nameValidation = validatePillName(name)
        if (nameValidation.isInvalid) {
            val errorMessage = nameValidation.errorMessages().joinToString("\n")
            Timber.e("Pill validation failed: $errorMessage")
            onError(errorMessage)
            return
        }

        val validatedName = nameValidation.getOrNull() ?: return
        viewModelScope.launch {
            val existingPill = _editingPill.value
            val pill = if (existingPill != null) {
                // 편집 모드
                existingPill.copy(
                    name = validatedName,
                    imageUri = _imageUri.value,
                    memo = memo.trim().takeIf { it.isNotBlank() }
                )
            } else {
                // 새로 추가
                Pill(
                    id = UUID.randomUUID().toString(),
                    name = validatedName,
                    imageUri = _imageUri.value,
                    memo = memo.trim().takeIf { it.isNotBlank() }
                )
            }

            if (existingPill != null) {
                pillDao.updatePill(pill)
            } else {
                pillDao.insertPill(pill)
            }

            onComplete()
        }
    }
} 