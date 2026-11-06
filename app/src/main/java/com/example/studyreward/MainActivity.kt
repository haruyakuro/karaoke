
package com.example.studyreward

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.studyreward.ui.navigation.AppNavHost
import com.example.studyreward.ui.theme.StudyRewardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StudyRewardTheme {
                val context = LocalContext.current

                val permissionLauncher =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        // 許可・拒否の結果を受け取る
                    }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= 33) {
                        val permission =
                            Manifest.permission.POST_NOTIFICATIONS

                        val granted =
                            ContextCompat.checkSelfPermission(
                                context,
                                permission
                            ) == PackageManager.PERMISSION_GRANTED

                        if (!granted) {
                            permissionLauncher.launch(permission)
                        }
                    }
                }

                AppNavHost()
            }
        }
    }
}
