package com.example.autismstroller.reusables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autismstroller.utilities.cardShadow

@Composable
fun InfoBox(
    title: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Box(
        // Applied custom shadow helper here
        modifier = modifier.cardShadow().padding(12.dp)
    ) {
        Column {
            Text(text = title, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(fontSize = 14.sp)
                )
            } else {
                Text(text = value.ifEmpty { "N/A" }, fontSize = 14.sp, color = Color.Black)
            }
        }
    }
}