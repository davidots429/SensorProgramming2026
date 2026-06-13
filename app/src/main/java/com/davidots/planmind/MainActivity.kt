// 파일 경로: com/davidots/planmind/MainActivity.kt
package com.davidots.planmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.davidots.planmind.data.local.AppDatabase
import com.davidots.planmind.ui.calendar.CalendarViewModel
import com.davidots.planmind.ui.focus.FocusViewModel
import com.davidots.planmind.ui.main.MainScreen
import com.davidots.planmind.ui.sleep.SleepAlarmState
import com.davidots.planmind.ui.sleep.SleepViewModel
import com.davidots.planmind.ui.theme.PlanMindTheme
import com.davidots.planmind.ui.schedule.ScheduleViewModel
import com.davidots.planmind.ui.schedule.ScheduleViewModelFactory


// 앱 구동 시 가장 먼저 실행되는 Android 진입점(Entry Point)
// I 렌더링 로직을 가지지 않고, 앱의 생명주기와 데이터베이스, 뷰모델 등의 환경 설정만 담당
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Room 데이터베이스 및 DAO(Data Access Object) 초기화
        val database = AppDatabase.getDatabase(this)
        val scheduleDao = database.scheduleDao()
        val sleepRecordDao = database.sleepRecordDao()

        // 2. [의존성 수동 주입] 각 화면에 필요한 전용 뷰모델들을 생성
        // Hilt나 Dagger 같은 라이브러리 없이 ViewModelFactory를 사용하여 DAO를 주입

        val calendarViewModel: CalendarViewModel by viewModels {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(scheduleDao) as T
            }
        }

        val sleepViewModel: SleepViewModel by viewModels {
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = SleepViewModel(sleepRecordDao) as T
            }
        }

        // FocusViewModel은 별도의 DB 접근이 필요 없으므로 기본 생성자로 초기화합니다.
        val focusViewModel: FocusViewModel by viewModels()

        // ScheduleDetailScreen 용도로 남겨둔 기존 뷰모델입니다.
        val scheduleViewModel: ScheduleViewModel by viewModels {
            ScheduleViewModelFactory(scheduleDao)
        }

        // 인텐트로 넘어온 알람 데이터를 분석하여 진입 화면(라우트) 결정
        val triggerSleepAlarm = intent.getStringExtra("TRIGGER_SLEEP_ALARM")
        val targetScheduleId = intent.getIntExtra("scheduleId", -1)
        val targetScheduleDate = intent.getStringExtra("scheduleDate") ?: ""

        val initialRoute = when {
            triggerSleepAlarm != null -> "sleep"
            targetScheduleId != -1 -> "detail/$targetScheduleDate/$targetScheduleId"
            else -> "home"
        }

        val initialSleepState = when (triggerSleepAlarm) {
            "SLEEP" -> SleepAlarmState.SLEEP_WAITING
            "WAKE" -> SleepAlarmState.WAKE_RINGING
            else -> SleepAlarmState.IDLE
        }

        // 3. Compose UI 그리기 시작
        setContent {
            PlanMindTheme {
                // UI 로직은 모두 MainScreen에 위임하여 Activity를 최대한 가볍게 유지합니다.
                MainScreen(
                    calendarViewModel = calendarViewModel,
                    focusViewModel = focusViewModel,
                    sleepViewModel = sleepViewModel,
                    scheduleViewModel = scheduleViewModel,
                    startDestination = initialRoute,       // 계산된 도착지로 직행
                    initialSleepState = initialSleepState  // 수면 미션 상태 전달
                )
            }
        }
    }
}