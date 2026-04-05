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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andyahmedov.enought.common.DiagnosticsLogClipboardWriter
import com.andyahmedov.enought.common.DiagnosticsLogShareLauncher
import com.andyahmedov.enought.domain.repository.RawNotificationEventRepository
import com.andyahmedov.enought.domain.usecase.BuildDiagnosticsReportUseCase
import kotlinx.coroutines.launch

@Composable
fun RawNotificationsRoute(
    rawNotificationEventRepository: RawNotificationEventRepository,
    buildDiagnosticsReportUseCase: BuildDiagnosticsReportUseCase,
    diagnosticsLogClipboardWriter: DiagnosticsLogClipboardWriter,
    diagnosticsLogShareLauncher: DiagnosticsLogShareLauncher,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val (statusMessage, setStatusMessage) = remember {
        mutableStateOf<String?>(null)
    }
    val rawEvents by rawNotificationEventRepository.observeRawEvents().collectAsState(initial = emptyList())
    val uiState = rawNotificationsUiState(
        rawEvents = rawEvents,
        statusMessage = statusMessage,
    )

    RawNotificationsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onCopyDiagnosticLog = {
            coroutineScope.launch {
                setStatusMessage(
                    runCatching {
                        buildDiagnosticsReportUseCase()
                    }.fold(
                        onSuccess = { reportText ->
                            diagnosticsLogClipboardWriter.copy(
                                context = context,
                                reportText = reportText,
                            )
                            "Diagnostic log copied. It includes all captured notifications for today."
                        },
                        onFailure = {
                            "Failed to build diagnostic log."
                        },
                    ),
                )
            }
        },
        onShareDiagnosticLog = {
            coroutineScope.launch {
                setStatusMessage(
                    runCatching {
                        buildDiagnosticsReportUseCase()
                    }.fold(
                        onSuccess = { reportText ->
                            diagnosticsLogShareLauncher.share(
                                context = context,
                                reportText = reportText,
                            )
                            "Diagnostic log opened in the system share sheet."
                        },
                        onFailure = {
                            "Failed to build diagnostic log."
                        },
                    ),
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawNotificationsScreen(
    uiState: RawNotificationsUiState,
    onNavigateBack: () -> Unit,
    onCopyDiagnosticLog: () -> Unit,
    onShareDiagnosticLog: () -> Unit,
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
        RawNotificationsContent(
            uiState = uiState,
            contentPadding = innerPadding,
            onCopyDiagnosticLog = onCopyDiagnosticLog,
            onShareDiagnosticLog = onShareDiagnosticLog,
        )
    }
}

@Composable
private fun RawNotificationsContent(
    uiState: RawNotificationsUiState,
    contentPadding: PaddingValues,
    onCopyDiagnosticLog: () -> Unit,
    onShareDiagnosticLog: () -> Unit,
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
        item {
            DiagnosticsExportCard(
                statusMessage = uiState.statusMessage,
                onCopyDiagnosticLog = onCopyDiagnosticLog,
                onShareDiagnosticLog = onShareDiagnosticLog,
            )
        }

        if (uiState.items.isEmpty()) {
            item {
                EmptyRawNotificationsState()
            }
        } else {
            items(
                items = uiState.items,
                key = { item -> item.id },
            ) { item ->
                RawNotificationCard(item = item)
            }
        }
    }
}

@Composable
private fun DiagnosticsExportCard(
    statusMessage: String?,
    onCopyDiagnosticLog: () -> Unit,
    onShareDiagnosticLog: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Diagnostic log",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Copy or share a plain-text report for today. It includes all captured notifications for the day and their current payment-event snapshot, even though this screen shows Mir Pay items only.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "The report may contain financial notification text. Share it intentionally.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCopyDiagnosticLog,
            ) {
                Text(text = "Copy Diagnostic Log")
            }
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onShareDiagnosticLog,
            ) {
                Text(text = "Share Diagnostic Log")
            }
            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyRawNotificationsState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "No Mir Pay raw notifications yet.",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "This screen only shows Mir Pay notifications. Grant notification access, then trigger a Mir Pay payment to verify ingestion.",
                style = MaterialTheme.typography.bodyLarge,
            )
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
