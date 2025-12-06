package com.uhstudio.pillreminder.util

/**
 * 입력 검증 결과를 나타내는 sealed class
 */
sealed class ValidationResult<out T> {
    /**
     * 검증 성공
     * @param value 검증된 값
     */
    data class Valid<T>(val value: T) : ValidationResult<T>()

    /**
     * 검증 실패
     * @param errors 에러 메시지 목록
     */
    data class Invalid(val errors: List<String>) : ValidationResult<Nothing>() {
        constructor(error: String) : this(listOf(error))
    }

    /**
     * 검증 결과가 유효한지 확인
     */
    val isValid: Boolean
        get() = this is Valid

    /**
     * 검증 결과가 무효한지 확인
     */
    val isInvalid: Boolean
        get() = this is Invalid

    /**
     * 유효한 경우 값을 가져오고, 무효한 경우 null 반환
     */
    fun getOrNull(): T? {
        return when (this) {
            is Valid -> value
            is Invalid -> null
        }
    }

    /**
     * 에러 메시지 목록 가져오기
     */
    fun errorMessages(): List<String> {
        return when (this) {
            is Valid -> emptyList()
            is Invalid -> errors
        }
    }
}

/**
 * 여러 개의 ValidationResult를 결합
 * 모든 결과가 Valid인 경우에만 Valid 반환
 */
fun <T> List<ValidationResult<T>>.combine(): ValidationResult<List<T>> {
    val errors = mutableListOf<String>()
    val values = mutableListOf<T>()

    for (result in this) {
        when (result) {
            is ValidationResult.Valid -> values.add(result.value)
            is ValidationResult.Invalid -> errors.addAll(result.errors)
        }
    }

    return if (errors.isEmpty()) {
        ValidationResult.Valid(values)
    } else {
        ValidationResult.Invalid(errors)
    }
}
