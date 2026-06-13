package com.davidots.planmind.ui.calendar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.davidots.planmind.data.local.ScheduleEntity
import com.davidots.planmind.ui.calendar.CalendarUiUtil
import java.time.LocalDate


// 월간 달력 타일 터치 시 활성화되는 해당 일자 등록 일정 다중 검토 및 일괄 다중 삭제 모달 컴포저블
@Composable
fun DailySchedulePopup(
    date: LocalDate,
    schedules: List<ScheduleEntity>,
    onDismiss: () -> Unit,
    onAddClick: (LocalDate) -> Unit,
    onScheduleClick: (Int) -> Unit,
    onDeleteConfirm: (List<ScheduleEntity>) -> Unit
) {
    var isDeleteMode by remember { mutableStateOf(false) }
    var selectedForDelete by remember { mutableStateOf(setOf<ScheduleEntity>()) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("일정 삭제") },
            text = { Text("선택한 ${selectedForDelete.size}개의 일정을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConfirm(selectedForDelete.toList())
                    showConfirmDialog = false
                    isDeleteMode = false
                    selectedForDelete = emptySet()
                }) { Text("확인", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("취소") } }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = date.dayOfMonth.toString().padStart(2, '0'), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = CalendarUiUtil.getDayOfWeekString(date.dayOfWeek.value),
                            fontSize = 14.sp,
                            color = CalendarUiUtil.getDayOfWeekColor(date.dayOfWeek.value),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (isDeleteMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selectedForDelete = if (selectedForDelete.size == schedules.size) emptySet() else schedules.toSet() }) { Text("모두 선택") }
                            TextButton(onClick = { isDeleteMode = false }) { Text("취소") }
                            TextButton(onClick = { if (selectedForDelete.isNotEmpty()) showConfirmDialog = true }, enabled = selectedForDelete.isNotEmpty()) { Text("확인", color = Color.Red) }
                        }
                    } else {
                        Row {
                            IconButton(onClick = { onAddClick(date) }) { Icon(Icons.Default.Add, "추가") }
                            IconButton(onClick = { isDeleteMode = true }) { Icon(Icons.Default.Delete, "삭제 모드") }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(schedules) { schedule ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable(!isDeleteMode) { onScheduleClick(schedule.id) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDeleteMode) {
                                Checkbox(
                                    checked = selectedForDelete.contains(schedule),
                                    onCheckedChange = { checked ->
                                        val newSet = selectedForDelete.toMutableSet()
                                        if (checked) newSet.add(schedule) else newSet.remove(schedule)
                                        selectedForDelete = newSet
                                    }
                                )
                            }
                            Column(modifier = Modifier.padding(start = if (isDeleteMode) 0.dp else 8.dp)) {
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