package com.davidots.planmind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


//"일정"과 "리마인더"처럼 상호 배타적인 옵션 중 하나를 탭하여 선택할 수 있도록 돕는 세그먼트 버튼 컴포넌트
@Composable
fun SegmentedButtonUI(
    selectedType: String, // viewmodel이나 상위 screen에서 관리하는 현재 선택 유형 상태 값
    onTypeSelected: (String) -> Unit, // 사용자가 탭을 전환했을 때 상위 데이터 레이어로 변경을 알리는 이벤트 콜백
    isEnabled: Boolean // 단순 상세 조회 중에는 탭이 변경되지 않도록 터치 활성화 여부를 결정
) {
    Row(
        modifier = Modifier.background(Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
    ) {
        listOf("일정", "리마인더").forEach { item ->
            val isSelected = selectedType == item

            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .background(
                        // 선택된 항목에는 메인 브랜드 컬러 배경을 칠하고, 비선택 항목은 투명하게 비워둠
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    // 모디파이어 체이닝을 활용. 활성화 상태일 때만 클릭 리스너를 결합하여 불필요한 리소스 낭비 방지
                    .then(if (isEnabled) Modifier.clickable { onTypeSelected(item) } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}