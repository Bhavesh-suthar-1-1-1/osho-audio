package com.app.oshoaudio.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Glass effect level, 0-100, chosen by the user in Settings.
 * 0 = solid cards, faint glow (easiest to read).
 * 100 = very translucent cards, full glow (strongest glass look).
 * Default 25 matches the readable v3 look.
 */
object GlassSettings {
    var pct by mutableStateOf(25)

    fun load(ctx: Context) {
        pct = ctx.getSharedPreferences("osho_settings", Context.MODE_PRIVATE)
            .getInt("glass_pct", 25)
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("osho_settings", Context.MODE_PRIVATE)
            .edit().putInt("glass_pct", pct).apply()
    }

    /** Card opacity: 1.0 at 0%, 0.55 at 100%. */
    val cardAlpha: Float get() = 1.0f - 0.45f * (pct / 100f)

    /** Glow strength: 0.30x at 0%, 1.0x at 100%. */
    val blobScale: Float get() = 0.3f + 0.7f * (pct / 100f)
}

object ThemeSettings {
    var mode by mutableStateOf("system") // system, dark, light

    fun load(ctx: Context) {
        mode = ctx.getSharedPreferences("osho_settings", Context.MODE_PRIVATE)
            .getString("theme_mode", "system") ?: "system"
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("osho_settings", Context.MODE_PRIVATE)
            .edit().putString("theme_mode", mode).apply()
    }
}

object DownloadSettings {
    var wifiOnly by mutableStateOf(false)

    fun load(ctx: Context) {
        wifiOnly = ctx.getSharedPreferences("osho_settings", Context.MODE_PRIVATE)
            .getBoolean("wifi_only", false)
    }

    fun save(ctx: Context) {
        ctx.getSharedPreferences("osho_settings", Context.MODE_PRIVATE)
            .edit().putBoolean("wifi_only", wifiOnly).apply()
    }
}
