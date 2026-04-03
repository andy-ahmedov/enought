package com.andyahmedov.enought.ui.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.andyahmedov.enought.common.parseRubAmountInput
import com.andyahmedov.enought.common.toRubInputString
import com.andyahmedov.enought.domain.model.TodayReviewItem
import com.andyahmedov.enought.domain.usecase.ConfirmPaymentEventUseCase
import com.andyahmedov.enought.domain.usecase.CorrectPaymentAmountUseCase
import com.andyahmedov.enought.domain.usecase.DismissPaymentEventUseCase
import com.andyahmedov.enought.domain.usecase.KeepDuplicateConflictSeparateUseCase
import com.andyahmedov.enought.domain.usecase.MergeDuplicateConflictUseCase
import com.andyahmedov.enought.domain.usecase.ObserveTodayReviewItemsUseCase
import kotlinx.coroutines.launch

@Composable
fun ReviewRoute(
    observeTodayReviewItemsUseCase: ObserveTodayReviewItemsUseCase,
    confirmPaymentEventUseCase: ConfirmPaymentEventUseCase,
    correctPaymentAmountUseCase: CorrectPaymentAmountUseCase,
    dismissPaymentEventUseCase: DismissPaymentEventUseCase,
    mergeDuplicateConflictUseCase: MergeDuplicateConflictUseCase,
    keepDuplicateConflictSeparateUseCase: KeepDuplicateConflictSeparateUseCase,
    onNavigateBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val reviewItemsFlow = remember(observeTodayReviewItemsUseCase) {
        observeTodayReviewItemsUseCase()
    }
    val reviewItems by reviewItemsFlow.collectAsState(initial = null as List<TodayReviewItem>?)
    val uiState = remember(reviewItems) {
        ReviewUiStateFactory.create(reviewItems)
    }
    var pendingItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var amountInput by rememberSaveable { mutableStateOf("") }
    val editingItem = (uiState as? ReviewUiState.Ready)
        ?.items
        ?.filterIsInstance<ReviewListItem.Single>()
        ?.firstOrNull { item -> item.id == editingItemId }

    ReviewScreen(
        uiState = uiState,
        pendingItemId = pendingItemId,
        editingItem = editingItem,
        amountInput = amountInput,
        onNavigateBack = onNavigateBack,
        onConfirm = { eventId ->
            pendingItemId = eventId
            scope.launch {
                confirmPaymentEventUseCase(eventId)
                pendingItemId = null
            }
        },
        onOpenCorrectAmount = { item ->
            editingItemId = item.id
            amountInput = item.amountMinor.toRubInputString()
        },
        onAmountInputChange = { newValue ->
            amountInput = newValue
        },
        onDismiss = { eventId ->
            pendingItemId = eventId
            scope.launch {
                dismissPaymentEventUseCase(eventId)
                pendingItemId = null
            }
        },
        onMergeDuplicateConflict = { duplicateGroupId ->
            pendingItemId = duplicateGroupId
            scope.launch {
                mergeDuplicateConflictUseCase(duplicateGroupId)
                pendingItemId = null
            }
        },
        onKeepDuplicateConflictSeparate = { duplicateGroupId ->
            pendingItemId = duplicateGroupId
            scope.launch {
                keepDuplicateConflictSeparateUseCase(duplicateGroupId)
                pendingItemId = null
            }
        },
        onCancelAmountEdit = {
            editingItemId = null
            amountInput = ""
        },
        onSubmitCorrectAmount = {
            val eventId = editingItemId ?: return@ReviewScreen
            val correctedAmountMinor = parseRubAmountInput(amountInput) ?: return@ReviewScreen

            pendingItemId = eventId
            scope.launch {
                correctPaymentAmountUseCase(
                    eventId = eventId,
                    correctedAmountMinor = correctedAmountMinor,
                )
                pendingItemId = null
                editingItemId = null
                amountInput = ""
            }
        },
    )
}
