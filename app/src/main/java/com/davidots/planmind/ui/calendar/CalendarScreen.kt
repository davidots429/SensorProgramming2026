package com.davidots.planmind.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.TargetedFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.davidots.planmind.ui.calendar.components.DailyTimeTableView
import com.davidots.planmind.ui.calendar.components.DailySchedulePopup
import com.davidots.planmind.ui.calendar.components.MonthlyView
import com.davidots.planmind.ui.calendar.components.WeeklyView
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// 캘린더 화면의 전체 뼈대 구성 및 페이징 레이아웃 조작을 수렴 처리하는 메인 프레임 스크린 컴포저블
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel, // 정형화 분리된 전용 캘린더 뷰모델을 의존성 주입받아 사용
    onNavigateToFocus: () -> Unit,
    onNavigateToSleep: () -> Unit,
    onNavigateToDetail: (String, Int?) -> Unit
) {
    // 뷰모델 상태 엔진이 방출하는 코어 가변 스트림 현황들을 실시간 결합 관찰(State) 레이어로 확보
    val viewType by viewModel.viewType.collectAsState()
    val targetDate by viewModel.targetDate.collectAsState()
    val schedulesByDate by viewModel.schedulesByDate.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var popupDate by remember { mutableStateOf<LocalDate?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var quickAddText by remember { mutableStateOf("") }

    val initialPage = 500
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { 1000 })

    // 화면 생명주기(Lifecycle) 상태 확보
    // 뒤로 가기를 눌러 캘린더가 닫히는 0.3초의 페이드아웃 애니메이션 도중에 터치를 무시하기 위함
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // 페이저 가로 스와이프 조작량과 뷰포트 중심 날짜 오프셋의 실시간 도출 연산
    val currentPageOffset = pagerState.currentPage - initialPage
    val displayedDate = remember(targetDate, viewType, currentPageOffset) {
        when (viewType) {
            ViewType.MONTHLY -> targetDate.plusMonths(currentPageOffset.toLong())
            ViewType.WEEKLY -> targetDate.plusWeeks(currentPageOffset.toLong())
            ViewType.DAILY -> targetDate.plusDays(currentPageOffset.toLong())
        }
    }

    // 슬라이드 전환 동작으로 실제 visible 날짜가 변동되면, 뷰모델에 감시 대역 주차 범위 리프레시를 자동 위임 요청
    LaunchedEffect(displayedDate, viewType) {
        viewModel.updateDisplayedDate(displayedDate, viewType)
    }

    // 오늘 버튼 입력 등으로 타겟 날짜 좌표축의 근본적 수정이 가해지면 드럼통 페이저 인덱스를 정중앙(500)으로 되감기 제어
    LaunchedEffect(targetDate) {
        popupDate = null
        if (pagerState.currentPage != initialPage) {
            pagerState.scrollToPage(initialPage)
        }
    }

    // 기본 Pager 동작
    val defaultFlingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(1)
    )

    // 가속도를 강제로 제한하는 래퍼(Wrapper) 로직
    val cappedFlingBehavior = remember(defaultFlingBehavior) {
        object : TargetedFlingBehavior {
            override suspend fun ScrollScope.performFling(
                initialVelocity: Float,
                onRemainingDistanceUpdated: (Float) -> Unit
            ): Float {
                // 아무리 세게 스와이프해도 가속도를 -2500f ~ 2500f 사이로 제한
                val limitedVelocity = initialVelocity.coerceIn(-2500f, 2500f)

                return with(defaultFlingBehavior) {
                    performFling(limitedVelocity, onRemainingDistanceUpdated)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // --- 1. 상단 타이틀 바 및 스위칭 조작 단추 바 세션 ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.Menu, contentDescription = "메뉴", tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(text = { Text("일정 (현재 화면)") }, onClick = { showMenu = false })
                    DropdownMenuItem(text = { Text("집중 타이머") }, onClick = { showMenu = false; onNavigateToFocus() })
                    DropdownMenuItem(text = { Text("수면/기상 관리") }, onClick = { showMenu = false; onNavigateToSleep() })
                }
            }

            val headerText = when (viewType) {
                ViewType.MONTHLY -> "${displayedDate.year}년 ${displayedDate.monthValue}월"
                ViewType.WEEKLY -> "${displayedDate.year}년 ${displayedDate.monthValue}월 ${CalendarUiUtil.getWeekOfMonth(displayedDate)}째 주"
                ViewType.DAILY -> "${displayedDate.year}년 ${displayedDate.monthValue}월 ${displayedDate.dayOfMonth}일"
            }
            Text(headerText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Button(
                onClick = { viewModel.toggleViewType() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(viewType.title, fontWeight = FontWeight.Bold)
            }
        }

        // --- 2. 중앙 수평 무한 스크롤 페이징 결합 캘린더 드로잉 세션 ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            flingBehavior = cappedFlingBehavior
        ) { page ->
            val pageOffset = page - initialPage
            when (viewType) {
                ViewType.MONTHLY -> {
                    MonthlyView(
                        displayMonth = YearMonth.from(targetDate).plusMonths(pageOffset.toLong()),
                        schedulesByDate = schedulesByDate,
                        onDayClick = {
                            // 화면이 완전히 멈춰있는 활성 상태(RESUMED)일 때만 터치를 허용하여 유령 터치 방어
                            if (lifecycle.currentState == Lifecycle.State.RESUMED) {
                                popupDate = it
                            }
                        }
                    )
                }
                ViewType.WEEKLY -> {
                    WeeklyView(
                        displayDate = targetDate.plusWeeks(pageOffset.toLong()),
                        schedulesByDate = schedulesByDate,
                        onAddClick = { dateString ->
                            if (lifecycle.currentState == Lifecycle.State.RESUMED) onNavigateToDetail(dateString, null)
                        },
                        onScheduleClick = { dateString, id ->
                            if (lifecycle.currentState == Lifecycle.State.RESUMED) onNavigateToDetail(dateString, id)
                        }
                    )
                }
                ViewType.DAILY -> {
                    DailyTimeTableView(
                        displayDate = targetDate.plusDays(pageOffset.toLong()),
                        schedulesByDate = schedulesByDate,
                        onScheduleClick = { dateString, id ->
                            if (lifecycle.currentState == Lifecycle.State.RESUMED) onNavigateToDetail(dateString, id)
                        },
                        onTimeUpdate = { id, start, end -> viewModel.updateScheduleTime(id, start, end) }
                    )
                }
            }
        }

        // --- 3. 하단 오늘 강제 복귀 및 한 줄 빠른 일정 추가 조작 레이어 세션 ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.moveToToday() }) {
                Icon(Icons.Default.Refresh, contentDescription = "오늘로 이동", tint = MaterialTheme.colorScheme.primary)
            }

            OutlinedTextField(
                value = quickAddText,
                onValueChange = { quickAddText = it },
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                placeholder = { Text("오늘의 일정 간단 추가") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            IconButton(
                onClick = {
                    if (quickAddText.isNotBlank()) {
                        viewModel.addQuickSchedule(quickAddText, displayedDate)
                        quickAddText = ""
                    }
                },
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            ) {
                Icon(Icons.Default.Add, contentDescription = "추가", tint = Color.White)
            }
        }

        popupDate?.let { date ->
            val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            DailySchedulePopup(
                date = date,
                schedules = schedulesByDate[dateString] ?: emptyList(),
                onDismiss = { popupDate = null },
                onAddClick = {
                    popupDate = null
                    onNavigateToDetail(dateString, null)
                },
                onScheduleClick = { id ->
                    popupDate = null
                    onNavigateToDetail(dateString, id)
                },
                onDeleteConfirm = { listToDelete ->
                    listToDelete.forEach { viewModel.deleteSchedule(it, context) }
                }
            )
        }
    }
}