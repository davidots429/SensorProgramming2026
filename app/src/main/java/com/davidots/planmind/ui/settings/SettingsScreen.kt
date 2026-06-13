package com.davidots.planmind.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToSleep: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // 현재 뷰모델에 저장되어 흐르는 테마 상태 구독 수집
    val currentTheme by viewModel.theme.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // [1] 공통 상단 네비게이션 메뉴 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Menu, contentDescription = "메뉴", tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("홈") }, onClick = { showMenu = false; onNavigateToHome() })
                    DropdownMenuItem(text = { Text("캘린더") }, onClick = { showMenu = false; onNavigateToCalendar() })
                    DropdownMenuItem(text = { Text("집중 모드") }, onClick = { showMenu = false; onNavigateToFocus() })
                    DropdownMenuItem(text = { Text("수면/기상 관리") }, onClick = { showMenu = false; onNavigateToSleep() })
                    DropdownMenuItem(text = { Text("설정 (현재 화면)") }, onClick = { showMenu = false }, enabled = false)
                }
            }
            Text("설정", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.width(48.dp)) // 메뉴 아이콘과 타이틀 간의 시각적 균형을 위한 여백
        }

        // [2] 테마 설정 타이틀 섹션
        Text(
            text = "화면 테마 설정",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // [3] 테마 선택 카드 바디 리스트
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .selectableGroup() // 접근성(Accessibility) 가이드를 위한 라디오 그룹 명시
            ) {
                // AppTheme에 명시된 3가지 정의를 순회하며 행(Row) 생성
                AppTheme.values().forEach { themeOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .selectable(
                                selected = (themeOption == currentTheme),
                                onClick = { viewModel.updateTheme(themeOption) }, // 클릭 시 뷰모델 데이터 업데이트
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (themeOption == currentTheme),
                            onClick = null // Row 영역 전체에 클릭이 먹히도록 하기 위해 독립 onClick은 차단
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = themeOption.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (themeOption == currentTheme) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}