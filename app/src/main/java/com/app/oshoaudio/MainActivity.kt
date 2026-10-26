package com.app.oshoaudio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import com.app.oshoaudio.data.CatalogStore
import com.app.oshoaudio.data.DownloadSettings
import com.app.oshoaudio.data.GlassSettings
import com.app.oshoaudio.data.ThemeSettings
import com.app.oshoaudio.ui.OshoApp
import com.app.oshoaudio.ui.OshoTheme

class MainActivity : ComponentActivity() {
    private val notifPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CatalogStore.load(this)
        GlassSettings.load(this)
        ThemeSettings.load(this)
        DownloadSettings.load(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent {
            val systemDark = isSystemInDarkTheme()
            val dark = when (ThemeSettings.mode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            OshoTheme(dark = dark) {
                OshoApp()
            }
        }
    }
}
