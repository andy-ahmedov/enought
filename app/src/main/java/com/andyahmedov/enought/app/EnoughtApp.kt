package com.andyahmedov.enought.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.andyahmedov.enought.app.navigation.EnoughtNavHost

@Composable
fun EnoughtApp() {
    MaterialTheme {
        Surface {
            EnoughtNavHost()
        }
    }
}

