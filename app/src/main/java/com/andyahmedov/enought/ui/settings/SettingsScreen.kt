package com.andyahmedov.enought.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andyahmedov.enought.common.toRubDisplayString

@Composable
fun SettingsScreen(
    currentLimitAmountMinor: Long?,
    amountInput: String,
    isSaveEnabled: Boolean,
    isClearEnabled: Boolean,
    onNavigateBack: () -> Unit,
    onAmountInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Daily limit",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Set one calm spending threshold for today's phone payments. Warnings appear near 80% and when you go over.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Current setting",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = currentLimitAmountMinor?.let { amountMinor ->
                                "Daily limit: ${amountMinor.toRubDisplayString()}"
                            } ?: "No daily limit yet. Add one to see how much is left today.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = onAmountInputChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = {
                                Text(text = "Limit in RUB")
                            },
                            placeholder = {
                                Text(text = "2500")
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                            ),
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                enabled = isSaveEnabled,
                                onClick = onSave,
                            ) {
                                Text(text = "Save")
                            }
                            TextButton(
                                enabled = isClearEnabled,
                                onClick = onClear,
                            ) {
                                Text(text = "Clear limit")
                            }
                        }
                    }
                }
            }

            item {
                TextButton(onClick = onNavigateBack) {
                    Text(text = "Back to Today")
                }
            }
        }
    }
}
