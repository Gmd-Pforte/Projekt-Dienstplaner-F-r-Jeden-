package de.psaimusic.dienstplaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import de.psaimusic.dienstplaner.ui.DienstplanApp
import de.psaimusic.dienstplaner.ui.theme.DienstplanerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGlobals.init(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            DienstplanerTheme {
                DienstplanApp()
            }
        }
    }
}
