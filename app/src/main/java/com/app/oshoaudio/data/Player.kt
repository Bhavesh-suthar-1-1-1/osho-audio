package com.app.oshoaudio.data

import android.app.DownloadManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

object PlayerState {
    var seriesId by mutableStateOf<String?>(null)
    var seriesTitle by mutableStateOf("")
    var trackIndex by mutableStateOf(-1)
    var trackTitle by mutableStateOf("")
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var positionMs by mutableStateOf(0)
    var durationMs by mutableStateOf(0)
    var error by mutableStateOf<String?>(null)
    var sleepTimerEndMs by mutableStateOf(0L)
    var sleepTimerMinutes by mutableStateOf(0)
    var repeatOne by mutableStateOf(false)

    val hasTrack: Boolean get() = seriesId != null && trackIndex >= 0
}

object PlayerController {
    fun play(ctx: Context, seriesId: String, index: Int, seekMs: Int = 0) {
        val i = Intent(ctx, PlayerService::class.java).apply {
            action = PlayerService.ACTION_PLAY
            putExtra("seriesId", seriesId)
            putExtra("index", index)
            putExtra("seekMs", seekMs)
        }
        ctx.startForegroundService(i)
    }

    fun toggle(ctx: Context) {
        ctx.startService(
            Intent(ctx, PlayerService::class.java).apply { action = PlayerService.ACTION_TOGGLE }
        )
    }

    fun next(ctx: Context) {
        ctx.startService(
            Intent(ctx, PlayerService::class.java).apply { action = PlayerService.ACTION_NEXT }
        )
    }

    fun prev(ctx: Context) {
        ctx.startService(
            Intent(ctx, PlayerService::class.java).apply { action = PlayerService.ACTION_PREV }
        )
    }

    fun seekTo(ctx: Context, ms: Int) {
        ctx.startService(
            Intent(ctx, PlayerService::class.java).apply {
                action = PlayerService.ACTION_SEEK
                putExtra("ms", ms)
            }
        )
    }

    fun sleepTimer(ctx: Context, minutes: Int) {
        ctx.startService(
            Intent(ctx, PlayerService::class.java).apply {
                action = PlayerService.ACTION_SLEEP
                putExtra("minutes", minutes)
            }
        )
    }

    fun repeatToggle(ctx: Context) {
        ctx.startService(
            Intent(ctx, PlayerService::class.java).apply { action = PlayerService.ACTION_REPEAT }
        )
    }
}

object LastPlayed {
    private const val PREF = "osho_last"
    fun save(ctx: Context, seriesId: String, index: Int, position: Int) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
            .putString("seriesId", seriesId)
            .putInt("index", index)
            .putInt("position", position)
            .apply()
    }

    fun read(ctx: Context): Triple<String, Int, Int>? {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val id = p.getString("seriesId", null) ?: return null
        return Triple(id, p.getInt("index", 0), p.getInt("position", 0))
    }
}

object DownloadHelper {
    fun dir(ctx: Context, series: Series): File {
        val safe = series.title.replace(Regex("[^A-Za-z0-9 _.-]"), "_").take(80)
        return File(ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Osho/$safe")
    }

    fun localFile(ctx: Context, series: Series, track: Track): File =
        File(dir(ctx, series), track.fileName)

    /**
     * True only when the file is complete. DownloadManager creates the file as
     * soon as a download starts, so "exists" alone would count a half-finished
     * (or failed) download as saved. The catalog knows each track's real size.
     */
    fun isDownloaded(ctx: Context, series: Series, track: Track): Boolean {
        val f = localFile(ctx, series, track)
        return f.exists() && (track.size <= 0 || f.length() >= track.size)
    }

    /** True while DownloadManager is still working on this destination file. */
    private fun isQueued(ctx: Context, dest: File): Boolean {
        val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val q = DownloadManager.Query().setFilterByStatus(
            DownloadManager.STATUS_PENDING or DownloadManager.STATUS_RUNNING or
                DownloadManager.STATUS_PAUSED
        )
        dm.query(q)?.use { c ->
            val col = c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            if (col < 0) return false
            while (c.moveToNext()) {
                val u = c.getString(col) ?: continue
                if (Uri.parse(u).path == dest.path) return true
            }
        }
        return false
    }

    fun isWifi(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE)
            as android.net.ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
    }

    /** Returns false when Wi-Fi-only is on and the phone is not on Wi-Fi. */
    fun download(ctx: Context, series: Series, track: Track): Boolean {
        if (DownloadSettings.wifiOnly && !isWifi(ctx)) return false
        val dest = localFile(ctx, series, track)
        if (dest.exists()) {
            if (isDownloaded(ctx, series, track)) return true
            if (isQueued(ctx, dest)) return true
            dest.delete() // leftover from a failed or cancelled download
        }
        dest.parentFile?.mkdirs()
        val req = DownloadManager.Request(Uri.parse(track.url))
            .setTitle(track.title)
            .setDescription(series.title)
            .setDestinationUri(Uri.fromFile(dest))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
        return true
    }

    /** Total size and count of all downloaded tracks in the app's folder. */
    fun downloadedStats(ctx: Context): Pair<Int, Long> {
        val root = File(ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Osho")
        var count = 0
        var bytes = 0L
        root.walkTopDown().forEach {
            if (it.isFile) {
                count++
                bytes += it.length()
            }
        }
        return count to bytes
    }

    fun deleteAllDownloads(ctx: Context) {
        val root = File(ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Osho")
        root.deleteRecursively()
    }
}

class PlayerService : Service() {
    companion object {
        const val ACTION_PLAY = "play"
        const val ACTION_TOGGLE = "toggle"
        const val ACTION_NEXT = "next"
        const val ACTION_PREV = "prev"
        const val ACTION_SEEK = "seek"
        const val ACTION_STOP = "stop"
        const val ACTION_SLEEP = "sleep"
        const val ACTION_REPEAT = "repeat"
        const val CHANNEL = "osho_playback"
        const val NOTIF_ID = 1
    }

    private var player: MediaPlayer? = null
    private var series: Series? = null
    private var index = -1
    private var pendingSeek = 0
    private var generation = 0
    private var retryCount = 0
    private var sleepRunnable: Runnable? = null
    private var mediaSession: MediaSession? = null

    /**
     * Bluetooth buttons and lock-screen controls talk to a MediaSession, not
     * to the service directly. Without this, earbud taps, remove-to-pause and
     * the lock-screen player all silently do nothing — which is exactly the
     * reported bug. The buds send standard commands (play/pause, next,
     * previous); the session translates them into the existing actions.
     */
    private val sessionCallback = object : MediaSession.Callback() {
        override fun onPlay() {
            val p = player
            if (p != null) {
                try {
                    requestFocus()
                    p.start()
                    PlayerState.error = null
                    PlayerState.isPlaying = true
                    updateNotif()
                } catch (e: Exception) {
                }
            } else if (PlayerState.hasTrack) {
                // Player gone but a track is loaded (e.g. after an error):
                // resume it from the saved position instead of doing nothing.
                series?.let { startTrack(it, index, PlayerState.positionMs) }
            }
        }

        override fun onPause() {
            resumeOnFocusGain = false
            pausePlayback()
        }

        override fun onSkipToNext() {
            series?.let { if (index + 1 < it.tracks.size) startTrack(it, index + 1) }
        }

        override fun onSkipToPrevious() {
            series?.let { if (index - 1 >= 0) startTrack(it, index - 1) }
        }

        override fun onStop() {
            stopPlayback()
            stopSelf()
        }
    }

    /** Buds disconnecting (or both buds back in the case) must pause, not
     *  keep playing out loud on the phone speaker. */
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                if (PlayerState.isPlaying) {
                    resumeOnFocusGain = false
                    pausePlayback()
                }
            }
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() {
            player?.let {
                try {
                    if (it.isPlaying) {
                        PlayerState.positionMs = it.currentPosition
                        PlayerState.durationMs = it.duration
                    }
                } catch (e: Exception) {
                    // player released mid-poll; ignore
                }
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL, "Osho playback", NotificationManager.IMPORTANCE_LOW)
            mgr.createNotificationChannel(ch)
        }
        mediaSession = MediaSession(this, "OshoAudio").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setCallback(sessionCallback)
        }
        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        handler.post(poll)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                // Started via startForegroundService(): must go foreground right
                // away, even if the request below turns out to be invalid.
                try {
                    startForeground(NOTIF_ID, buildNotif())
                } catch (e: Exception) {
                }
                val id = intent.getStringExtra("seriesId")
                val idx = intent.getIntExtra("index", 0)
                val s = id?.let { CatalogStore.seriesById(it) }
                if (s != null && idx in s.tracks.indices) {
                    startTrack(s, idx, intent.getIntExtra("seekMs", 0))
                } else if (player == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            ACTION_TOGGLE -> toggle()
            ACTION_NEXT -> series?.let { if (index + 1 < it.tracks.size) startTrack(it, index + 1) }
            ACTION_PREV -> series?.let { if (index - 1 >= 0) startTrack(it, index - 1) }
            ACTION_SEEK -> {
                try {
                    player?.seekTo(intent.getIntExtra("ms", 0))
                } catch (e: Exception) {
                }
            }
            ACTION_STOP -> {
                stopPlayback()
                stopSelf()
            }
            ACTION_SLEEP -> {
                setSleepTimer(intent.getIntExtra("minutes", 0))
            }
            ACTION_REPEAT -> {
                PlayerState.repeatOne = !PlayerState.repeatOne
                updateNotif()
            }
        }
        return START_NOT_STICKY
    }

    private fun startTrack(s: Series, idx: Int, seekMs: Int = 0, resetRetry: Boolean = true) {
        generation++
        val myGen = generation
        if (resetRetry) retryCount = 0
        pendingSeek = seekMs
        series = s
        index = idx
        val track = s.tracks[idx]
        PlayerState.seriesId = s.id
        PlayerState.seriesTitle = s.title
        PlayerState.trackIndex = idx
        PlayerState.trackTitle = track.title
        PlayerState.isBuffering = true
        PlayerState.error = null
        PlayerState.positionMs = 0
        PlayerState.durationMs = 0
        LastPlayed.save(this, s.id, idx, 0)
        val local = DownloadHelper.localFile(this, s, track)
        val source = if (DownloadHelper.isDownloaded(this, s, track)) local.absolutePath else track.url
        // startForegroundService() obliges us to call startForeground() within a
        // few seconds, so do it before anything below can throw.
        try {
            startForeground(NOTIF_ID, buildNotif())
        } catch (e: Exception) {
        }
        try {
            player?.release()
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(source)
                setOnPreparedListener {
                    if (myGen != generation) return@setOnPreparedListener
                    requestFocus()
                    it.start()
                    PlayerState.error = null
                    if (pendingSeek > 0) {
                        try {
                            it.seekTo(pendingSeek)
                        } catch (e: Exception) {
                        }
                        pendingSeek = 0
                    }
                    PlayerState.isPlaying = true
                    PlayerState.isBuffering = false
                    PlayerState.durationMs = it.duration
                    updateNotif()
                }
                setOnCompletionListener {
                    if (myGen != generation) return@setOnCompletionListener
                    val cur = series
                    if (cur != null && PlayerState.repeatOne) {
                        startTrack(cur, index)
                    } else if (cur != null && index + 1 < cur.tracks.size) {
                        startTrack(cur, index + 1)
                    } else {
                        stopPlayback()
                    }
                }
                setOnErrorListener { _, _, _ ->
                    if (myGen != generation) return@setOnErrorListener true
                    if (retryCount < 2) {
                        // Transient hitch (redirect/stall): retry quietly from
                        // the same spot instead of flashing a false failure.
                        retryCount++
                        val resumeAt = PlayerState.positionMs
                        PlayerState.isBuffering = true
                        PlayerState.error = null
                        handler.postDelayed({
                            val cur = series
                            if (cur != null && myGen == generation) {
                                startTrack(cur, index, resumeAt, false)
                            }
                        }, 1200)
                    } else {
                        PlayerState.error = "Could not play this track"
                        PlayerState.isPlaying = false
                        PlayerState.isBuffering = false
                        updateNotif()
                    }
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            PlayerState.error = "Could not play: ${e.message}"
            PlayerState.isBuffering = false
            PlayerState.isPlaying = false
            updateNotif()
        }
    }

    private fun toggle() {
        val p = player ?: return
        try {
            if (p.isPlaying) {
                resumeOnFocusGain = false
                p.pause()
                PlayerState.isPlaying = false
                series?.let { LastPlayed.save(this, it.id, index, p.currentPosition) }
            } else {
                p.start()
                PlayerState.error = null
                PlayerState.isPlaying = true
            }
            updateNotif()
        } catch (e: Exception) {
        }
    }

    private var audioManager: AudioManager? = null
    private var focusRequest: android.media.AudioFocusRequest? = null
    private var resumeOnFocusGain = false

    /**
     * Audio focus: when another app plays audio (a reel, a video, another
     * music app) or records (a voice note), Android tells the current focus
     * holder. We pause on transient loss and resume on gain — but only if
     * the pause was ours, never over a manual pause by the user.
     */
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                player?.setVolume(1f, 1f)
                if (resumeOnFocusGain && PlayerState.hasTrack) {
                    resumeOnFocusGain = false
                    player?.start()
                    PlayerState.isPlaying = true
                    updateNotif()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                pausePlayback()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (PlayerState.isPlaying) {
                    resumeOnFocusGain = true
                    pausePlayback()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                player?.setVolume(0.2f, 0.2f)
            }
        }
    }

    private fun requestFocus() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = am
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val req = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        focusRequest = req
        am.requestAudioFocus(req)
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        focusRequest = null
    }

    private fun pausePlayback() {
        player?.pause()
        PlayerState.isPlaying = false
        updateNotif()
    }

    private fun setSleepTimer(minutes: Int) {
        sleepRunnable?.let { handler.removeCallbacks(it) }
        sleepRunnable = null
        if (minutes <= 0) {
            PlayerState.sleepTimerEndMs = 0
            PlayerState.sleepTimerMinutes = 0
            return
        }
        PlayerState.sleepTimerMinutes = minutes
        PlayerState.sleepTimerEndMs = System.currentTimeMillis() + minutes * 60_000L
        val r = Runnable {
            PlayerState.sleepTimerEndMs = 0
            PlayerState.sleepTimerMinutes = 0
            stopPlayback()
        }
        sleepRunnable = r
        handler.postDelayed(r, minutes * 60_000L)
    }

    private fun stopPlayback() {
        try {
            player?.stop()
        } catch (e: Exception) {
        }
        player?.release()
        player = null
        PlayerState.isPlaying = false
        PlayerState.sleepTimerEndMs = 0
        PlayerState.sleepTimerMinutes = 0
        resumeOnFocusGain = false
        mediaSession?.isActive = false
        abandonFocus()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun actionIntent(action: String): PendingIntent {
        val i = Intent(this, PlayerService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            this, action.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotif(): Notification {
        val title = if (PlayerState.trackTitle.isNotEmpty()) PlayerState.trackTitle else "Osho Audio"
        val builder = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(PlayerState.seriesTitle)
            .setOngoing(PlayerState.isPlaying)
            .addAction(android.R.drawable.ic_media_previous, "Prev", actionIntent(ACTION_PREV))
            .addAction(
                if (PlayerState.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (PlayerState.isPlaying) "Pause" else "Play",
                actionIntent(ACTION_TOGGLE)
            )
            .addAction(android.R.drawable.ic_media_next, "Next", actionIntent(ACTION_NEXT))
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
        return builder.build()
    }

    private fun updateNotif() {
        try {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.notify(NOTIF_ID, buildNotif())
        } catch (e: Exception) {
        }
        updateSessionState()
    }

    /**
     * Keeps the lock-screen player and Bluetooth buttons in sync: track info,
     * play/pause state and position. The session stays active while a player
     * exists (playing or paused) so buds can resume and the lock screen
     * keeps showing controls; it goes inactive once playback is stopped.
     */
    private fun updateSessionState() {
        val session = mediaSession ?: return
        try {
            if (PlayerState.hasTrack) {
                session.setMetadata(
                    MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, PlayerState.trackTitle)
                        .putString(MediaMetadata.METADATA_KEY_ALBUM, PlayerState.seriesTitle)
                        .putString(MediaMetadata.METADATA_KEY_ARTIST, "Osho")
                        .putLong(MediaMetadata.METADATA_KEY_DURATION, PlayerState.durationMs.toLong())
                        .build()
                )
            }
            val pos = try {
                player?.currentPosition?.toLong()
            } catch (e: Exception) {
                null
            } ?: PlayerState.positionMs.toLong()
            session.setPlaybackState(
                PlaybackState.Builder()
                    .setState(
                        if (PlayerState.isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                        pos, 1f
                    )
                    .setActions(
                        PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_STOP
                    )
                    .build()
            )
            session.isActive = player != null
        } catch (e: Exception) {
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        sleepRunnable?.let { handler.removeCallbacks(it) }
        abandonFocus()
        try {
            unregisterReceiver(noisyReceiver)
        } catch (e: Exception) {
        }
        mediaSession?.release()
        mediaSession = null
        try {
            player?.release()
        } catch (e: Exception) {
        }
        player = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
