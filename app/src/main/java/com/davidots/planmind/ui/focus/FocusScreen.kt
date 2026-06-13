package com.davidots.planmind.ui.focus

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.MediaRecorder
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.davidots.planmind.ui.components.NumberWheelPicker
import kotlin.math.log10
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 타이머 종료 알람 헬퍼 함수 (안드로이드 시스템 서비스 접근용)
private fun playTimerAlarm(context: Context) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(1000)
        }
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri).play()
    } catch (e: Exception) { e.printStackTrace() }
}

@Composable
fun FocusScreen(
    viewModel: FocusViewModel = viewModel(), // 분리한 뷰모델 주입
    onNavigateToCalendar: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current

    // [1] 뷰모델의 상태(StateFlow) 관찰
    val timerState by viewModel.timerState.collectAsState()
    val focusMode by viewModel.focusMode.collectAsState()
    val timeLeftSeconds by viewModel.timeLeftSeconds.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val currentLux by viewModel.currentLux.collectAsState()
    val currentDb by viewModel.currentDb.collectAsState()
    val isBadEnv = viewModel.isBadEnvironment

    // 뷰모델 상태(MutableStateFlow) 바인딩용 변수
    val pomoDurationMins by viewModel.pomoDurationMins.collectAsState()
    val pomoTotalSessions by viewModel.pomoTotalSessions.collectAsState()
    val normalHour by viewModel.normalHour.collectAsState()
    val normalMinute by viewModel.normalMinute.collectAsState()
    val normalSecond by viewModel.normalSecond.collectAsState()

    // UI 전용 상태 (팝업, 메뉴 제어)
    var showMenu by remember { mutableStateOf(false) }
    var pendingExitAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showEnvWarningPopup by remember { mutableStateOf(false) }
    var showSilentModePopup by remember { mutableStateOf(false) }

    // [2] 뷰모델에서 방출되는 알람 이벤트 관찰
    LaunchedEffect(Unit) {
        viewModel.alarmEvent.collect {
            playTimerAlarm(context)
        }
    }

    // [3] 하드웨어 센서 생명주기 관리 (뷰모델로 값 전달)
    DisposableEffect(timerState) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var mediaRecorder: MediaRecorder? = null
        var isMeasuring = false

        // 환경(조도) 및 기기 엎음(가속도) 측정 리스너
        val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_LIGHT) {
                    viewModel.updateEnvironment(lux = event.values[0], db = currentDb)
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val zValue = event.values[2]
                    viewModel.updateFaceDownState(zValue < -8.0f) // -8.0 이하면 엎음 판정
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // 항상 가속도 센서는 켜둠 (엎음 감지용)
        sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        // IDLE 상태일 때만 환경 센서(조도, 데시벨) 가동 (배터리 최적화)
        if (timerState == TimerState.IDLE) {
            sensorManager.registerListener(sensorListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
            try {
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
                mediaRecorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(context.cacheDir.absolutePath + "/dummy_audio.3gp")
                    prepare()
                    start()
                }
                isMeasuring = true
            } catch (e: Exception) { e.printStackTrace() }
        }

        val measureJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            while (isMeasuring && mediaRecorder != null) {
                try {
                    val amplitude = mediaRecorder.maxAmplitude
                    if (amplitude > 0) {
                        viewModel.updateEnvironment(lux = currentLux, db = 20 * log10(amplitude.toDouble()).toFloat())
                    }
                    delay(500)
                } catch (e: Exception) { break }
            }
        }

        onDispose {
            isMeasuring = false
            measureJob.cancel()
            sensorManager.unregisterListener(sensorListener)
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) {}
        }
    }

    // HH:mm:ss 포맷팅
    val hrs = timeLeftSeconds / 3600
    val mins = (timeLeftSeconds % 3600) / 60
    val secs = timeLeftSeconds % 60
    val timeString = String.format("%02d : %02d : %02d", hrs, mins, secs)

    // 시작 조건 확인 로직 (무음/진동 모드 확인)
    val checkMuteAndStart = {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            showSilentModePopup = true
        } else {
            viewModel.startTimer()
        }
    }

    val onStartClicked = {
        if (focusMode == FocusMode.POMODORO && isBadEnv) {
            showEnvWarningPopup = true
        } else {
            checkMuteAndStart()
        }
    }

    // 뒤로 가기 제어 (타이머 작동 중이면 팝업)
    val navigateWithConfirm: (() -> Unit) -> Unit = { targetAction ->
        if (timerState == TimerState.IDLE || timerState == TimerState.COMPLETED) targetAction()
        else pendingExitAction = targetAction
    }
    BackHandler(onBack = { navigateWithConfirm(onNavigateToHome) })

    // UI 레이아웃 렌더링
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // --- 1. 상단 헤더 ---
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.Menu, "메뉴", tint = MaterialTheme.colorScheme.primary) }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("홈") }, onClick = { showMenu = false; onNavigateToHome() })
                    DropdownMenuItem(text = { Text("캘린더") }, onClick = { showMenu = false; onNavigateToCalendar() })
                    DropdownMenuItem(text = { Text("집중 모드 (현재 화면)") }, onClick = { showMenu = false; }, enabled = false)
                    DropdownMenuItem(text = { Text("수면/기상 관리") }, onClick = { showMenu = false; onNavigateToSleep() })
                    DropdownMenuItem(text = { Text("설정") }, onClick = { showMenu = false; onNavigateToSettings() })
                }
            }
            Text("집중 모드", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Button(
                onClick = {
                    if (timerState == TimerState.IDLE || timerState == TimerState.COMPLETED) viewModel.toggleMode()
                    else pendingExitAction = { viewModel.forceStopTimer(); viewModel.toggleMode() }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) { Text(focusMode.title, fontWeight = FontWeight.Bold) }
        }

        // --- 2. 메인 콘텐츠 영역 ---
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when (timerState) {
                TimerState.IDLE -> {
                    if (focusMode == FocusMode.POMODORO) {
                        // [포모도로 설정 뷰]
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isBadEnv) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isBadEnv) "현재 집중하기 어려운 환경입니다." else "집중하기 좋은 환경입니다.",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBadEnv) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    "소음: ${currentDb.toInt()}dB / 밝기: ${currentLux.toInt()}lx",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Text("집중할 시간: ${pomoDurationMins.toInt()}분", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                        Slider(
                            value = pomoDurationMins,
                            onValueChange = { viewModel.pomoDurationMins.value = it },
                            valueRange = 10f..25f,
                            steps = 14,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("반복 횟수 (1사이클): $pomoTotalSessions 회", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
                        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            (1..4).forEach { count ->
                                FilterChip(
                                    selected = pomoTotalSessions == count,
                                    onClick = { viewModel.pomoTotalSessions.value = count },
                                    label = { Text("${count}회") })
                            }
                        }
                    } else {
                        // [일반 타이머 설정 뷰] - 공통 컴포넌트인 NumberWheelPicker 사용
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("시간", color = Color.Gray, fontSize = 14.sp); Spacer(modifier = Modifier.height(12.dp))
                                    NumberWheelPicker(range = 0..9, value = normalHour, onValueChange = { viewModel.normalHour.value = it }, modifier = Modifier.fillMaxSize(), isInfinite = false)
                                }
                                Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.LightGray, modifier = Modifier.padding(top = 30.dp))
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("분", color = Color.Gray, fontSize = 14.sp); Spacer(modifier = Modifier.height(12.dp))
                                    NumberWheelPicker(range = 0..59, value = normalMinute, onValueChange = { viewModel.normalMinute.value = it }, modifier = Modifier.fillMaxSize(), isInfinite = true)
                                }
                                Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.LightGray, modifier = Modifier.padding(top = 30.dp))
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("초", color = Color.Gray, fontSize = 14.sp); Spacer(modifier = Modifier.height(12.dp))
                                    NumberWheelPicker(range = 0..59, value = normalSecond, onValueChange = { viewModel.normalSecond.value = it }, modifier = Modifier.fillMaxSize(), isInfinite = true)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(onClick = { viewModel.normalHour.value = 0; viewModel.normalMinute.value = 10; viewModel.normalSecond.value = 0 }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4453))) { Text("10분") }
                            Button(onClick = { viewModel.normalHour.value = 0; viewModel.normalMinute.value = 30; viewModel.normalSecond.value = 0 }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4453))) { Text("30분") }
                            Button(onClick = { viewModel.normalHour.value = 1; viewModel.normalMinute.value = 0; viewModel.normalSecond.value = 0 }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4453))) { Text("1시간") }
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(onClick = onStartClicked, modifier = Modifier.fillMaxWidth(0.8f).height(54.dp)) { Text("타이머 시작", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                }

                TimerState.FOCUSING, TimerState.PAUSED_FACE_UP, TimerState.BREAK -> {
                    val statusText = when (timerState) {
                        TimerState.FOCUSING -> if (focusMode == FocusMode.POMODORO) "[$currentSession/$pomoTotalSessions] 집중하는 중... (화면 차단)" else "일반 타이머 진행 중... (화면 차단)"
                        TimerState.PAUSED_FACE_UP -> "기기를 엎어놓으면 타이머가 시작/재개됩니다"
                        TimerState.BREAK -> "[$currentSession/$pomoTotalSessions] 5분 휴식 시간입니다!"
                        else -> ""
                    }
                    Text(text = statusText, style = MaterialTheme.typography.bodyLarge, color = if (timerState == TimerState.BREAK) Color(0xFF14B8A6) else Color(0xFF64748B), fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(text = timeString, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(48.dp))
                    TextButton(onClick = { pendingExitAction = { viewModel.forceStopTimer() } }) { Text("강제 종료", color = Color.Red) }
                }

                TimerState.COMPLETED -> {
                    if (focusMode == FocusMode.POMODORO) {
                        Text("🎉 포모도로 사이클 완료! 🎉", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        if (pomoTotalSessions == 4) Text("최대 사이클(4회)을 모두 완수했습니다.\n30분 이상 긴 휴식을 취해보세요.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else Text("목표한 ${pomoTotalSessions}회 집중을 완료했습니다.", textAlign = TextAlign.Center)
                    } else {
                        Text("🎉 타이머 완료! 🎉", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("설정한 시간이 모두 지났습니다.", textAlign = TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                    Button(onClick = { viewModel.forceStopTimer() }) { Text("처음으로 돌아가기") }
                }
            }
        }
    }

    // --- 4. 팝업(Dialog) 제어 ---
    if (showEnvWarningPopup) {
        AlertDialog(
            onDismissRequest = { showEnvWarningPopup = false }, title = { Text("집중 환경 경고") },
            text = { Text("현재 집중하기 어려운 환경입니다.\n(${currentDb.toInt()}dB / ${currentLux.toInt()}lx)\n\n그래도 타이머를 시작하시겠습니까?") },
            confirmButton = { TextButton(onClick = { showEnvWarningPopup = false; checkMuteAndStart() }) { Text("시작하기") } },
            dismissButton = { TextButton(onClick = { showEnvWarningPopup = false }) { Text("취소") } }
        )
    }

    if (showSilentModePopup) {
        AlertDialog(
            onDismissRequest = { showSilentModePopup = false }, title = { Text("알림 소리 꺼짐") },
            text = { Text("현재 기기가 무음 또는 진동 모드입니다.\n타이머 종료 소리를 듣지 못할 수 있습니다.\n시작하시겠습니까?") },
            confirmButton = { TextButton(onClick = { showSilentModePopup = false; viewModel.startTimer() }) { Text("시작", color = MaterialTheme.colorScheme.primary) } },
            dismissButton = { TextButton(onClick = { showSilentModePopup = false }) { Text("취소") } }
        )
    }

    if (pendingExitAction != null) {
        AlertDialog(
            onDismissRequest = { pendingExitAction = null }, title = { Text("집중 모드 종료") },
            text = { Text("진행 중인 타이머가 초기화됩니다. 정말 종료하시겠습니까?") },
            confirmButton = { TextButton(onClick = { pendingExitAction?.invoke(); pendingExitAction = null }) { Text("예", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { pendingExitAction = null }) { Text("아니오") } }
        )
    }
}