package com.davidots.planmind.ui.main

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.davidots.planmind.ui.calendar.CalendarScreen
import com.davidots.planmind.ui.calendar.CalendarViewModel
import com.davidots.planmind.ui.focus.FocusScreen
import com.davidots.planmind.ui.focus.FocusViewModel
import com.davidots.planmind.ui.home.HomeScreen
import com.davidots.planmind.ui.sleep.SleepAlarmState
import com.davidots.planmind.ui.sleep.SleepScreen
import com.davidots.planmind.ui.sleep.SleepViewModel
import com.davidots.planmind.ui.schedule.ScheduleViewModel
import com.davidots.planmind.ui.schedule.ScheduleDetailScreen


// 앱의 전체 화면 이동(Navigation)을 관리하는 최상위 UI 컨테이너

@Composable
fun MainScreen(
    calendarViewModel: CalendarViewModel,
    focusViewModel: FocusViewModel,
    sleepViewModel: SleepViewModel,
    scheduleViewModel: ScheduleViewModel,
    startDestination: String = "home",
    initialSleepState: SleepAlarmState = SleepAlarmState.IDLE
) {
    // 화면 이동을 제어하는 내비게이션 컨트롤러 객체 생성
    val navController = rememberNavController()

    // NavHost를 통해 라우팅 테이블(지도)을 구성
    // MainActivity가 계산해서 넘겨준 startDestination 변수를 사용
    NavHost(navController = navController, startDestination = startDestination) {

        // --- 0. 홈(로비) 화면 ---
        composable("home") {
            HomeScreen(
                calendarViewModel = calendarViewModel,
                onNavigateToCalendar = { navController.navigate("calendar") },
                onNavigateToFocus = { navController.navigate("focus") },
                onNavigateToSleep = { navController.navigate("sleep") },
                onNavigateToDetail = { dateString, scheduleId ->
                    val idToPass = scheduleId ?: -1
                    navController.navigate("detail/$dateString/$idToPass")
                }
            )
        }

        // --- 1. 달력 화면 ---
        composable("calendar") {
            CalendarScreen(
                viewModel = calendarViewModel,
                onNavigateToFocus = {
                    // 백스택에 화면이 쌓이지 않도록 단일 이동 처리
                    navController.navigate("focus") { popUpTo("home") }
                },
                onNavigateToSleep = {
                    navController.navigate("sleep") { popUpTo("home") }
                },
                onNavigateToDetail = { dateString, scheduleId ->
                    // id가 null(새로 추가)일 경우 -1을 전달하여 라우팅 경로를 만듬
                    val idToPass = scheduleId ?: -1
                    navController.navigate("detail/$dateString/$idToPass")
                }
            )
        }

        // --- 2. 집중 모드 화면 ---
        composable("focus") {
            FocusScreen(
                viewModel = focusViewModel,
                onNavigateToCalendar = {
                    // 달력으로 돌아갈 때 백스택 초기화
                    navController.navigate("calendar") { popUpTo("home") }
                },
                onNavigateToSleep = {
                    navController.navigate("sleep") { popUpTo("home") }
                },
                onNavigateToHome = { navController.popBackStack() }
            )
        }

        // --- 3. 수면/기상 관리 화면 ---
        composable("sleep") {
            SleepScreen(
                viewModel = sleepViewModel,
                initialAlarmState = initialSleepState,
                onNavigateToCalendar = {
                    navController.navigate("calendar") { popUpTo("home") }
                },
                onNavigateToFocus = {
                    navController.navigate("focus") { popUpTo("home") }
                }
            )
        }

        // --- 4. 일정 상세/추가 화면 ---
        // 경로에 변수({date}, {id})를 포함하여 데이터를 전달받음
        composable(
            route = "detail/{date}/{id}",
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("id") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val date = backStackEntry.arguments?.getString("date") ?: ""
            val idParam = backStackEntry.arguments?.getInt("id") ?: -1
            // 전달받은 id가 -1이면 새로 만들기(null)로 처리
            val scheduleId = if (idParam == -1) null else idParam

            ScheduleDetailScreen(
                viewModel = scheduleViewModel,
                date = date,
                scheduleId = scheduleId,
                onDismiss = {
                    // 알림을 타고 바로 들어온 경우 뒤로 가기가 꼬일 수 있으므로 홈으로 강제 복귀 유도
                    if (navController.previousBackStackEntry == null) {
                        navController.navigate("home") { popUpTo(0) }
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        }
    }
}