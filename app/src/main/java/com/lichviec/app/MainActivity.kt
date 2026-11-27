package com.lichviec.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.lichviec.app.ui.MainApp
import com.lichviec.app.ui.theme.LichViecTheme

class MainActivity : ComponentActivity() {

    private var launchTab by mutableIntStateOf(0)
    private var launchOpenAddDialog by mutableStateOf(false)
    private var launchEventCounter by mutableIntStateOf(0)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleLaunchIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            LichViecTheme {
                MainApp(
                    launchTab = launchTab,
                    launchOpenAddDialog = launchOpenAddDialog,
                    launchEventCounter = launchEventCounter
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.hasExtra(EXTRA_OPEN_TAB) || intent.hasExtra(EXTRA_OPEN_ADD_DIALOG)) {
            launchTab = intent.getIntExtra(EXTRA_OPEN_TAB, 0)
            launchOpenAddDialog = intent.getBooleanExtra(EXTRA_OPEN_ADD_DIALOG, false)
            launchEventCounter += 1
        }
    }

    companion object {
        const val EXTRA_OPEN_TAB = "extra_open_tab"
        const val EXTRA_OPEN_ADD_DIALOG = "extra_open_add_dialog"
        const val EXTRA_START_VOICE = "extra_start_voice"
    }
}
