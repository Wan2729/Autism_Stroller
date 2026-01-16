package com.example.autismstroller.reusables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SimpleDropDownMenu(
    optionSelectedDisplayed: String,
    listOfItem: List<String>,
    onGenderSelected: (String) -> Unit
) {
    val items = listOfItem
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(optionSelectedDisplayed) }

    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopCenter)
    ){
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.DarkGray
                ),
            onClick = { expanded = true }
        ) {
            Text(text = selectedText)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.wrapContentSize()
        ) {
            items.forEach { gender ->
                DropdownMenuItem(
                    text = { Text(text = gender, color = Color.DarkGray) },
                    onClick = {
                        selectedText = gender
                        onGenderSelected(gender)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun MenuSample() {
    var genderSelected by remember {
        mutableStateOf("Select Gender")
    }
    val genders = listOf<String>("Male", "Female")

    SimpleDropDownMenu(
        optionSelectedDisplayed = genderSelected,
        listOfItem = genders,
        onGenderSelected = { gender ->
            genderSelected = gender
        }
    )
}