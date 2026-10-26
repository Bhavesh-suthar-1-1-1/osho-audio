package com.app.oshoaudio.ui

import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.oshoaudio.data.CatalogStore
import com.app.oshoaudio.data.DownloadHelper
import com.app.oshoaudio.data.DownloadSettings
import com.app.oshoaudio.data.GlassSettings
import com.app.oshoaudio.data.ThemeSettings
import com.app.oshoaudio.data.LastPlayed
import com.app.oshoaudio.data.PlayerController
import com.app.oshoaudio.data.PlayerState
import com.app.oshoaudio.data.Series

sealed class Screen {
    object Home : Screen()
    data class Lang(val lang: String) : Screen()
    data class Person(val person: String) : Screen()
    object All : Screen()
    data class Detail(val seriesId: String) : Screen()
    object Player : Screen()
    object Settings : Screen()
}

fun fmtMs(ms: Int): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Warm blurred blobs behind everything: the liquid-glass backdrop. */
@Composable
fun GlassBackground() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.size(320.dp).offset(220.dp, (-120).dp)
                .clip(CircleShape)
                .background(Color(0xFFE65100).copy(alpha = 0.26f * GlassSettings.blobScale))
                .graphicsLayer {
                    renderEffect = RenderEffect.createBlurEffect(90f, 90f, Shader.TileMode.CLAMP).asComposeRenderEffect()
                }
        )
        Box(
            Modifier.size(280.dp).offset((-120).dp, 320.dp)
                .clip(CircleShape)
                .background(Color(0xFFF59E0B).copy(alpha = 0.18f * GlassSettings.blobScale))
                .graphicsLayer {
                    renderEffect = RenderEffect.createBlurEffect(90f, 90f, Shader.TileMode.CLAMP).asComposeRenderEffect()
                }
        )
        Box(
            Modifier.size(300.dp).offset(120.dp, 640.dp)
                .clip(CircleShape)
                .background(Color(0xFFB91C1C).copy(alpha = 0.15f * GlassSettings.blobScale))
                .graphicsLayer {
                    renderEffect = RenderEffect.createBlurEffect(90f, 90f, Shader.TileMode.CLAMP).asComposeRenderEffect()
                }
        )
    }
}

/** Translucent card over the blurred backdrop. */
@Composable
fun GlassCard(mod: Modifier, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = if (onClick != null) mod.clickable { onClick() } else mod,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = GlassSettings.cardAlpha)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, Color.Gray.copy(alpha = 0.25f)
        )
    ) {
        content()
    }
}

/** Gradient tile with a series/person initial, used as artwork. */
@Composable
fun GradientTile(letter: String, size: Int, fontSize: Int) {
    Box(
        Modifier.size(size.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF97316), Color(0xFFEA580C), Color(0xFFB91C1C))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter.uppercase().take(1),
            color = Color.White,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OshoApp() {
    val stack = remember { mutableStateListOf<Screen>(Screen.Home) }
    val screen = stack.last()
    val go: (Screen) -> Unit = { stack.add(it) }
    val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    // System back gesture / back button now walks the app's own screens.
    BackHandler(enabled = stack.size > 1) { stack.removeAt(stack.lastIndex) }

    Box(Modifier.fillMaxSize()) {
        GlassBackground()
        // Default text/icon color for everything NOT inside a card. Without
        // this, plain texts fall back to black and vanish on the dark theme.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                if (screen !is Screen.Player) {
                TopAppBar(
                    title = {
                        Text(
                            when (screen) {
                                is Screen.Home -> "Osho Audio"
                                is Screen.Lang -> (screen as Screen.Lang).lang
                                is Screen.Person -> (screen as Screen.Person).person
                                is Screen.All -> "All series"
                                is Screen.Detail -> "Series"
                                is Screen.Player -> "Now playing"
                                is Screen.Settings -> "Settings"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (stack.size > 1) {
                            IconButton(onClick = back) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (screen !is Screen.Settings) {
                            IconButton(onClick = { go(Screen.Settings) }) {
                                Icon(Icons.Filled.Settings, contentDescription = "Settings")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
                }
            },
            bottomBar = {
                if (PlayerState.hasTrack && screen !is Screen.Player) {
                    Box(Modifier.padding(16.dp)) {
                        MiniPlayer(onOpen = { go(Screen.Player) })
                    }
                }
            }
        ) { pad ->
            when (screen) {
                is Screen.Home -> HomeScreen(Modifier.padding(pad), go)
                is Screen.Lang -> SeriesListScreen(
                    Modifier.padding(pad),
                    list = CatalogStore.seriesForLanguage((screen as Screen.Lang).lang),
                    go = go
                )
                is Screen.Person -> SeriesListScreen(
                    Modifier.padding(pad),
                    list = CatalogStore.seriesForPerson((screen as Screen.Person).person),
                    go = go
                )
                is Screen.All -> SeriesListScreen(
                    Modifier.padding(pad),
                    list = CatalogStore.series.sortedBy { it.title.lowercase() },
                    go = go,
                    searchable = true
                )
                is Screen.Detail -> {
                    val s = CatalogStore.seriesById((screen as Screen.Detail).seriesId)
                    if (s != null) DetailScreen(Modifier.padding(pad), s, go)
                    else Text("Not found", modifier = Modifier.padding(16.dp))
                }
                is Screen.Player -> PlayerScreen(Modifier.padding(pad), onBack = back, go = go)
                is Screen.Settings -> SettingsScreen(Modifier.padding(pad))
            }
        }
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp)
    )
}

@Composable
fun SearchPill(query: String, onQuery: (String) -> Unit, placeholder: String, mod: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = GlassSettings.cardAlpha),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = GlassSettings.cardAlpha),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        modifier = mod.fillMaxWidth()
    )
}

@Composable
fun HomeScreen(mod: Modifier, go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    val results = remember(query) { CatalogStore.searchSeries(query) }
    val last = remember { LastPlayed.read(ctx) }
    val lastSeries = last?.let { CatalogStore.seriesById(it.first) }

    // The search box stays at the same spot in the tree whether or not a query
    // is typed, so it keeps focus and never disappears while typing.
    Column(mod.fillMaxSize()) {
        if (query.isEmpty()) {
            Text(
                "Osho Audio",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
            Text(
                "295 series · 5,179 talks · Hindi & English",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 16.dp)
            )
        }
        SearchPill(
            query, { query = it }, "Search series, e.g. Ashtavakra, Kabir…",
            Modifier.padding(horizontal = 16.dp, vertical = if (query.isEmpty()) 0.dp else 8.dp)
        )
        if (query.isNotEmpty()) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(results) { s -> SeriesRow(s) { go(Screen.Detail(s.id)) } }
            }
        } else Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
        ) {
        if (lastSeries != null && last != null) {
            SectionTitle("Continue listening")
            GlassCard(
                Modifier.fillMaxWidth(),
                onClick = {
                    PlayerController.play(ctx, lastSeries.id, last.second, last.third)
                    go(Screen.Player)
                }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    GradientTile(lastSeries.title, 48, 20)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(lastSeries.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            lastSeries.tracks.getOrNull(last.second)?.title ?: "",
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        SectionTitle("Languages")
        for ((lang, nSeries, nTracks) in CatalogStore.languageStats()) {
            GlassCard(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                onClick = { go(Screen.Lang(lang)) }
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientTile(lang, 44, 18)
                        Text(
                            lang,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    Text("$nSeries series · $nTracks talks")
                }
            }
        }

        SectionTitle("Persons Osho spoke about")
        for ((person, tracks) in CatalogStore.persons()) {
            val n = CatalogStore.seriesForPerson(person).size
            GlassCard(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                onClick = { go(Screen.Person(person)) }
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GradientTile(person, 44, 18)
                        Text(
                            person,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                    Text("$n series · $tracks talks")
                }
            }
        }

        SectionTitle("Browse")
        GlassCard(
            Modifier.fillMaxWidth().padding(bottom = 24.dp),
            onClick = { go(Screen.All) }
        ) {
            Text(
                "All series A–Z",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }
        }
    }
}

@Composable
fun SeriesRow(s: Series, onClick: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GradientTile(s.title, 44, 18)
            Column(Modifier.padding(start = 12.dp)) {
                Text(s.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${s.language} · ${s.count} talks" + (s.person?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SeriesListScreen(
    mod: Modifier,
    list: List<Series>,
    go: (Screen) -> Unit,
    searchable: Boolean = false
) {
    var query by remember { mutableStateOf("") }
    val shown = remember(query, list) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) list else list.filter { it.title.lowercase().contains(q) }
    }
    Column(mod.fillMaxSize()) {
        if (searchable) {
            SearchPill(
                query, { query = it }, "Filter series…",
                Modifier.padding(16.dp)
            )
        }
        Text(
            "${shown.size} series",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown) { s -> SeriesRow(s) { go(Screen.Detail(s.id)) } }
        }
    }
}

@Composable
fun DetailScreen(mod: Modifier, s: Series, go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    var dlTick by remember { mutableStateOf(0) }
    Column(mod.fillMaxSize()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GradientTile(s.title, 72, 28)
            Column(Modifier.padding(start = 16.dp)) {
                Text(s.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${s.language} · ${s.count} talks" + (s.person?.let { " · About $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Text(
            "Tap a talk to play. Use the download icon to save it offline in the app.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Button(
            onClick = {
                if (DownloadSettings.wifiOnly && !DownloadHelper.isWifi(ctx)) {
                    Toast.makeText(
                        ctx,
                        "Wi-Fi only is on. Connect to Wi-Fi to download.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    var n = 0
                    for (t in s.tracks) {
                        if (!DownloadHelper.isDownloaded(ctx, s, t) &&
                            DownloadHelper.download(ctx, s, t)
                        ) n++
                    }
                    Toast.makeText(
                        ctx,
                        if (n > 0) "Downloading $n talks"
                        else "All talks already downloaded",
                        Toast.LENGTH_SHORT
                    ).show()
                    dlTick++
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text("Download all talks")
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(s.tracks) { idx, t ->
                val downloaded = remember(dlTick) {
                    DownloadHelper.isDownloaded(ctx, s, t)
                }
                val isCurrent = PlayerState.seriesId == s.id && PlayerState.trackIndex == idx
                GlassCard(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                    onClick = {
                        PlayerController.play(ctx, s.id, idx)
                        go(Screen.Player)
                    }
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "%03d".format(idx + 1),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                t.title, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (t.sizeMb.isNotEmpty()) {
                                Text(t.sizeMb, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (isCurrent && PlayerState.isPlaying) {
                            Icon(
                                Icons.Filled.Pause, contentDescription = "Playing",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        } else if (downloaded) {
                            Icon(
                                Icons.Filled.Done, contentDescription = "Downloaded",
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            IconButton(onClick = {
                                if (!DownloadHelper.download(ctx, s, t)) {
                                    Toast.makeText(
                                        ctx,
                                        "Wi-Fi only is on. Connect to Wi-Fi to download.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                dlTick++
                            }) {
                                Icon(Icons.Filled.Download, contentDescription = "Download")
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Dark full-screen player modelled on the reference music-player shot:
 *  near-black background, huge gold typography as the artwork, big pill
 *  play/pause in the middle, round prev/next, three pills at the bottom.
 *  No scroll — the layout is fixed with weights. */
@Composable
fun PlayerScreen(mod: Modifier, onBack: () -> Unit, go: (Screen) -> Unit) {
    val ctx = LocalContext.current
    val series = PlayerState.seriesId?.let { CatalogStore.seriesById(it) }
    val idx = PlayerState.trackIndex
    val track = series?.tracks?.getOrNull(idx)
    var sliderPos by remember { mutableStateOf<Float?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    val prefs = remember { ctx.getSharedPreferences("osho_likes", Context.MODE_PRIVATE) }
    var likedSet by remember { mutableStateOf(prefs.getStringSet("liked", emptySet()) ?: emptySet()) }

    Column(
        mod.fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (series == null || track == null) {
            Text("Nothing playing yet. Pick a talk from any series.", color = Color.White)
            return
        }
        val likeKey = "${series.id}:$idx"
        val liked = likeKey in likedSet
        val downloaded = DownloadHelper.isDownloaded(ctx, series, track)

        // Header: collapse chevron, centered NOW PLAYING, three-dot menu.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Close player",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "NOW PLAYING",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    "${series.language} · ${series.tracks.size} talks",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = Color.White)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("View series") },
                        onClick = { menuOpen = false; go(Screen.Detail(series.id)) }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = { menuOpen = false; go(Screen.Settings) }
                    )
                }
            }
        }

        // Artwork: huge gold typography, person first, series title second.
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                (series.person ?: "OSHO").uppercase(),
                color = Color(0xFFC9A86A),
                fontWeight = FontWeight.Black,
                fontSize = 64.sp,
                lineHeight = 68.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                series.title.uppercase(),
                color = Color(0xFFC9A86A),
                fontWeight = FontWeight.Black,
                fontSize = 64.sp,
                lineHeight = 68.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Title row: talk + series on the left, like + download on the right.
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    series.title,
                    color = Color.Gray,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = {
                    val next = if (liked) likedSet - likeKey else likedSet + likeKey
                    prefs.edit().putStringSet("liked", next).apply()
                    likedSet = next
                },
                modifier = Modifier.size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262626))
            ) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (liked) Color(0xFFC9A86A) else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            IconButton(
                onClick = {
                    if (!downloaded && !DownloadHelper.download(ctx, series, track)) {
                        Toast.makeText(
                            ctx,
                            "Wi-Fi only is on. Connect to Wi-Fi to download.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                modifier = Modifier.size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262626))
            ) {
                Icon(
                    if (downloaded) Icons.Filled.Done else Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (PlayerState.isBuffering) {
            Spacer(Modifier.height(8.dp))
            Text("Loading…", color = Color.Gray, fontSize = 14.sp)
        }
        PlayerState.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
        }

        // Progress: thin gold slider, elapsed left, remaining right.
        Spacer(Modifier.height(16.dp))
        val dur = PlayerState.durationMs
        val frac = if (dur > 0) PlayerState.positionMs.toFloat() / dur else 0f
        val remainMs = (dur - PlayerState.positionMs).coerceAtLeast(0)
        Slider(
            value = sliderPos ?: frac,
            onValueChange = { sliderPos = it },
            onValueChangeFinished = {
                val v = sliderPos ?: frac
                if (dur > 0) PlayerController.seekTo(ctx, (v * dur).toInt())
                sliderPos = null
            },
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFC9A86A),
                activeTrackColor = Color(0xFFC9A86A),
                inactiveTrackColor = Color(0xFF3A3A3A)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(fmtMs(PlayerState.positionMs), color = Color.Gray, fontSize = 13.sp)
            Text("-" + fmtMs(remainMs), color = Color.Gray, fontSize = 13.sp)
        }

        // Controls: round prev, big maroon pill play/pause, round next.
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { PlayerController.prev(ctx) },
                modifier = Modifier.size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262626))
            ) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Button(
                onClick = { PlayerController.toggle(ctx) },
                modifier = Modifier.width(200.dp).height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A2320),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    if (PlayerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (PlayerState.isPlaying) "Pause" else "Play",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(
                onClick = { PlayerController.next(ctx) },
                modifier = Modifier.size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF262626))
            ) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // Bottom pills: sleep timer (tap cycles), source, repeat-one.
        Spacer(Modifier.height(24.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val sleepLabel = if (PlayerState.sleepTimerMinutes == 0) "Off"
            else "${PlayerState.sleepTimerMinutes} min"
            PlayerPill(
                text = sleepLabel,
                onClick = {
                    val next = when (PlayerState.sleepTimerMinutes) {
                        0 -> 15
                        15 -> 30
                        30 -> 60
                        else -> 0
                    }
                    PlayerController.sleepTimer(ctx, next)
                }
            )
            PlayerPill(
                text = if (downloaded) "Downloaded" else "Streaming",
                onClick = null
            )
            PlayerIconPill(
                active = PlayerState.repeatOne,
                onClick = { PlayerController.repeatToggle(ctx) }
            )
        }

        Spacer(Modifier.height(12.dp))
        if (PlayerState.sleepTimerEndMs > 0) {
            val remain = ((PlayerState.sleepTimerEndMs - System.currentTimeMillis()) / 60_000L)
                .coerceAtLeast(0)
            Text(
                "Stops in ~$remain min",
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
        Text(
            "Talk ${idx + 1} of ${series.tracks.size}",
            color = Color.Gray,
            fontSize = 13.sp
        )
    }
}

/** Rounded dark pill used on the player screen (sleep timer, source). */
@Composable
private fun PlayerPill(text: String, onClick: (() -> Unit)?) {
    Box(
        Modifier.clip(RoundedCornerShape(28.dp))
            .background(Color(0xFF262626))
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/** Rounded dark pill holding the repeat-one toggle icon. */
@Composable
private fun PlayerIconPill(active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(28.dp))
            .background(if (active) Color(0xFF4A2320) else Color(0xFF262626))
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Repeat,
            contentDescription = "Repeat one",
            tint = if (active) Color(0xFFC9A86A) else Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MiniPlayer(onOpen: () -> Unit) {
    val ctx = LocalContext.current
    GlassCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { PlayerController.toggle(ctx) },
                modifier = Modifier.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (PlayerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play / pause",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    PlayerState.trackTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    PlayerState.seriesTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(mod: Modifier) {
    val ctx = LocalContext.current
    var dlTick by remember { mutableStateOf(0) }
    Column(
        mod.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text(
            "Appearance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Glass effect: ${GlassSettings.pct}%",
            style = MaterialTheme.typography.titleSmall
        )
        Slider(
            value = GlassSettings.pct.toFloat(),
            onValueChange = {
                GlassSettings.pct = it.toInt()
                GlassSettings.save(ctx)
            },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Higher = more glass. Lower = easier to read.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(16.dp))
        GlassCard(Modifier.fillMaxWidth()) {
            Text(
                "Preview — cards and glow look like this at ${GlassSettings.pct}% glass.",
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("Theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            for ((label, value) in listOf("System" to "system", "Dark" to "dark", "Light" to "light")) {
                val selected = ThemeSettings.mode == value
                Button(
                    onClick = {
                        ThemeSettings.mode = value
                        ThemeSettings.save(ctx)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface.copy(alpha = GlassSettings.cardAlpha),
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(label)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Downloads", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wi-Fi only downloads")
            Switch(
                checked = DownloadSettings.wifiOnly,
                onCheckedChange = {
                    DownloadSettings.wifiOnly = it
                    DownloadSettings.save(ctx)
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        val (dlCount, dlBytes) = remember(dlTick) { DownloadHelper.downloadedStats(ctx) }
        Text(
            "Downloaded: $dlCount talks · ${dlBytes / 1_000_000} MB",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                DownloadHelper.deleteAllDownloads(ctx)
                dlTick++
                Toast.makeText(ctx, "Downloads deleted", Toast.LENGTH_SHORT).show()
            }
        ) {
            Text("Delete all downloads")
        }
    }
}
