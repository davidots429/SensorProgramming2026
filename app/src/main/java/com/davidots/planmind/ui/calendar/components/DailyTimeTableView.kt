package com.davidots.planmind.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidots.planmind.data.local.ScheduleEntity
import com.davidots.planmind.ui.calendar.CalendarUiUtil
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt


// 24시간 시간 흐름 세로축 위에 일정 블록들을 배치하는 일간 스케줄 컴포저블
@Composable
fun DailyTimeTableView(
    displayDate: LocalDate,
    schedulesByDate: Map<String, List<ScheduleEntity>>,
    onScheduleClick: (String, Int) -> Unit,
    onTimeUpdate: (Int, String, String) -> Unit
) {
    val dateString = displayDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val timedSchedules = (schedulesByDate[dateString] ?: emptyList()).filter { !it.isAllDay }

    var timeAdjustSchedule by remember { mutableStateOf<ScheduleEntity?>(null) }
    var tempStartMins by remember { mutableStateOf(0) }
    var tempEndMins by remember { mutableStateOf(0) }

    // 리컴포지션 유발 시 불필요한 행렬 연산 연쇄 반응을 막도록 의존 관계(remember) 캡슐화를 부여하여 수학 기둥 연산을 위임 호출
    val (totalColumns, scheduleColumnMap) = remember(timedSchedules) {
        CalendarUiUtil.calculateScheduleColumns(timedSchedules)
    }

    val hourHeightDp = 60.dp
    val totalHeightDp = hourHeightDp * 24

    Box(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.fillMaxWidth().height(totalHeightDp)) {
            for (hour in 0..24) {
                Row(modifier = Modifier.fillMaxWidth().height(hourHeightDp), verticalAlignment = Alignment.Top) {
                    Text(
                        text = "${hour.toString().padStart(2, '0')}:00",
                        modifier = Modifier.width(50.dp).offset(y = (-8).dp),
                        fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(totalHeightDp).padding(start = 58.dp)) {
            val density = LocalDensity.current
            val colWidth = maxWidth / totalColumns

            timedSchedules.forEach { schedule ->
                val colIndex = scheduleColumnMap[schedule.id] ?: 0
                val startMins = CalendarUiUtil.timeToMins(schedule.startTime)
                val durationMins = (CalendarUiUtil.timeToMins(schedule.endTime) - startMins).coerceAtLeast(15)

                var dragOffsetMins by remember { mutableStateOf(0) }

                Box(
                    modifier = Modifier
                        .offset(x = colWidth * colIndex, y = (startMins + dragOffsetMins).dp)
                        .width(colWidth)
                        .height(durationMins.dp)
                        .padding(end = 4.dp, bottom = 1.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CalendarUiUtil.getScheduleColor(schedule.type).copy(alpha = 0.9f))
                        .clickable { onScheduleClick(dateString, schedule.id) }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetMins += (dragAmount.y / density.density).roundToInt()
                                },
                                onDragEnd = {
                                    val newStartMins = startMins + dragOffsetMins
                                    val snappedStartMins = ((newStartMins + 2) / 5) * 5

                                    timeAdjustSchedule = schedule
                                    tempStartMins = snappedStartMins.coerceIn(0, 24 * 60 - durationMins)
                                    tempEndMins = tempStartMins + durationMins
                                    dragOffsetMins = 0
                                }
                            )
                        }
                        .padding(4.dp)
                ) {
                    Column {
                        Text(
                            text = schedule.title,
                            fontWeight = FontWeight.Bold,
                            color = if (schedule.isCompleted) Color.White.copy(alpha = 0.5f) else Color.White,
                            fontSize = 12.sp,
                            textDecoration = if (schedule.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        if (durationMins >= 30) {
                            Text(
                                text = "${schedule.startTime} - ${schedule.endTime}",
                                color = if (schedule.isCompleted) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                textDecoration = if (schedule.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }
    }

    if (timeAdjustSchedule != null) {
        val schedule = timeAdjustSchedule!!
        var newStartStr by remember { mutableStateOf(CalendarUiUtil.minsToTime(tempStartMins)) }
        var newEndStr by remember { mutableStateOf(CalendarUiUtil.minsToTime(tempEndMins)) }

        AlertDialog(
            onDismissRequest = { timeAdjustSchedule = null },
            title = { Text("시간 변경 확인") },
            text = {
                Column {
                    Text("원하는 시간으로 세밀하게 조절하세요.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = newStartStr, onValueChange = { newStartStr = it }, label = { Text("시작 시간") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = newEndStr, onValueChange = { newEndStr = it }, label = { Text("종료 시간") })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTimeUpdate(schedule.id, newStartStr, newEndStr)
                    timeAdjustSchedule = null
                }) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { timeAdjustSchedule = null }) { Text("취소") } }
        )
    }
}