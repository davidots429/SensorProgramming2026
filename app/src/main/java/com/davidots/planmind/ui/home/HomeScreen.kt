package com.davidots.planmind.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidots.planmind.ui.calendar.CalendarViewModel
import com.davidots.planmind.ui.calendar.ViewType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    calendarViewModel: CalendarViewModel, // 오늘의 일정을 불러오기 위해 캘린더 뷰모델 공유
    onNavigateToCalendar: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToDetail: (String, Int?) -> Unit
) {
    val today = remember { LocalDate.now() }
    val todayStr = remember { today.format(DateTimeFormatter.ISO_LOCAL_DATE) }

    // 캘린더 뷰모델에서 현재 달의 데이터를 관찰하여 오늘 날짜의 일정만 필터링
    val schedulesMap by calendarViewModel.schedulesByDate.collectAsState()
    val todaySchedules = schedulesMap[todayStr]?.sortedBy { it.startTime } ?: emptyList()

    LaunchedEffect(today) {
        calendarViewModel.updateDisplayedDate(today, ViewType.MONTHLY)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // [1] 헤더 환영 인사
        Text(
            text = "안녕하세요, 범수님! 👋",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )
        Text(
            text = "오늘도 알찬 하루를 계획해 볼까요?",
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // [2] 빠른 이동(네비게이션) 메뉴 카드들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MenuCard(
                modifier = Modifier.weight(1f),
                title = "캘린더",
                icon = Icons.Default.CalendarMonth,
                color = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToCalendar
            )
            MenuCard(
                modifier = Modifier.weight(1f),
                title = "집중 모드",
                icon = Icons.Default.Timer,
                color = Color(0xFFF59E0B), // 주황색
                onClick = onNavigateToFocus
            )
            MenuCard(
                modifier = Modifier.weight(1f),
                title = "수면 관리",
                icon = Icons.Default.Bedtime,
                color = Color(0xFF8B5CF6), // 보라색
                onClick = onNavigateToSleep
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // [3] 오늘의 일정 대시보드
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("오늘의 일정", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onNavigateToDetail(todayStr, null) }) {
                Icon(Icons.Default.Add, contentDescription = "일정 추가", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (todaySchedules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("오늘 등록된 일정이 없습니다.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(todaySchedules) { schedule ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetail(todayStr, schedule.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = schedule.title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (schedule.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (schedule.isAllDay) "하루 종일" else "${schedule.startTime} ~ ${schedule.endTime}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            if (schedule.isCompleted) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "완료", tint = Color(0xFF14B8A6))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuCard(modifier: Modifier = Modifier, title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        }
    }
}