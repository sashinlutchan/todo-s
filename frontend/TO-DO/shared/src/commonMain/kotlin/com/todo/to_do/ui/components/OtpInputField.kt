package com.todo.to_do.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.todo.to_do.ui.theme.TaskFlowTheme

/** Six single-digit boxes that together edit one 6-digit OTP string. */
@Composable
fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false
) {
    val colors = TaskFlowTheme.colors
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(length) { index ->
            val digit = value.getOrNull(index)?.toString() ?: ""
            OutlinedTextField(
                value = digit,
                onValueChange = { input ->
                    val typed = input.filter { it.isDigit() }
                    when {
                        typed.length <= 1 -> {
                            val padded = value.padEnd(length, ' ')
                            val updated = padded.substring(0, index) + typed.ifEmpty { " " } + padded.substring(index + 1)
                            onValueChange(updated.trimEnd())
                        }
                        else -> onValueChange(typed.take(length))
                    }
                },
                modifier = Modifier.width(48.dp),
                textStyle = TextStyle(textAlign = TextAlign.Center, fontSize = MaterialTheme.typography.titleMedium.fontSize),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = isError,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = colors.textPrimary)
            )
        }
    }
}
