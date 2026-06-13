package com.davidots.planmind.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 앱 전역에서 시간 입력을 위해 재사용하는 커스텀 무한 휠 피커 컴포넌트입니다.
@Composable
fun InlineWheelTimePicker(
    timeStr: String,
    onTimeChange: (String) -> Unit
) {
    val parts = timeStr.split(":")
    val h24 = parts.getOrNull(0)?.toIntOrNull() ?: 9
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

    val isPm = h24 >= 12
    val h12 = if (h24 % 12 == 0) 12 else h24 % 12

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "오전",
                modifier = Modifier
                    .clickable {
                        if (isPm) {
                            val newH24 = if (h12 == 12) 0 else h12
                            onTimeChange(String.format("%02d:%02d", newH24, m))
                        }
                    }
                    .padding(8.dp)
                    .alpha(if (!isPm) 1f else 0.3f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (!isPm) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "오후",
                modifier = Modifier
                    .clickable {
                        if (!isPm) {
                            val newH24 = if (h12 == 12) 12 else h12 + 12
                            onTimeChange(String.format("%02d:%02d", newH24, m))
                        }
                    }
                    .padding(8.dp)
                    .alpha(if (isPm) 1f else 0.3f),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPm) MaterialTheme.colorScheme.primary else Color.Gray
            )
        }

        NumberWheelPicker(
            range = 1..12,
            value = h12,
            onValueChange = { newH12 ->
                val newH24 = when {
                    isPm && newH12 == 12 -> 12
                    isPm && newH12 < 12 -> newH12 + 12
                    !isPm && newH12 == 12 -> 0
                    else -> newH12
                }
                onTimeChange(String.format("%02d:%02d", newH24, m))
            },
            modifier = Modifier.weight(1f),
            isInfinite = true
        )

        Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))

        NumberWheelPicker(
            range = 0..59,
            value = m,
            onValueChange = { newM ->
                onTimeChange(String.format("%02d:%02d", h24, newM))
            },
            modifier = Modifier.weight(1f),
            isInfinite = true
        )
    }
}