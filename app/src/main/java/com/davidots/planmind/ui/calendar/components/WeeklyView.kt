package com.davidots.planmind.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidots.planmind.data.local.ScheduleEntity
import com.davidots.planmind.ui.calendar.CalendarUiUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter


// 7일간의 주간 범위 세부 소일정을 리스트 타일 목록 형태로 가공 표시하는 스크롤 컴포저블
@Composable
fun WeeklyView(
    displayDate: LocalDate,
    schedulesByDate: Map<String, List<ScheduleEntity>>,
    onAddClick: (String) -> Unit,
    onScheduleClick: (String, Int) -> Unit
) {
    val startOfWeek = displayDate.minusDays((displayDate.dayOfWeek.value % 7).toLong())
    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        items(weekDays) { date ->
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dailySchedules = schedulesByDate[dateString] ?: emptyList()

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = date.dayOfMonth.toString().padStart(2, '0'), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = CalendarUiUtil.getDayOfWeekString(date.dayOfWeek.value),
                        fontSize = 14.sp,
                        color = CalendarUiUtil.getDayOfWeekColor(date.dayOfWeek.value),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                IconButton(onClick = { onAddClick(dateString) }) { Icon(Icons.Default.Add, "일정 추가") }
            }

            HorizontalDivider()

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                if (dailySchedules.isEmpty()) {
                    Text("일정이 없습니다.", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp))
                } else {
                    dailySchedules.forEach { schedule ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onScheduleClick(dateString, schedule.id) }.padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(CalendarUiUtil.getScheduleColor(schedule.type)))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = schedule.title,
                                    fontWeight = FontWeight.Bold,
                                    color = if (schedule.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (schedule.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                                Text(
                                    text = if (schedule.isAllDay) "하루 종일" else "${schedule.startTime} - ${schedule.endTime}",
                                    color = if (schedule.isCompleted) Color.LightGray else Color.Gray,
                                    fontSize = 12.sp,
                                    textDecoration = if (schedule.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}