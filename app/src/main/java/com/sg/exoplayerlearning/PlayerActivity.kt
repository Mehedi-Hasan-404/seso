package com.sg.exoplayerlearning

import android.app.AppOpsManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.media3.common.util.UnstableApi
import com.sg.exoplayerlearning.ui.screens.PlayerRoute
import com.sg.exoplayerlearning.ui.theme.ExoPlayerLearningTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private var playerRect: Rect? = null
    private val canDeviceSupportPipMode: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExoPlayerLearningTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PlayerRoute(
                        modifier = Modifier.padding(innerPadding),
                    ) { androidRect ->
                        playerRect = androidRect
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (canDeviceSupportPipMode.not()) return

        if (isPipEnabledInDevice().not()) return

        getPipParams()?.let {
            enterPictureInPictureMode(it)
        }
    }

    private fun getPipParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) return null

        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16,9))
            .setSourceRectHint(playerRect)
            .build()
    }

    fun isPipEnabledInDevice(): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) return true

        return (getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager)
            .unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                android.os.Process.myUid(),
                packageName
            ) == AppOpsManager.MODE_ALLOWED
    }
}