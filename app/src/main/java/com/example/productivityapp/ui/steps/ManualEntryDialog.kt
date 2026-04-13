package com.example.productivityapp.ui.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.example.productivityapp.ui.theme.BlendLight
import com.example.productivityapp.ui.theme.BlendPrimaryDark
import com.example.productivityapp.ui.theme.TextPrimary

@Composable
fun ManualEntryDialog(show: Boolean, onDismiss: () -> Unit, onAdd: (Int) -> Unit) {
    if (!show) return
    var input by rememberSaveable { mutableStateOf("") }
    val isDarkMode = isSystemInDarkTheme()
    val dialogContainerColor = if (isDarkMode) BlendPrimaryDark else BlendLight
    val dialogContentColor = if (isDarkMode) Color.White else TextPrimary
    val buttonContainerColor = MaterialTheme.colorScheme.surface
    val buttonContentColor = MaterialTheme.colorScheme.onSurface

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogContainerColor,
        iconContentColor = dialogContentColor,
        titleContentColor = dialogContentColor,
        textContentColor = dialogContentColor,
        title = { Text("Add steps") },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { ch -> ch.isDigit() }.take(6) },
                    label = { Text("Steps") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = dialogContentColor,
                        unfocusedTextColor = dialogContentColor,
                        focusedBorderColor = dialogContentColor.copy(alpha = 0.85f),
                        unfocusedBorderColor = dialogContentColor.copy(alpha = 0.55f),
                        focusedLabelColor = dialogContentColor.copy(alpha = 0.85f),
                        unfocusedLabelColor = dialogContentColor.copy(alpha = 0.7f),
                        cursorColor = dialogContentColor,
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { val cur = input.toIntOrNull() ?: 0; input = (cur + 100).toString() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonContainerColor,
                            contentColor = buttonContentColor,
                        )
                    ) { Text("+100") }
                    Button(
                        onClick = { val cur = input.toIntOrNull() ?: 0; input = (cur + 500).toString() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonContainerColor,
                            contentColor = buttonContentColor,
                        )
                    ) { Text("+500") }
                    OutlinedButton(
                        onClick = { input = "" },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = dialogContentColor),
                        border = BorderStroke(1.dp, dialogContentColor.copy(alpha = 0.6f)),
                    ) { Text("Clear") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                input.toIntOrNull()?.takeIf { it > 0 }?.let { onAdd(it) }
                onDismiss()
            }, colors = ButtonDefaults.textButtonColors(contentColor = dialogContentColor)) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = dialogContentColor)) { Text("Cancel") }
        }
    )
}

@Composable
fun AddStepsFab(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val fabWidth = 56.dp
    val fabHeight = 44.dp
    val cornerRadius = 8.dp
    val isDarkMode = isSystemInDarkTheme()
    val fabContainerColor = if (isDarkMode) BlendPrimaryDark else BlendLight
    val fabContentColor = if (isDarkMode) Color.White else TextPrimary
    Surface(
        modifier = modifier
            .size(fabWidth, fabHeight)
            .shadow(6.dp, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(cornerRadius),
        color = fabContainerColor,
        tonalElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add steps", tint = fabContentColor)
        }
    }
}
