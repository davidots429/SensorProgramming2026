package com.davidots.planmind.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 위아래로 스크롤하여 숫자를 선택하는 커스텀 휠 피커 컴포넌트
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NumberWheelPicker(
    range: IntRange, // 피커가 표현할 숫자의 범위
    value: Int, // 현재 선택된 실시간 숫자 데이터 매핑
    onValueChange: (Int) -> Unit, // 사용자가 굴려서 숫자가 확정되었을 때 상위로 전달하는 이벤트
    modifier: Modifier = Modifier,
    isInfinite: Boolean = false // 무한 루프 스크롤을 활성화할 것인지 지정
) {
    val count = range.count()
    val listSize = if (isInfinite) Int.MAX_VALUE else count

    // 무한 스크롤 연출 시, 대규모 가상 리스트의 정중앙 부근에서 연산이 시작되도록 시작 인덱스를 보정
    val half = listSize / 2
    val startIndex = half - (half % count) + (value - range.first)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = value)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // 매 스크롤 변화 시 컴포저블이 포맷 함수를 다시 타는 현상을 제거하기 위한 구조적 장치
    val formattedStrings = remember(range) {
        range.map { String.format("%02d", it) }
    }

    // 최상단에 노출되고 있는 visible 항목을 derivedStateOf 상태 래퍼로 묶음
    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    // 화면이 처음 활성화되어 드로잉될 때 설정된 타겟 위치로 휠 포커스를 강제 이동
    LaunchedEffect(Unit) {
        if (listState.firstVisibleItemIndex < 10000) {
            listState.scrollToItem(startIndex)
        }
    }

    // 퀵 프리셋 버튼을 눌렀을 때, 휠 피커가 원거리에서 최단 거리로 스무스하게 애니메이션 스크롤되도록 보정하는 로직
    LaunchedEffect(value) {
        val currentIndex = listState.firstVisibleItemIndex
        val currentValue = range.elementAt(currentIndex % count)

        if (currentValue != value) {
            if (isInfinite) {
                // 위로 굴리는 것과 아래로 굴리는 것 중 더 가까운 인덱스 오프셋 거리를 연산
                val diff1 = value - currentValue
                val diff2 = if (diff1 > 0) diff1 - count else diff1 + count
                val minDiff = if (kotlin.math.abs(diff1) < kotlin.math.abs(diff2)) diff1 else diff2

                listState.animateScrollToItem(currentIndex + minDiff)
            } else {
                listState.animateScrollToItem(value - range.first)
            }
        }
    }

    // 사용자가 손가락 제스처로 드래그하는 도중에는 이벤트를 발생시키지 않고, 스크롤 동작이 완전히 멈춘 상태가 되었을 때만 최종 선택값을 리턴
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val finalCenterIndex = listState.firstVisibleItemIndex
            if (isInfinite) {
                onValueChange(range.elementAt(finalCenterIndex % count))
            } else {
                if (finalCenterIndex in 0 until count) {
                    onValueChange(range.elementAt(finalCenterIndex))
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 60.dp)
    ) {
        items(count = listSize) { index ->
            val actualIndex = if (isInfinite) index % count else index
            val isCenter = index == centerIndex

            Text(
                text = formattedStrings[actualIndex],
                fontSize = if (isCenter) 36.sp else 28.sp,
                fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                color = if (isCenter) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .graphicsLayer {
                        alpha = if (isCenter) 1f else 0.3f
                    },
                textAlign = TextAlign.Center
            )
        }
    }
}