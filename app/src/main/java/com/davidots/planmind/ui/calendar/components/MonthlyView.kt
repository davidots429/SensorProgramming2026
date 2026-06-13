package com.davidots.planmind.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidots.planmind.data.local.ScheduleEntity
import com.davidots.planmind.ui.calendar.CalendarUiUtil
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// 월간 격자형 캘린더 정렬 모양을 빌드해내고 렌더링하는 UI 컴포넌트
@Composable
fun MonthlyView(
    displayMonth: YearMonth,
    schedulesByDate: Map<String, List<ScheduleEntity>>,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDayOfMonth = displayMonth.atDay(1)
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7
    val startDate = firstDayOfMonth.minusDays(startDayOfWeek.toLong())
    val calendarDays = (0 until 42).map { startDate.plusDays(it.toLong()) }
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxSize()) {
        val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            daysOfWeek.forEachIndexed { index, day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = if (index == 0) Color.Red else if (index == 6) Color.Blue else Color.Gray
                )
            }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxSize()) {
            items(calendarDays) { date ->
                val isCurrentMonth = date.monthValue == displayMonth.monthValue
                val isToday = date == today
                val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val dailySchedules = schedulesByDate[dateString] ?: emptyList()

                Box(
                    modifier = Modifier
                        .aspectRatio(0.6f)
                        .padding(2.dp)
                        .background(
                            color = if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .border(
                            width = if (isToday) 2.dp else 1.dp,
                            color = if (isToday) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .alpha(if (isCurrentMonth) 1f else 0.4f)
                        .clickable { onDayClick(date) }
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                        Text(
                            text = date.dayOfMonth.toString(),
                            modifier = Modifier.padding(start = 2.dp, top = 2.dp),
                            fontWeight = if (isToday || isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp
                        )

                        if (dailySchedules.isNotEmpty()) {
                            ScheduleBlockMini(dailySchedules.first())
                            if (dailySchedules.size > 1) {
                                Text(
                                    text = "+${dailySchedules.size - 1} 더보기",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleBlockMini(schedule: ScheduleEntity) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(CalendarUiUtil.getScheduleColor(schedule.type))
            .padding(horizontal = 2.dp, vertical = 1.dp)
    ) {
        Text(
            text = schedule.title,
            color = Color.White,
            fontSize = 8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}