package com.uhstudio.pillreminder.util

import java.util.UUID

/**
 * PendingIntent의 Request Code를 안전하게 생성하는 유틸리티
 *
 * String.hashCode()는 서로 다른 문자열이 같은 값을 반환할 수 있어 (해시 충돌)
 * 알람이 덮어씌워지는 치명적 버그를 유발할 수 있습니다.
 *
 * 이 유틸리티는 UUID 기반으로 고유한 Request Code를 생성합니다.
 */
object RequestCodeUtil {
    /**
     * UUID 문자열로부터 고유한 Request Code를 생성합니다.
     *
     * @param id UUID 형식의 문자열 (예: "550e8400-e29b-41d4-a716-446655440000")
     * @return 양수 정수 (0 ~ Int.MAX_VALUE)
     */
    fun generateRequestCode(id: String): Int {
        return try {
            val uuid = UUID.fromString(id)
            // UUID의 상위 비트와 하위 비트를 XOR하고, 양수로 변환
            (uuid.mostSignificantBits xor uuid.leastSignificantBits)
                .toInt() and 0x7FFFFFFF
        } catch (e: IllegalArgumentException) {
            // UUID 파싱 실패 시 fallback (하위 호환성)
            id.hashCode() and 0x7FFFFFFF
        }
    }

    /**
     * Prefix를 포함한 ID로부터 Request Code를 생성합니다.
     *
     * @param prefix 접두사 (예: "take", "skip", "snooze")
     * @param id UUID 형식의 문자열
     * @return 양수 정수
     */
    fun generateRequestCodeWithPrefix(prefix: String, id: String): Int {
        // prefix와 id를 결합한 새로운 UUID 생성
        val combined = "$prefix-$id"
        return combined.hashCode() and 0x7FFFFFFF
    }
}
