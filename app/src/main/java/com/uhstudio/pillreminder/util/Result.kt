package com.uhstudio.pillreminder.util

/**
 * 비동기 작업의 결과를 나타내는 sealed class
 * Repository 계층에서 사용
 */
sealed class Result<out T> {
    /**
     * 성공 결과
     * @param data 성공 시 반환되는 데이터
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * 실패 결과
     * @param exception 발생한 예외
     * @param message 에러 메시지 (사용자에게 표시할 수 있는 메시지)
     */
    data class Error(
        val exception: Exception,
        val message: String = exception.message ?: "알 수 없는 오류가 발생했습니다."
    ) : Result<Nothing>()

    /**
     * 로딩 상태
     */
    object Loading : Result<Nothing>()

    /**
     * 결과가 성공인지 확인
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * 결과가 실패인지 확인
     */
    val isError: Boolean
        get() = this is Error

    /**
     * 결과가 로딩 중인지 확인
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * 성공한 경우 데이터를 가져오고, 실패한 경우 null 반환
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            is Error -> null
            is Loading -> null
        }
    }

    /**
     * 성공한 경우 데이터를 가져오고, 실패한 경우 기본값 반환
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T {
        return when (this) {
            is Success -> data
            else -> defaultValue
        }
    }

    /**
     * 성공한 경우 데이터를 가져오고, 실패한 경우 예외 발생
     */
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data
            is Error -> throw exception
            is Loading -> throw IllegalStateException("Result is still loading")
        }
    }

    /**
     * 성공한 경우 transform 함수를 적용한 새로운 Result 반환
     */
    fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> this
        }
    }

    /**
     * 성공한 경우 transform 함수를 적용하고, 실패한 경우 그대로 반환
     */
    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> {
        return when (this) {
            is Success -> transform(data)
            is Error -> this
            is Loading -> this
        }
    }

    /**
     * 성공/실패에 따라 다른 작업 수행
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    inline fun onError(action: (Exception, String) -> Unit): Result<T> {
        if (this is Error) {
            action(exception, message)
        }
        return this
    }

    inline fun onLoading(action: () -> Unit): Result<T> {
        if (this is Loading) {
            action()
        }
        return this
    }
}

/**
 * 여러 개의 Result를 결합
 * 모든 결과가 Success인 경우에만 Success 반환
 */
fun <T> List<Result<T>>.combine(): Result<List<T>> {
    val data = mutableListOf<T>()

    for (result in this) {
        when (result) {
            is Result.Success -> data.add(result.data)
            is Result.Error -> return result
            is Result.Loading -> return Result.Loading
        }
    }

    return Result.Success(data)
}

/**
 * suspend 함수를 Result로 감싸서 실행
 */
suspend fun <T> runCatching(block: suspend () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e)
    }
}
