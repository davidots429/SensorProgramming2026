package com.davidots.planmind.ui.settings

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current

    // 현재 뷰모델에 저장되어 흐르는 테마 상태 구독 수집
    val currentTheme by viewModel.theme.collectAsState()

    // 삭제 확인 팝업을 띄울지 말지 결정하는 상태 변수
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "메뉴",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("홈") },
                        onClick = { showMenu = false; onNavigateToHome() })
                    DropdownMenuItem(
                        text = { Text("캘린더") },
                        onClick = { showMenu = false; onNavigateToCalendar() })
                    DropdownMenuItem(
                        text = { Text("집중 모드") },
                        onClick = { showMenu = false; onNavigateToFocus() })
                    DropdownMenuItem(
                        text = { Text("수면/기상 관리") },
                        onClick = { showMenu = false; onNavigateToSleep() })
                    DropdownMenuItem(
                        text = { Text("설정 (현재 화면)") },
                        onClick = { showMenu = false },
                        enabled = false
                    )
                }
            }
            Text(
                "설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(48.dp))
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
                    .selectableGroup()
            ) {
                // AppTheme에 명시된 3가지 정의를 순회하며 행(Row) 생성
                AppTheme.values().forEach { themeOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .selectable(
                                selected = (themeOption == currentTheme),
                                onClick = { viewModel.updateTheme(themeOption) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (themeOption == currentTheme),
                            onClick = null
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

        Spacer(modifier = Modifier.height(32.dp))

        // [4] 데이터 관리 섹션
        Text(
            text = "데이터 관리",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "수면 기록 초기화",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "지금까지 기록된 모든 수면 데이터를 삭제합니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { showDeleteConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("삭제")
                }
            }
        }
    }

    // [5] 삭제 재확인 팝업
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text("수면 기록 초기화", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("정말 모든 수면 기록을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없습니다.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllSleepRecords()
                        showDeleteConfirmDialog = false
                        Toast.makeText(context, "수면 기록이 모두 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("삭제", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("취소", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}