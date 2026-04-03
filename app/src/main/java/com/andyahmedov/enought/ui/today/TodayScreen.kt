package com.andyahmedov.enought.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andyahmedov.enought.common.toRubDisplayString
import com.andyahmedov.enought.domain.model.DailyLimitWarningLevel
import com.andyahmedov.enought.ui.permissions.notificationAccessDisclosure
import kotlin.math.absoluteValue

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onSetWidgetPrivateModeEnabled: (Boolean) -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
    onOpenRawNotifications: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenSettings: () -> Unit,
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
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.headlineMedium,
                )
            }

            when (uiState) {
                TodayUiState.Loading -> item {
                    LoadingTodayState()
                }

                TodayUiState.NoPermission -> item {
                    NoPermissionTodayState(
                        onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                    )
                }

                is TodayUiState.Empty -> item {
                    EmptyTodayState(
                        uiState = uiState,
                        onSetWidgetPrivateModeEnabled = onSetWidgetPrivateModeEnabled,
                        onOpenReview = onOpenReview,
                        onOpenSettings = onOpenSettings,
                    )
                }

                is TodayUiState.Ready -> {
                    item {
                        ReadySummaryCard(
                            uiState = uiState,
                            onSetWidgetPrivateModeEnabled = onSetWidgetPrivateModeEnabled,
                            onOpenReview = onOpenReview,
                            onOpenSettings = onOpenSettings,
                        )
                    }
                    item {
                        TodayEventsCard(
                            events = uiState.events,
                        )
                    }
                }
            }

            item {
                DeveloperActions(
                    onOpenRawNotifications = onOpenRawNotifications,
                )
            }
        }
    }
}

@Composable
private fun LoadingTodayState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Loading today's spend…",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Checking notification access and today's confirmed payments.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun NoPermissionTodayState(
    onOpenNotificationAccessSettings: () -> Unit,
) {
    val disclosure = notificationAccessDisclosure()

    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Notification access is required.",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = disclosure.todayNoPermissionText,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = disclosure.todayNoPermissionSupportingText,
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onOpenNotificationAccessSettings) {
                Text(text = "Open Notification Access")
            }
        }
    }
}

@Composable
private fun EmptyTodayState(
    uiState: TodayUiState.Empty,
    onSetWidgetPrivateModeEnabled: (Boolean) -> Unit,
    onOpenReview: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No confirmed phone payments today.",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Confirmed Mir Pay payments will appear here after they are captured and normalized.",
                style = MaterialTheme.typography.bodyLarge,
            )
            DailyLimitSection(
                limitAmountMinor = uiState.limitAmountMinor,
                remainingAmountMinor = uiState.remainingAmountMinor,
                limitWarningLevel = uiState.limitWarningLevel,
                onOpenSettings = onOpenSettings,
            )
            WidgetPrivacyModeToggle(
                isEnabled = uiState.isWidgetPrivateModeEnabled,
                onCheckedChange = onSetWidgetPrivateModeEnabled,
            )
            if (uiState.hasLowConfidenceItems) {
                Text(
                    text = "There are suspected items today, but they are not included in the total yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onOpenReview) {
                    Text(text = "Review suspected items")
                }
            }
        }
    }
}

@Composable
private fun ReadySummaryCard(
    uiState: TodayUiState.Ready,
    onSetWidgetPrivateModeEnabled: (Boolean) -> Unit,
    onOpenReview: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Spent today",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = uiState.totalAmountMinor.toRubDisplayString(),
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = "Confirmed payments: ${uiState.paymentsCount}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Last payment: ${uiState.lastPaymentAmountMinor?.toRubDisplayString() ?: "—"}",
                style = MaterialTheme.typography.bodyLarge,
            )
            DailyLimitSection(
                limitAmountMinor = uiState.limitAmountMinor,
                remainingAmountMinor = uiState.remainingAmountMinor,
                limitWarningLevel = uiState.limitWarningLevel,
                onOpenSettings = onOpenSettings,
            )
            WidgetPrivacyModeToggle(
                isEnabled = uiState.isWidgetPrivateModeEnabled,
                onCheckedChange = onSetWidgetPrivateModeEnabled,
            )
            if (uiState.hasLowConfidenceItems) {
                Text(
                    text = "There are suspected items today. They are not included in the total yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onOpenReview) {
                    Text(text = "Review suspected items")
                }
            }
        }
    }
}

@Composable
private fun DailyLimitSection(
    limitAmountMinor: Long?,
    remainingAmountMinor: Long?,
    limitWarningLevel: DailyLimitWarningLevel?,
    onOpenSettings: () -> Unit,
) {
    if (limitAmountMinor == null || remainingAmountMinor == null) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "No daily limit yet. Add one to see how much is left today.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onOpenSettings) {
                Text(text = "Set daily limit")
            }
        }
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Daily limit: ${limitAmountMinor.toRubDisplayString()}",
            style = MaterialTheme.typography.titleMedium,
        )
        LinearProgressIndicator(
            progress = {
                calculateLimitProgress(
                    limitAmountMinor = limitAmountMinor,
                    remainingAmountMinor = remainingAmountMinor,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = when {
                remainingAmountMinor > 0L -> "Left today: ${remainingAmountMinor.toRubDisplayString()}"
                remainingAmountMinor == 0L -> "Today's limit is fully used."
                else -> "Over limit by ${remainingAmountMinor.absoluteValue.toRubDisplayString()}"
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        limitWarningLevel?.let { warningLevel ->
            Text(
                text = when (warningLevel) {
                    DailyLimitWarningLevel.NEAR_LIMIT -> "You are close to today's limit."
                    DailyLimitWarningLevel.LIMIT_REACHED -> {
                        "Today's confirmed phone spend is already over the limit."
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onOpenSettings) {
            Text(text = "Edit daily limit")
        }
    }
}

@Composable
private fun WidgetPrivacyModeToggle(
    isEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Widget privacy",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Hide the exact amount on the home screen widget.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
private fun TodayEventsCard(
    events: List<TodayPaymentEventListItem>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Today's payments",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            events.forEachIndexed { index, event ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                TodayPaymentEventRow(
                    item = event,
                )
            }
        }
    }
}

@Composable
private fun TodayPaymentEventRow(
    item: TodayPaymentEventListItem,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = buildString {
                    append(item.paidAt.toLocalTimeDisplayString())
                    append(" • ")
                    append(item.sourceKind.toSourceLabel())
                    if (item.userEdited) {
                        append(" • Edited")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = item.amountMinor.toRubDisplayString(),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun calculateLimitProgress(
    limitAmountMinor: Long,
    remainingAmountMinor: Long,
): Float {
    val spentAmountMinor = (limitAmountMinor - remainingAmountMinor)
        .coerceAtLeast(0L)
    val progress = spentAmountMinor.toFloat() / limitAmountMinor.toFloat()

    return progress.coerceIn(0f, 1f)
}

@Composable
private fun DeveloperActions(
    onOpenRawNotifications: () -> Unit,
) {
    TextButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenRawNotifications,
    ) {
        Text(
            text = "Open Raw Notifications Debug Screen",
            textAlign = TextAlign.Center,
        )
    }
}
