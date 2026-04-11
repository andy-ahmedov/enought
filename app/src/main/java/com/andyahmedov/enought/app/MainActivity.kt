package com.andyahmedov.enought.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EnoughtApp()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            applicationContext.appContainer.enforceDataRetentionUseCase()
            applicationContext.appContainer.widgetUpdater.refresh()
        }
    }
}
