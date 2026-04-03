package com.andyahmedov.enought.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.andyahmedov.enought.common.parseRubAmountInput
import com.andyahmedov.enought.common.toRubDisplayString
import com.andyahmedov.enought.ui.today.toLocalTimeDisplayString
import com.andyahmedov.enought.ui.today.toSourceLabel

@Composable
fun ReviewScreen(
    uiState: ReviewUiState,
    pendingItemId: String?,
    editingItem: ReviewListItem.Single?,
    amountInput: String,
    onNavigateBack: () -> Unit,
    onConfirm: (String) -> Unit,
    onOpenCorrectAmount: (ReviewListItem.Single) -> Unit,
    onAmountInputChange: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onMergeDuplicateConflict: (String) -> Unit,
    onKeepDuplicateConflictSeparate: (String) -> Unit,
    onCancelAmountEdit: () -> Unit,
    onSubmitCorrectAmount: () -> Unit,
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
                        text = "Review suspected items",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Resolve unclear phone payments so today's total stays honest.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            when (uiState) {
                ReviewUiState.Loading -> item {
                    ReviewLoadingState()
                }

                ReviewUiState.Empty -> item {
                    ReviewEmptyState(
                        onNavigateBack = onNavigateBack,
                    )
                }

                is ReviewUiState.Ready -> item {
                    ReviewItemsCard(
                        items = uiState.items,
                        pendingItemId = pendingItemId,
                        onConfirm = onConfirm,
                        onOpenCorrectAmount = onOpenCorrectAmount,
                        onDismiss = onDismiss,
                        onMergeDuplicateConflict = onMergeDuplicateConflict,
                        onKeepDuplicateConflictSeparate = onKeepDuplicateConflictSeparate,
                    )
                }
            }
        }
    }

    if (editingItem != null) {
        CorrectAmountDialog(
            item = editingItem,
            amountInput = amountInput,
            onAmountInputChange = onAmountInputChange,
            onDismissRequest = onCancelAmountEdit,
            onConfirm = onSubmitCorrectAmount,
        )
    }
}

@Composable
private fun ReviewLoadingState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Loading suspected items…",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Checking today's unresolved payment candidates.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun ReviewEmptyState(
    onNavigateBack: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "No suspected items left for today.",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Today's total now uses the resolved events only.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onNavigateBack) {
                Text(text = "Back to Today")
            }
        }
    }
}

@Composable
private fun ReviewItemsCard(
    items: List<ReviewListItem>,
    pendingItemId: String?,
    onConfirm: (String) -> Unit,
    onOpenCorrectAmount: (ReviewListItem.Single) -> Unit,
    onDismiss: (String) -> Unit,
    onMergeDuplicateConflict: (String) -> Unit,
    onKeepDuplicateConflictSeparate: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                when (item) {
                    is ReviewListItem.DuplicateConflict -> DuplicateConflictRow(
                        item = item,
                        isPending = pendingItemId == item.duplicateGroupId,
                        onMergeDuplicateConflict = onMergeDuplicateConflict,
                        onKeepDuplicateConflictSeparate = onKeepDuplicateConflictSeparate,
                    )
                    is ReviewListItem.Single -> ReviewItemRow(
                        item = item,
                        isPending = pendingItemId == item.id,
                        onConfirm = onConfirm,
                        onOpenCorrectAmount = onOpenCorrectAmount,
                        onDismiss = onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewItemRow(
    item: ReviewListItem.Single,
    isPending: Boolean,
    onConfirm: (String) -> Unit,
    onOpenCorrectAmount: (ReviewListItem.Single) -> Unit,
    onDismiss: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = buildString {
                append(item.amountMinor.toRubDisplayString())
                append(" • ")
                append(item.paidAt.toLocalTimeDisplayString())
                append(" • ")
                append(item.sourceKind.toSourceLabel())
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                enabled = !isPending,
                onClick = { onConfirm(item.id) },
            ) {
                Text(text = "Confirm")
            }
            TextButton(
                enabled = !isPending,
                onClick = { onOpenCorrectAmount(item) },
            ) {
                Text(text = "Edit amount")
            }
            TextButton(
                enabled = !isPending,
                onClick = { onDismiss(item.id) },
            ) {
                Text(text = "Dismiss")
            }
        }
    }
}

@Composable
private fun DuplicateConflictRow(
    item: ReviewListItem.DuplicateConflict,
    isPending: Boolean,
    onMergeDuplicateConflict: (String) -> Unit,
    onKeepDuplicateConflictSeparate: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Possible duplicate phone payment",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = buildString {
                append(item.amountMinor.toRubDisplayString())
                append(" • Two overlapping signals need review")
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        item.items.forEach { conflictEntry ->
            Text(
                text = buildString {
                    append(conflictEntry.title)
                    append(" • ")
                    append(conflictEntry.paidAt.toLocalTimeDisplayString())
                    append(" • ")
                    append(conflictEntry.sourceKind.toSourceLabel())
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                enabled = !isPending,
                onClick = { onMergeDuplicateConflict(item.duplicateGroupId) },
            ) {
                Text(text = "Merge as one")
            }
            TextButton(
                enabled = !isPending,
                onClick = { onKeepDuplicateConflictSeparate(item.duplicateGroupId) },
            ) {
                Text(text = "Keep both")
            }
        }
    }
}

@Composable
private fun CorrectAmountDialog(
    item: ReviewListItem.Single,
    amountInput: String,
    onAmountInputChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val isConfirmEnabled = parseRubAmountInput(amountInput) != null

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "Edit amount")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = onAmountInputChange,
                    singleLine = true,
                    label = {
                        Text(text = "Amount in RUB")
                    },
                    placeholder = {
                        Text(text = "349.00")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                    ),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isConfirmEnabled,
                onClick = onConfirm,
            ) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "Cancel")
            }
        },
    )
}
