package com.uhstudio.pillreminder.data.repository

import com.uhstudio.pillreminder.data.dao.PillDao
import com.uhstudio.pillreminder.data.model.Pill
import com.uhstudio.pillreminder.util.Result
import com.uhstudio.pillreminder.util.ValidationResult
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * 약 정보 관리를 위한 Repository
 * 비즈니스 로직과 데이터 접근 로직 분리
 */
class PillRepository(
    private val pillDao: PillDao
) {
    /**
     * 모든 약 목록을 Flow로 반환
     */
    fun getAllPills(): Flow<List<Pill>> {
        return pillDao.getAllPills()
    }

    /**
     * 모든 약 목록을 한 번만 조회
     */
    suspend fun getAllPillsOnce(): Result<List<Pill>> {
        return try {
            val pills = pillDao.getAllPillsOnce()
            Timber.d("getAllPillsOnce: ${pills.size} pills found")
            Result.Success(pills)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all pills")
            Result.Error(e, "약 목록을 불러오는데 실패했습니다.")
        }
    }

    /**
     * ID로 약 정보 조회
     */
    suspend fun getPillById(pillId: String): Result<Pill> {
        return try {
            val pill = pillDao.getPillById(pillId)
            if (pill != null) {
                Timber.d("getPillById: pill found - ${pill.name}")
                Result.Success(pill)
            } else {
                Timber.w("getPillById: pill not found - $pillId")
                Result.Error(
                    Exception("Pill not found"),
                    "약 정보를 찾을 수 없습니다."
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get pill by id: $pillId")
            Result.Error(e, "약 정보를 불러오는데 실패했습니다.")
        }
    }

    /**
     * 약 이름 검증
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

    /**
     * 약 정보 추가
     */
    suspend fun addPill(pill: Pill): Result<Unit> {
        // 입력 검증
        val nameValidation = validatePillName(pill.name)
        if (nameValidation.isInvalid) {
            val errorMessage = nameValidation.errorMessages().joinToString("\n")
            Timber.e("Pill validation failed: $errorMessage")
            return Result.Error(
                Exception("Validation failed"),
                errorMessage
            )
        }

        return try {
            val validatedPill = pill.copy(
                name = nameValidation.getOrNull() ?: pill.name,
                memo = pill.memo?.trim()?.takeIf { it.isNotBlank() }
            )
            pillDao.insertPill(validatedPill)
            Timber.d("addPill: pill added - ${validatedPill.name}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add pill: ${pill.name}")
            Result.Error(e, "약 정보를 저장하는데 실패했습니다.")
        }
    }

    /**
     * 약 정보 수정
     */
    suspend fun updatePill(pill: Pill): Result<Unit> {
        // 입력 검증
        val nameValidation = validatePillName(pill.name)
        if (nameValidation.isInvalid) {
            val errorMessage = nameValidation.errorMessages().joinToString("\n")
            Timber.e("Pill validation failed: $errorMessage")
            return Result.Error(
                Exception("Validation failed"),
                errorMessage
            )
        }

        return try {
            val validatedPill = pill.copy(
                name = nameValidation.getOrNull() ?: pill.name,
                memo = pill.memo?.trim()?.takeIf { it.isNotBlank() }
            )
            pillDao.updatePill(validatedPill)
            Timber.d("updatePill: pill updated - ${validatedPill.name}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update pill: ${pill.name}")
            Result.Error(e, "약 정보를 수정하는데 실패했습니다.")
        }
    }

    /**
     * 약 정보 삭제
     */
    suspend fun deletePill(pill: Pill): Result<Unit> {
        return try {
            pillDao.deletePill(pill)
            Timber.d("deletePill: pill deleted - ${pill.name}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete pill: ${pill.name}")
            Result.Error(e, "약 정보를 삭제하는데 실패했습니다.")
        }
    }
}
