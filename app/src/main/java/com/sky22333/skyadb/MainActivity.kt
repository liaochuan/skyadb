package com.sky22333.skyadb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sky22333.skyadb.data.ThemeMode
import com.sky22333.skyadb.ui.AdbManagerApp
import com.sky22333.skyadb.ui.theme.AdbManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by AppServices.settingsStore.settings.collectAsState(
                initial = com.sky22333.skyadb.data.AppSettings(),
            )
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.System -> systemInDarkTheme
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            AdbManagerTheme(darkTheme = darkTheme) {
                AdbManagerApp()
            }
        }
    }
}
