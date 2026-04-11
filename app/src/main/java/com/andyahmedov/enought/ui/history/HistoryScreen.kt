package com.andyahmedov.enought.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andyahmedov.enought.common.toRubDisplayString
import com.andyahmedov.enought.domain.model.HistoryDaySummary
import com.andyahmedov.enought.domain.model.HistoryPeriod
import com.andyahmedov.enought.ui.permissions.notificationAccessDisclosure
import com.andyahmedov.enought.ui.today.TodayPaymentEventListItem
import com.andyahmedov.enought.ui.today.toLocalTimeDisplayString
import com.andyahmedov.enought.ui.today.toSourceLabel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onSelectPeriod: (HistoryPeriod) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenNotificationAccessSettings: () -> Unit,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "History",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "Back")
                    }
                }
            }

            item {
                HistoryPeriodPicker(
                    selectedPeriod = uiState.selectedPeriod,
                    onSelectPeriod = onSelectPeriod,
                )
            }

            when (uiState) {
                is HistoryUiState.Loading -> item {
                    LoadingHistoryState(period = uiState.selectedPeriod)
                }

                is HistoryUiState.NoPermission -> item {
                    NoPermissionHistoryState(
                        onOpenNotificationAccessSettings = onOpenNotificationAccessSettings,
                    )
                }

                is HistoryUiState.Empty -> item {
                    EmptyHistoryState(
                        period = uiState.selectedPeriod,
                        hasLowConfidenceItems = uiState.hasLowConfidenceItems,
                        onOpenReview = onOpenReview,
                    )
                }

                is HistoryUiState.Ready -> {
                    item {
                        HistorySummaryCard(uiState = uiState)
                    }
                    item {
                        if (uiState.selectedPeriod == HistoryPeriod.TODAY) {
                            HistoryPaymentsCard(
                                items = uiState.paymentItems,
                            )
                        } else {
                            HistoryDaysCard(
                                period = uiState.selectedPeriod,
                                items = uiState.dayItems,
                            )
                        }
                    }
                    if (uiState.hasLowConfidenceItems) {
                        item {
                            ReviewHintCard(
                                period = uiState.selectedPeriod,
                                onOpenReview = onOpenReview,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryPeriodPicker(
    selectedPeriod: HistoryPeriod,
    onSelectPeriod: (HistoryPeriod) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(HistoryPeriod.entries) { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = {
                    onSelectPeriod(period)
                },
                label = {
                    Text(text = period.toLabel())
                },
            )
        }
    }
}

@Composable
private fun LoadingHistoryState(
    period: HistoryPeriod,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Loading ${period.toSummaryTitle().lowercase(Locale.getDefault())}…",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Preparing recent phone-spend history from local data.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun NoPermissionHistoryState(
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
                text = "History is built from the same local payment events as Today.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onOpenNotificationAccessSettings) {
                Text(text = "Open Notification Access")
            }
        }
    }
}

@Composable
private fun EmptyHistoryState(
    period: HistoryPeriod,
    hasLowConfidenceItems: Boolean,
    onOpenReview: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No confirmed phone payments in ${period.toSummaryTitle().lowercase(Locale.getDefault())}.",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Confirmed Mir Pay payments will appear here after capture and normalization.",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (hasLowConfidenceItems) {
                Text(
                    text = "There are suspected items in this period, but they are not included in the total yet.",
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
private fun HistorySummaryCard(
    uiState: HistoryUiState.Ready,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = uiState.selectedPeriod.toSummaryTitle(),
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
        }
    }
}

@Composable
private fun HistoryPaymentsCard(
    items: List<TodayPaymentEventListItem>,
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
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                HistoryPaymentRow(item = item)
            }
        }
    }
}

@Composable
private fun HistoryDaysCard(
    period: HistoryPeriod,
    items: List<HistoryDaySummary>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = period.toDayListTitle(),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
                HistoryDayRow(item = item)
            }
        }
    }
}

@Composable
private fun ReviewHintCard(
    period: HistoryPeriod,
    onOpenReview: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "There are suspected items in ${period.toSummaryTitle().lowercase(Locale.getDefault())}.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onOpenReview) {
                Text(text = "Review suspected items")
            }
        }
    }
}

@Composable
private fun HistoryPaymentRow(
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

@Composable
private fun HistoryDayRow(
    item: HistoryDaySummary,
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
                text = item.date.toHistoryDayDisplayString(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Payments: ${item.paymentsCount} • Last: ${item.lastPaymentAmountMinor?.toRubDisplayString() ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Text(
            text = item.totalAmountMinor.toRubDisplayString(),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun HistoryPeriod.toLabel(): String {
    return when (this) {
        HistoryPeriod.TODAY -> "Today"
        HistoryPeriod.LAST_7_DAYS -> "7 days"
        HistoryPeriod.LAST_30_DAYS -> "30 days"
        HistoryPeriod.LAST_90_DAYS -> "90 days"
    }
}

private fun HistoryPeriod.toSummaryTitle(): String {
    return when (this) {
        HistoryPeriod.TODAY -> "Today"
        HistoryPeriod.LAST_7_DAYS -> "Last 7 days"
        HistoryPeriod.LAST_30_DAYS -> "Last 30 days"
        HistoryPeriod.LAST_90_DAYS -> "Last 90 days"
    }
}

private fun HistoryPeriod.toDayListTitle(): String {
    return when (this) {
        HistoryPeriod.TODAY -> "Today's payments"
        HistoryPeriod.LAST_7_DAYS -> "Days in the last 7 days"
        HistoryPeriod.LAST_30_DAYS -> "Days in the last 30 days"
        HistoryPeriod.LAST_90_DAYS -> "Days in the last 90 days"
    }
}

private fun LocalDate.toHistoryDayDisplayString(): String {
    return format(HISTORY_DAY_FORMATTER)
}

private val HISTORY_DAY_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())
