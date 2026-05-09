package com.aykutcincik.mimir

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aykutcincik.mimir.ui.MimirApp
import com.aykutcincik.mimir.ui.theme.MimirTheme
import com.aykutcincik.mimir.ui.theme.ThemeMode
import com.aykutcincik.mimir.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ctx = LocalContext.current
            val themePref = remember { ThemePreference(ctx) }
            val mode by themePref.mode.collectAsState(initial = ThemeMode.System)

            MimirTheme(themeMode = mode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MimirApp()
                }
            }
        }
    }
}
