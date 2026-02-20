package com.uhstudio.pillreminder.data.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "user_preferences"
        private const val KEY_USER_NAME = "user_name"
        private const val DEFAULT_USER_NAME = "사용자님"
    }

    /**
     * 사용자 이름을 가져옵니다.
     * @return 저장된 사용자 이름, 없으면 기본값 "사용자님" 반환
     */
    fun getUserName(): String {
        return prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
    }

    /**
     * 사용자 이름을 저장합니다.
     * @param name 저장할 사용자 이름
     */
    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    /**
     * 사용자 이름을 기본값으로 초기화합니다.
     */
    fun resetUserName() {
        prefs.edit().remove(KEY_USER_NAME).apply()
    }
}
