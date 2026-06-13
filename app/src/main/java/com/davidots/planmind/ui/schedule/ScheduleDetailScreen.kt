package com.davidots.planmind.ui.schedule

import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.davidots.planmind.data.local.ScheduleEntity
import com.davidots.planmind.receiver.AlarmHelper
import com.davidots.planmind.receiver.GeofenceHelper
import com.davidots.planmind.ui.components.DetailTextField
import com.davidots.planmind.ui.components.InlineWheelTimePicker
import com.davidots.planmind.ui.components.SegmentedButtonUI
import com.davidots.planmind.ui.schedule.components.LocationSelectionDialog
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState


// 일정을 새로 추가하거나 기존 일정을 조회/수정하는 상세 화면
// UI 상태(사용자 입력값)를 임시로 들고 있다가, '저장' 버튼을 누르면 뷰모델(ViewModel)로 데이터를 전달하여 DB 처리를 위임
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDetailScreen(
    viewModel: ScheduleViewModel, // 단일 책임 원칙에 따라 가벼워진 일정 전용 뷰모델
    date: String,                 // 선택된 날짜 (추가 모드일 때 기본값으로 사용)
    scheduleId: Int? = null,      // null이면 '새 일정 추가', 값이 있으면 '기존 일정 수정/조회' 모드
    onDismiss: () -> Unit         // 뒤로 가기 또는 저장 완료 시 호출될 콜백
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 뷰모델에서 현재 포커싱된 일정 데이터를 관찰 (수정 모드일 때 데이터가 들어옴)
    val currentSchedule by viewModel.currentSchedule.collectAsState()

    // 화면 렌더링 시 scheduleId가 있으면 무조건 읽기 모드(false), 없으면 생성 모드(true)로 고정
    var isEditMode by remember(scheduleId) { mutableStateOf(scheduleId == null) }

    LaunchedEffect(currentSchedule) {
        if (scheduleId != null && currentSchedule != null) {
            isEditMode = false
        }
    }

    // [1] 화면 진입 시 초기화 로직
    LaunchedEffect(scheduleId) {
        if (scheduleId != null) {
            viewModel.loadSchedule(scheduleId) // 기존 데이터 로드
        } else {
            viewModel.clearCurrentSchedule()   // 이전 상태 찌꺼기 초기화
        }
    }

    // [2] 폼(Form) 입력 상태 변수 관리
    // 수정 모드일 경우 DB 데이터를 초기값으로 세팅하고, 아니면 빈 값으로 시작
    var title by remember(currentSchedule) { mutableStateOf(currentSchedule?.title ?: "") }
    var memo by remember(currentSchedule) { mutableStateOf(currentSchedule?.memo ?: "") }
    var type by remember(currentSchedule) { mutableStateOf(currentSchedule?.type ?: "일정") }
    var startTime by remember(currentSchedule) { mutableStateOf(currentSchedule?.startTime ?: "09:00") }
    var endTime by remember(currentSchedule) { mutableStateOf(currentSchedule?.endTime ?: "10:00") }
    var isAllDay by remember(currentSchedule) { mutableStateOf(currentSchedule?.isAllDay ?: false) }
    var locationName by remember(currentSchedule) { mutableStateOf(currentSchedule?.location ?: "") }
    var isCompleted by remember(currentSchedule) { mutableStateOf(currentSchedule?.isCompleted ?: false) }

    // 위치 권한 및 지도 연동 데이터 보관용
    var address by remember(currentSchedule) { mutableStateOf(currentSchedule?.address ?: "") }
    var latitude by remember(currentSchedule) { mutableStateOf(currentSchedule?.latitude) }
    var longitude by remember(currentSchedule) { mutableStateOf(currentSchedule?.longitude) }

    // 인라인 휠 타임 피커 노출 상태 제어 ("START" or "END" or null)
    var expandedTimePicker by remember { mutableStateOf<String?>(null) }

    // 삭제 확인 팝업 상태
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 수정 취소 확인 팝업 상태
    var showCancelEditConfirm by remember { mutableStateOf(false) }

    // 수정 모드일 때 안드로이드 시스템 '뒤로 가기' 버튼을 가로채서 팝업 띄우기
    BackHandler(enabled = isEditMode) {
        showCancelEditConfirm = true
    }

    // 지도 팝업 상태 및 권한 요청 런처
    var showMapDialog by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            showMapDialog = true
        } else {
            Toast.makeText(context, "지도 검색을 위해 위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    // [3] 유효성 검사 및 저장 처리 비즈니스 로직

    // 시간 유효성 검사
    val isTimeValid = remember(startTime, endTime, isAllDay) {
        if (isAllDay) true else startTime < endTime
    }

    val onSaveClick = {
        if (title.isBlank()) {
            Toast.makeText(context, "일정 제목을 입력해주세요.", Toast.LENGTH_SHORT).show()
        } else if (!isTimeValid) {
            // 시간이 올바르지 않으면 저장을 막고 경고 메시지 출력
            Toast.makeText(context, "종료 시간은 시작 시간보다 늦어야 합니다.", Toast.LENGTH_SHORT).show()
        } else {
            val finalSchedule = ScheduleEntity(
                id = currentSchedule?.id ?: 0,
                title = title,
                date = currentSchedule?.date ?: date,
                startTime = startTime,
                endTime = endTime,
                isAllDay = isAllDay,
                type = type,
                memo = memo,
                location = locationName,
                address = address,
                latitude = latitude,
                longitude = longitude,

                // 수정 시 기존에 가지고 있던 알람 및 반복 속성 증발 방지
                alarmTime = currentSchedule?.alarmTime ?: "시작 시간",
                repeatMode = currentSchedule?.repeatMode ?: "안함",
                isAcknowledged = currentSchedule?.isAcknowledged ?: false,

                // 체크박스에서 변경된 완료 상태를 뷰모델로 넘길 객체에 최종 매핑
                isCompleted = isCompleted
            )

            viewModel.saveSchedule(finalSchedule) { savedId ->
                if (type == "리마인더" || !isAllDay) {
                    AlarmHelper.scheduleAlarm(
                        context = context,
                        id = savedId,
                        title = finalSchedule.title,
                        date = finalSchedule.date,
                        startTime = finalSchedule.startTime,
                        alarmTimeStr = finalSchedule.alarmTime,
                        isAllDay = finalSchedule.isAllDay,
                        latitude = finalSchedule.latitude,
                        longitude = finalSchedule.longitude,
                        locationName = finalSchedule.location,
                    )
                }
                if (finalSchedule.latitude != null && finalSchedule.longitude != null) {
                    GeofenceHelper.addGeofence(context, finalSchedule.copy(id = savedId))
                }
                Toast.makeText(context, "일정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (scheduleId == null) "새 일정 추가" else "일정 상세", fontWeight = FontWeight.Bold)
                        if (scheduleId != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Checkbox(
                                checked = isCompleted,
                                onCheckedChange = {
                                    isCompleted = it
                                    val updated = currentSchedule!!.copy(isCompleted = it)
                                    viewModel.saveSchedule(updated) { }
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                            )
                            Text("완료", fontSize = 14.sp, color = if (isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                actions = {
                    if (scheduleId != null) {
                        IconButton(
                            onClick = {
                                // 수정 모드에서 아이콘을 또 누르면 바로 잠기지 않고 취소 확인 팝업 띄움
                                if (isEditMode) {
                                    showCancelEditConfirm = true
                                } else {
                                    isEditMode = true
                                }
                            },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = if (isEditMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "수정", tint = if (isEditMode) MaterialTheme.colorScheme.primary else Color.Gray)
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Red)
                        }
                    }
                }
            )
        },
        bottomBar = {
            // 하단 컨트롤 바 영역
            AnimatedVisibility(visible = isEditMode) {
                Surface(shadowElevation = 8.dp, color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCancelEditConfirm = true }) { Text("취소", color = Color.Gray) }
                        Button(onClick = onSaveClick) { Text("저장하기") }
                    }
                }
            }
        }
    ) { paddingValues ->
        // [4] 메인 입력 폼 UI (공통 컴포넌트 적극 활용)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ① 공통 세그먼트 버튼 컴포넌트 재사용
            SegmentedButtonUI(
                selectedType = type,
                onTypeSelected = { type = it },
                isEnabled = isEditMode
            )

            // ② 공통 텍스트 필드 컴포넌트 재사용
            DetailTextField(
                value = title,
                onValueChange = { title = it },
                label = "제목",
                isEditMode = isEditMode
            )

            // 시간 설정 영역
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("하루 종일", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = {
                                isAllDay = it
                                if (it) expandedTimePicker = null // 종일 모드로 변경 시 피커 닫기
                            },
                            enabled = isEditMode,
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary)
                        )
                    }

                    if (!isAllDay) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // 시작 시간 폼 (오버레이 터치 감지)
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = {},
                                    label = { Text("시작 시간") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true, // 키보드가 올라오는 것을 방지
                                    enabled = isEditMode, // 포커스 원천 차단
                                    isError = !isTimeValid, // 에러 발생 시 폼 테두리가 빨간색으로 변경
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = if (!isTimeValid) MaterialTheme.colorScheme.error else Color(0xFFE2E8F0),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                // 보기 모드에서는 클릭 감지 박스 자체를 비활성화하여 피커 호출을 차단
                                if (isEditMode) {
                                    Box(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { expandedTimePicker = if (expandedTimePicker == "START") null else "START" })
                                }
                            }
                            // 종료 시간 폼 (오버레이 터치 감지)
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = {},
                                    label = { Text("종료 시간") },
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true, // 키보드가 올라오는 것을 방지
                                    enabled = isEditMode, // 포커스 원천 차단
                                    isError = !isTimeValid, // 에러 발생 시 폼 테두리가 빨간색으로 변경
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = if (!isTimeValid) MaterialTheme.colorScheme.error else Color(0xFFE2E8F0),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                // 보기 모드에서는 클릭 감지 박스 자체를 비활성화하여 피커 호출을 차단
                                if (isEditMode) {
                                    Box(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { expandedTimePicker = if (expandedTimePicker == "END") null else "END" })
                                }
                            }
                        }

                        // 에러 텍스트 노출
                        AnimatedVisibility(visible = !isTimeValid) {
                            Text(
                                text = "종료 시간은 시작 시간보다 늦어야 합니다.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }

                        // 스르륵 열리는 인라인 휠 피커 영역
                        AnimatedVisibility(visible = expandedTimePicker != null && isEditMode) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                val isStart = expandedTimePicker == "START"
                                Text(
                                    text = if (isStart) "시작 시간 설정" else "종료 시간 설정",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                InlineWheelTimePicker(
                                    timeStr = if (isStart) startTime else endTime,
                                    onTimeChange = { newTime ->
                                        if (isStart) startTime = newTime else endTime = newTime
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 위치 지정 영역
            DetailTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = "장소 지정 (선택)",
                isEditMode = isEditMode,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (!isEditMode) return@IconButton // 보기 모드 시 무시

                            val hasFine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                            if (hasFine || hasCoarse) {
                                showMapDialog = true
                            } else {
                                locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        },
                        enabled = isEditMode
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = "지도 검색", tint = if (isEditMode) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                }
            )

            // 미니 지도 렌더링 뷰 (위치가 지정되었을 때만 노출)
            if (latitude != null && longitude != null) {
                val miniMapLatLng = LatLng(latitude!!, longitude!!)
                val miniMapCameraState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(miniMapLatLng, 15f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = miniMapCameraState,
                        uiSettings = MapUiSettings(
                            scrollGesturesEnabled = false, zoomGesturesEnabled = false,
                            tiltGesturesEnabled = false, rotationGesturesEnabled = false,
                            compassEnabled = false, myLocationButtonEnabled = false, mapToolbarEnabled = false
                        )
                    ) {
                        Marker(
                            state = MarkerState(position = miniMapLatLng),
                            title = locationName.ifBlank { "지정된 위치" }
                        )
                    }
                }
            }

            // 메모 입력 영역
            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text("메모") },
                enabled = isEditMode,
                modifier = Modifier.fillMaxWidth().height(150.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = if (!isTimeValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // [5] 팝업 다이얼로그 렌더링 영역

    // 지도 검색 팝업
    if (showMapDialog && isEditMode) {
        val initialLatLng = if (latitude != null && longitude != null) LatLng(latitude!!, longitude!!) else null
        LocationSelectionDialog(
            initialLatLng = initialLatLng,
            onDismiss = { showMapDialog = false },
            onLocationSelected = { latLng, placeName, placeAddress ->
                latitude = latLng.latitude
                longitude = latLng.longitude
                if (placeName.isNotBlank()) locationName = placeName
                address = placeAddress
                showMapDialog = false
            }
        )
    }

    // 취소 다이얼로그
    if (showCancelEditConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelEditConfirm = false },
            title = { Text(if (scheduleId == null) "작성 취소" else "수정 취소") },
            text = { Text(if (scheduleId == null) "작성 중인 일정을 취소하시겠습니까?" else "수정 중인 내용을 취소하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelEditConfirm = false
                        if (scheduleId == null) {
                            onDismiss() // 새 일정 생성 중이었다면 화면 자체를 닫음
                        } else {
                            // 기존 일정 수정 중이었다면 DB의 원본 데이터로 롤백하고 보기 모드로 전환
                            currentSchedule?.let {
                                title = it.title
                                memo = it.memo
                                type = it.type
                                startTime = it.startTime
                                endTime = it.endTime
                                isAllDay = it.isAllDay
                                locationName = it.location
                                address = it.address
                                latitude = it.latitude
                                longitude = it.longitude
                                isCompleted = it.isCompleted
                            }
                            expandedTimePicker = null
                            isEditMode = false
                        }
                    }
                ) { Text("네", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelEditConfirm = false }) { Text("아니오") }
            }
        )
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirm && currentSchedule != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("일정 삭제") },
            text = { Text("이 일정을 영구적으로 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSchedule(currentSchedule!!) {
                            // 백그라운드 예약 작업 해제
                            AlarmHelper.cancelAlarm(context, currentSchedule!!.id)
                            GeofenceHelper.removeGeofence(context, currentSchedule!!.id)
                            Toast.makeText(context, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                            showDeleteConfirm = false
                            onDismiss()
                        }
                    }
                ) { Text("삭제", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소") }
            }
        )
    }
}