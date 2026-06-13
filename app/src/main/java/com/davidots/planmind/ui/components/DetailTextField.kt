package com.davidots.planmind.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

// 앱 전역에서 일정 제목, 메모 등의 텍스트 입력을 위해 공통으로 사용하는 텍스트 필드
@Composable
fun DetailTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isEditMode: Boolean, // 현재 화면이 수정/추가 가능한 상태인지 여부를 제어
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    // disabled 상태가 되어도 사용자가 입력된 내용을 또렷하게 파악할 수 있도록 텍스트와 테두리 색상을 커스텀 정의
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        disabledTextColor = MaterialTheme.colorScheme.onSurface,
        disabledBorderColor = Color.Gray,
        disabledLabelColor = MaterialTheme.colorScheme.onSurface,
        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        enabled = isEditMode, // 보기 모드일 때는 사용자의 키보드 입력 및 터치 인터랙션을 차단
        colors = textFieldColors,
        modifier = modifier.fillMaxWidth(),
        trailingIcon = trailingIcon
    )
}