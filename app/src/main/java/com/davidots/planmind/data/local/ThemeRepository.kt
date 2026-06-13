package com.davidots.planmind.data.local

import android.content.Context
import android.content.SharedPreferences
import com.davidots.planmind.ui.settings.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeRepository(context: Context) {
    // 앱 설정 저장을 위한 SharedPreferences 파일 객체 생성
    private val prefs: SharedPreferences = context.getSharedPreferences("planmind_prefs", Context.MODE_PRIVATE)

    // 뷰모델 및 UI가 관찰할 가변 상태 흐름 엔진
    private val _theme = MutableStateFlow(getSavedTheme())
    val theme: StateFlow<AppTheme> = _theme

    // 새로운 테마 저장 및 스트림 업데이트 함수
    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("key_app_theme", theme.name).apply()
        _theme.value = theme
    }

    // 기존에 저장된 테마 불러오기 (기본값은 SYSTEM 모드)
    private fun getSavedTheme(): AppTheme {
        val themeStr = prefs.getString("key_app_theme", AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(themeStr ?: AppTheme.SYSTEM.name)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }
}