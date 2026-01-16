package com.example.autismstroller.reusables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.autismstroller.utilities.cardShadow

@Composable
fun GenderBox(
    gender: String,
    isEditing: Boolean,
    onGenderChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        // Applied custom shadow helper here
        modifier = modifier.cardShadow().padding(12.dp)
    ) {
        Column {
            Text(text = "Gender", fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            if (isEditing) {
                Box(modifier = Modifier.fillMaxWidth().clickable { expanded = true }) {
                    Text(text = gender, fontSize = 14.sp, modifier = Modifier.padding(vertical = 8.dp))
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Male") }, onClick = { onGenderChange("Male"); expanded = false })
                        DropdownMenuItem(text = { Text("Female") }, onClick = { onGenderChange("Female"); expanded = false })
                    }
                }
            } else {
                Text(text = gender, fontSize = 14.sp, color = Color.Black)
            }
        }
    }
}