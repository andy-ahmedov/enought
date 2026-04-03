package com.andyahmedov.enought.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andyahmedov.enought.domain.repository.RawNotificationEventRepository

@Composable
fun RawNotificationsRoute(
    rawNotificationEventRepository: RawNotificationEventRepository,
    onNavigateBack: () -> Unit,
) {
    val rawEvents by rawNotificationEventRepository.observeRawEvents().collectAsState(initial = emptyList())
    val uiState = RawNotificationsUiState(
        items = rawEvents.map { it.toListItem() },
    )

    RawNotificationsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawNotificationsScreen(
    uiState: RawNotificationsUiState,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Raw Notifications")
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(text = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (uiState.items.isEmpty()) {
            EmptyRawNotificationsState(
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            RawNotificationsList(
                items = uiState.items,
                contentPadding = innerPadding,
            )
        }
    }
}

@Composable
private fun EmptyRawNotificationsState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "No supported raw notifications yet.",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Only allowlisted packages are captured. Grant notification access, then trigger a Mir Pay, Alfa-Bank, or Sber notification to verify ingestion.",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun RawNotificationsList(
    items: List<RawNotificationListItem>,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = items,
            key = { item -> item.id },
        ) { item ->
            RawNotificationCard(item = item)
        }
    }
}

@Composable
private fun RawNotificationCard(
    item: RawNotificationListItem,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = item.sourcePackage,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = item.postedAt,
                style = MaterialTheme.typography.bodySmall,
            )
            RawNotificationLine(label = "Title", value = item.title)
            RawNotificationLine(label = "Text", value = item.text)
            RawNotificationLine(label = "Subtext", value = item.subText)
            RawNotificationLine(label = "Big text", value = item.bigText)
            Text(
                text = "Payload hash: ${item.payloadHash}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun RawNotificationLine(
    label: String,
    value: String?,
) {
    if (value.isNullOrBlank()) {
        return
    }

    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.bodyMedium,
    )
}
