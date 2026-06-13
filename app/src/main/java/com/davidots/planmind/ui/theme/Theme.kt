package com.davidots.planmind.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 라이트 모드 기반의 쿨톤 컬러 스킴 정의
private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = SurfaceWhite,
    secondary = SecondaryTeal,
    background = BackgroundLight,
    surface = SurfaceWhite,
    onBackground = TextMain,
    onSurface = TextMain
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue, // 포인트 컬러는 유지
    onPrimary = Color.White,
    secondary = SecondaryTeal,
    background = Color(0xFF121212), // 어두운 배경색
    surface = Color(0xFF1E1E1E),    // 카드 등 표면의 어두운 색
    onBackground = Color.White,     // 어두운 배경 위에는 흰색 글씨
    onSurface = Color.White
)

@Composable
fun PlanMindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // darkTheme 값에 따라 쓸 물감 세트(ColorScheme)를 다르게 결정
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}