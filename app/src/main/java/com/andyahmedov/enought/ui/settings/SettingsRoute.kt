package com.andyahmedov.enought.ui.settings

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
import com.andyahmedov.enought.domain.usecase.ClearDailyLimitUseCase
import com.andyahmedov.enought.domain.usecase.ObserveDailyLimitUseCase
import com.andyahmedov.enought.domain.usecase.SetDailyLimitUseCase
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    observeDailyLimitUseCase: ObserveDailyLimitUseCase,
    setDailyLimitUseCase: SetDailyLimitUseCase,
    clearDailyLimitUseCase: ClearDailyLimitUseCase,
    onNavigateBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dailyLimitFlow = remember(observeDailyLimitUseCase) {
        observeDailyLimitUseCase()
    }
    val currentLimitAmountMinor by dailyLimitFlow.collectAsState(initial = null as Long?)
    var amountInput by rememberSaveable(currentLimitAmountMinor) {
        mutableStateOf(currentLimitAmountMinor?.toRubInputString().orEmpty())
    }
    var isPending by rememberSaveable { mutableStateOf(false) }
    val parsedAmountMinor = parseRubAmountInput(amountInput)

    SettingsScreen(
        currentLimitAmountMinor = currentLimitAmountMinor,
        amountInput = amountInput,
        isSaveEnabled = !isPending && parsedAmountMinor != null && parsedAmountMinor != currentLimitAmountMinor,
        isClearEnabled = !isPending && currentLimitAmountMinor != null,
        onNavigateBack = onNavigateBack,
        onAmountInputChange = { newValue ->
            amountInput = newValue
        },
        onSave = {
            val amountMinor = parsedAmountMinor ?: return@SettingsScreen
            isPending = true
            scope.launch {
                setDailyLimitUseCase(amountMinor)
                amountInput = amountMinor.toRubInputString()
                isPending = false
            }
        },
        onClear = {
            isPending = true
            scope.launch {
                clearDailyLimitUseCase()
                amountInput = ""
                isPending = false
            }
        },
    )
}
