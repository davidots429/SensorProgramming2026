package com.davidots.planmind.ui.sleep

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.davidots.planmind.receiver.AlarmHelper
import com.davidots.planmind.ui.components.InlineWheelTimePicker
import kotlinx.coroutines.delay

@Composable
fun SleepScreen(
    viewModel: SleepViewModel, // 의존성 주입된 수면 전용 뷰모델
    initialAlarmState: SleepAlarmState = SleepAlarmState.IDLE, // MainActivity의 인텐트로부터 전달받는 초기 상태
    onNavigateToHome: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current

    // [1] ViewModel 상태 관찰
    val alarmState by viewModel.alarmState.collectAsState()
    val currentLux by viewModel.currentLux.collectAsState()
    val stepCount by viewModel.stepCount.collectAsState()
    val recentRecords by viewModel.recentSleepRecords.collectAsState()

    // [2] 설정 시간 로컬 보관 (SharedPreferences 활용)
    val sharedPref = context.getSharedPreferences("PlanMindPrefs", Context.MODE_PRIVATE)
    var sleepTimeStr by remember { mutableStateOf(sharedPref.getString("sleepTime", "23:30") ?: "23:30") }
    var wakeTimeStr by remember { mutableStateOf(sharedPref.getString("wakeTime", "07:00") ?: "07:00") }

    var showMenu by remember { mutableStateOf(false) }

    // 휠 피커 다이얼로그 제어용 상태
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var isEditingSleepTime by remember { mutableStateOf(true) }
    var tempTimeStr by remember { mutableStateOf("") }

    // 화면 진입 시 보류 중인 미션(알림을 놓친 미션)이 있는지 검사하여 강제 소환
    LaunchedEffect(Unit) {
        val pendingMission = sharedPref.getString("PENDING_MISSION", null)
        if (pendingMission == "SLEEP" && alarmState == SleepAlarmState.IDLE) {
            viewModel.setAlarmState(SleepAlarmState.SLEEP_WAITING)
        } else if (pendingMission == "WAKE" && alarmState == SleepAlarmState.IDLE) {
            viewModel.setAlarmState(SleepAlarmState.WAKE_RINGING)
        }
    }

    // 미션을 완료하거나 강제 종료하여 IDLE 상태가 되면 보류 표식을 지워버림
    LaunchedEffect(alarmState) {
        if (alarmState == SleepAlarmState.IDLE) {
            sharedPref.edit().remove("PENDING_MISSION").apply()
        }
    }

    // 앱 화면을 보고 있을 때 시간이 되면 즉시 뷰를 전환하기 위한 실시간 타이머
    var currentTimeStr by remember { mutableStateOf("") }
    var lastTriggeredTime by remember { mutableStateOf("") } // 같은 분 내에 강제 종료 시 재실행 방지

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = java.util.Calendar.getInstance()
            currentTimeStr = String.format("%02d:%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))
            delay(1000L) // 1초마다 갱신하여 분 단위가 바뀌는 찰나를 정확히 캐치합니다.
        }
    }

    // 현재 시각이 설정한 취침/기상 시각과 일치하면 즉시 상태 전환
    LaunchedEffect(currentTimeStr, sleepTimeStr, wakeTimeStr) {
        if (alarmState == SleepAlarmState.IDLE && currentTimeStr != lastTriggeredTime) {
            if (currentTimeStr == sleepTimeStr) {
                lastTriggeredTime = currentTimeStr
                viewModel.setAlarmState(SleepAlarmState.SLEEP_WAITING)
            } else if (currentTimeStr == wakeTimeStr) {
                lastTriggeredTime = currentTimeStr
                viewModel.setAlarmState(SleepAlarmState.WAKE_RINGING)
            }
        }
    }

    // 외부 진입 시 초기 상태 세팅
    LaunchedEffect(initialAlarmState) {
        if (initialAlarmState != SleepAlarmState.IDLE) {
            viewModel.setAlarmState(initialAlarmState)
        }
    }

    // [3] 하드웨어 장치(소리, 진동, 센서) 생명주기 관리 구역
    DisposableEffect(alarmState) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        var mediaPlayer: MediaPlayer? = null
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // 센서 값을 읽어 뷰모델로 전송 (판단은 뷰모델이 함)
                if (event.sensor.type == Sensor.TYPE_LIGHT) {
                    viewModel.updateLux(event.values[0])
                } else if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                    viewModel.updateStepCount()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        when (alarmState) {
            SleepAlarmState.SLEEP_WAITING -> {
                // 조도 센서 가동
                val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
                sensorManager.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
            SleepAlarmState.WAKE_RINGING -> {
                // 걸음 수 센서 가동 및 알람 소리/진동 재생
                val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
                sensorManager.registerListener(sensorListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST)

                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(context, uri)
                        setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                        isLooping = true
                        prepare()
                        start()
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 0))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 1000, 1000), 0)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            SleepAlarmState.IDLE -> { /* 센서 해제 대기 */ }
        }

        // 상태가 변경되거나 화면이 파괴될 때 모든 하드웨어 리소스 강제 해제 (메모리 누수 방지)
        onDispose {
            sensorManager.unregisterListener(sensorListener)
            mediaPlayer?.stop()
            mediaPlayer?.release()
            vibrator.cancel()
        }
    }

    // 알람 진행 중에는 뒤로 가기 버튼을 무력화하여 사용자가 미션을 완수하도록 강제합니다.
    BackHandler(enabled = alarmState != SleepAlarmState.IDLE) { /* 무시 */ }

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // [4] 상단 헤더 메뉴
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { showMenu = true }, enabled = alarmState == SleepAlarmState.IDLE) {
                    Icon(Icons.Default.Menu, "메뉴", tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("홈") }, onClick = { showMenu = false; onNavigateToHome() })
                    DropdownMenuItem(text = { Text("캘린더") }, onClick = { showMenu = false; onNavigateToCalendar() } )
                    DropdownMenuItem(text = { Text("집중 모드") }, onClick = { showMenu = false; onNavigateToFocus() })
                    DropdownMenuItem(text = { Text("수면/기상 관리 (현재 화면)") }, onClick = { showMenu = false }, enabled = false)
                    DropdownMenuItem(text = { Text("설정") }, onClick = { showMenu = false; onNavigateToSettings() })
                }
            }
            Text("수면 및 기상", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.width(48.dp)) // 균형 맞추기용
        }

        // [5] 상태별 화면 렌더링
        when (alarmState) {
            SleepAlarmState.IDLE -> {
                // 평상시: 설정 및 대시보드 화면
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("목표 취침 시간", color = Color.Gray, fontSize = 14.sp)
                                Text(sleepTimeStr, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = {
                                isEditingSleepTime = true
                                tempTimeStr = sleepTimeStr
                                showTimePickerDialog = true
                            }) { Text("변경") }
                        }
                        Divider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("목표 기상 시간", color = Color.Gray, fontSize = 14.sp)
                                Text(wakeTimeStr, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(onClick = {
                                isEditingSleepTime = false
                                tempTimeStr = wakeTimeStr
                                showTimePickerDialog = true
                            }) { Text("변경") }
                        }
                    }
                }

                // SleepTimeUtil을 활용한 적정 수면 체크 안내 카드 렌더링[cite: 5]
                val durationMins = SleepTimeUtil.calculateDurationMins(sleepTimeStr, wakeTimeStr)
                val sleepHours = durationMins / 60
                val sleepMinsRemaining = durationMins % 60
                val isEnoughSleep = SleepTimeUtil.isEnoughSleep(durationMins)
                val isGoodSleepTime = SleepTimeUtil.isGoodSleepTime(sleepTimeStr)

                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("총 수면 예정 시간: ${sleepHours}시간 ${sleepMinsRemaining}분", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isEnoughSleep) "✅" else "⚠️", modifier = Modifier.padding(end = 8.dp))
                            Text(text = if (isEnoughSleep) "최소 수면 시간(6시간)을 충족합니다." else "수면 시간이 너무 짧습니다.", color = if (isEnoughSleep) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isGoodSleepTime) "✅" else "⚠️", modifier = Modifier.padding(end = 8.dp))
                            Text(text = if (isGoodSleepTime) "Deep Sleep이 가능한 적절한 취침 시간입니다." else "새벽 2시 이전 취침을 권장합니다.", color = if (isGoodSleepTime) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        }
                    }
                }

                Text("최근 수면 기록", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(bottom = 8.dp))

                if (recentRecords.isEmpty()) {
                    Text("아직 기록이 없습니다. 수면 미션을 완료해 보세요!", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(top = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(recentRecords) { record ->
                            val hrs = record.sleepDurationMins / 60
                            val mins = record.sleepDurationMins % 60
                            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(record.date, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("${record.actualSleepTime} ~ ${record.actualWakeTime}", color = Color.Gray, fontSize = 12.sp)
                                    }
                                    Text("${hrs}시간 ${mins}분", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                }
            }

            SleepAlarmState.SLEEP_WAITING -> {
                // 취침 대기 중 (어두운 환경 유도 화면)
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("숙면을 취할 시간입니다 🌙", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("수면을 위해 방의 불을 꺼주세요.\\n어두워지면 자동으로 취침 기록이 시작됩니다.", textAlign = TextAlign.Center, color = Color.Gray)
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("현재 방 밝기: ${currentLux.toInt()} lux", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("(5 lux 이하 시 자동으로 수면 돌입)", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(48.dp))

                    //TextButton(onClick = { viewModel.forceStopAlarm() }) { Text("[디버그] 강제 종료") }
                }
            }

            SleepAlarmState.WAKE_RINGING -> {
                // 기상 알람 진행 중 (걷기 미션 화면)
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("기상 시간입니다 ☀️", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("알람을 끄려면 휴대폰을 들고\\n10걸음 이상 걸어주세요.", textAlign = TextAlign.Center, color = Color.Gray)
                    Spacer(modifier = Modifier.height(32.dp))
                    Text("현재 걸음 수: $stepCount / 10", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(48.dp))

                    //TextButton(onClick = { viewModel.forceStopAlarm() }) { Text("[디버그] 강제 종료", color = Color.Red) }
                }
            }
        }
    }

    // [신규 렌더링] 커스텀 휠 피커 다이얼로그
    if (showTimePickerDialog) {
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = { Text(if (isEditingSleepTime) "취침 시간 설정" else "기상 시간 설정", fontWeight = FontWeight.Bold) },
            text = {
                // 공통 컴포넌트로 승격된 InlineWheelTimePicker 호출
                InlineWheelTimePicker(
                    timeStr = tempTimeStr,
                    onTimeChange = { tempTimeStr = it }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (isEditingSleepTime) {
                        sleepTimeStr = tempTimeStr
                        sharedPref.edit().putString("sleepTime", tempTimeStr).apply()
                        AlarmHelper.scheduleDailyAlarm(context, 2001, tempTimeStr, "SLEEP", "취침 알람")
                    } else {
                        wakeTimeStr = tempTimeStr
                        sharedPref.edit().putString("wakeTime", tempTimeStr).apply()
                        AlarmHelper.scheduleDailyAlarm(context, 2002, tempTimeStr, "WAKE", "기상 알람")
                    }
                    showTimePickerDialog = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) { Text("취소", color = Color.Gray) }
            }
        )
    }
}