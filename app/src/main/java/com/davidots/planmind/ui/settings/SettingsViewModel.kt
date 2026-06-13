package com.davidots.planmind.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.davidots.planmind.data.local.ThemeRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    // 테마 리포지토리 초기화
    private val themeRepository = ThemeRepository(application)

    // UI 레이어가 수집(Collect)할 최종 읽기 전용 상태
    val theme: StateFlow<AppTheme> = themeRepository.theme

    // 사용자가 테마를 클릭했을 때 호출할 이벤트 처리 함수
    fun updateTheme(newTheme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setTheme(newTheme)
        }
    }
}