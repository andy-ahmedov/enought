package com.andyahmedov.enought.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andyahmedov.enought.ui.permissions.notificationAccessDisclosure

sealed interface OnboardingUiState {
    data object CheckingAccess : OnboardingUiState

    data object PermissionRequired : OnboardingUiState

    data object PermissionStillRequired : OnboardingUiState
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onOpenNotificationAccessSettings: () -> Unit,
) {
    val disclosure = notificationAccessDisclosure()

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "See today's phone spend without opening your bank app",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = disclosure.supportedSourcesText,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    DisclosureSection(
                        title = "What stays private",
                        body = disclosure.localProcessingText,
                    )
                    DisclosureSection(
                        title = "What this app does not do",
                        body = disclosure.limitationsText,
                    )

                    when (uiState) {
                        OnboardingUiState.CheckingAccess -> {
                            CircularProgressIndicator()
                            Text(
                                text = "Checking notification access…",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        OnboardingUiState.PermissionRequired,
                        OnboardingUiState.PermissionStillRequired,
                        -> {
                            if (uiState == OnboardingUiState.PermissionStillRequired) {
                                Text(
                                    text = "Notification access is still off.",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onOpenNotificationAccessSettings,
                            ) {
                                Text(text = "Open Notification Access")
                            }
                            Text(
                                text = when (uiState) {
                                    OnboardingUiState.PermissionRequired -> disclosure.firstAttemptSettingsHint
                                    OnboardingUiState.PermissionStillRequired -> disclosure.retrySettingsHint
                                    OnboardingUiState.CheckingAccess -> error("Unexpected state")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclosureSection(
    title: String,
    body: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
