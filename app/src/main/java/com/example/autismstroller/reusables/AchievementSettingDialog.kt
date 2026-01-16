package com.example.autismstroller.reusables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.autismstroller.functional.AchievementHandler
import com.example.autismstroller.functional.AchievementHandlerFactory
import com.example.autismstroller.functional.UserHandler
import com.example.autismstroller.models.Achievement
import com.example.autismstroller.models.AchievementCondition
import com.example.autismstroller.models.Operator
import com.example.autismstroller.models.User
import com.example.autismstroller.models.UserStatDefinition
import com.example.autismstroller.models.UserStatDefinitions
import java.util.UUID

@Composable
fun AchievementSettingDialog(
    onAchievementCreated: (Achievement) -> Unit,
    onDismiss: () -> Unit
) {
    // -- Achievement fields
    var id by remember { mutableStateOf("") }
    var iconRes by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    // -- Condition fields (single condition for now)
    var selectedStat by remember {
        mutableStateOf<UserStatDefinition?>(null)
    }
    var operator by remember { mutableStateOf(Operator.EQUAL) }
    var value by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                // ── Title
                Text(
                    text = "Create Achievement",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                AchievementTextField("Achievement ID", id) { id = it }
                AchievementTextField("Icon Path (Firebase)", iconRes) { iconRes = it }
                AchievementTextField("Title", title) { title = it }
                AchievementTextField(
                    label = "Description",
                    value = description,
                    singleLine = false
                ) { description = it }

                Spacer(Modifier.height(12.dp))

                Divider()

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Unlock Condition",
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                StatDropdown(
                    selectedStat = selectedStat,
                    onSelected = { selectedStat = it }
                )

                OperatorDropdown(
                    selectedOperator = operator,
                    onSelected = { operator = it }
                )

                AchievementTextField(
                    label = "Value",
                    value = value,
                    keyboardType = KeyboardType.Number
                ) { value = it }

                Spacer(Modifier.height(20.dp))

                // ── Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val condition = AchievementCondition(
                                stat = selectedStat?.key ?: return@Button,
                                operator = operator,
                                value = value.toIntOrNull() ?: 0
                            )

                            val achievement = Achievement(
                                id = id.ifBlank { UUID.randomUUID().toString() },
                                iconRes = iconRes,
                                title = title,
                                description = description,
                                completed = false,
                                unlockedAt = null,
                                stats = listOf(condition.stat),
                                unlockCondition = listOf(condition)
                            )

                            onAchievementCreated(achievement)
                        }
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementTextField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(10.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatDropdown(
    selectedStat: UserStatDefinition?,
    onSelected: (UserStatDefinition) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedStat?.displayName ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text("Stat") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .menuAnchor()   // 🔑 IMPORTANT
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            UserStatDefinitions.all.forEach { stat ->
                DropdownMenuItem(
                    text = { Text(stat.displayName) },
                    onClick = {
                        onSelected(stat)
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorDropdown(
    selectedOperator: Operator,
    onSelected: (Operator) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedOperator.name.replace("_", " "),
            onValueChange = {},
            readOnly = true,
            label = { Text("Operator") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier
                .menuAnchor()   // 🔑 REQUIRED
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Operator.values().forEach { op ->
                DropdownMenuItem(
                    text = { Text(op.name.replace("_", " ")) },
                    onClick = {
                        onSelected(op)
                        expanded = false
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))
}