package com.app.oshoaudio.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Track(val title: String, val url: String, val size: Long) {
    val fileName: String
        get() = try {
            java.net.URLDecoder.decode(url.substringAfterLast('/'), "UTF-8")
        } catch (e: Exception) {
            url.substringAfterLast('/')
        }
    val sizeMb: String
        get() = if (size > 0) "%.0f MB".format(size / 1_000_000.0) else ""
}

data class Series(
    val id: String,
    val language: String,
    val title: String,
    val count: Int,
    val person: String?,
    val tracks: List<Track>
)

data class TgChannel(
    val name: String,
    val username: String,
    val kind: String,
    val members: Int?,
    val content: String
)

object CatalogStore {
    var series: List<Series> = emptyList()
    var tg: List<TgChannel> = emptyList()
    var loaded = false

    fun load(ctx: Context) {
        if (loaded) return
        val root = JSONObject(ctx.assets.open("catalog.json").bufferedReader().readText())
        val arr = root.getJSONArray("series")
        val list = ArrayList<Series>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val tarr = o.getJSONArray("tracks")
            val tracks = ArrayList<Track>()
            for (j in 0 until tarr.length()) {
                val t = tarr.getJSONObject(j)
                tracks.add(Track(t.getString("title"), t.getString("url"), t.optLong("size")))
            }
            list.add(
                Series(
                    o.getString("id"),
                    o.getString("language"),
                    o.getString("title"),
                    o.optInt("count", tracks.size),
                    if (o.isNull("person")) null else o.getString("person"),
                    tracks
                )
            )
        }
        series = list
        val tgArr = JSONArray(ctx.assets.open("telegram.json").bufferedReader().readText())
        val tgList = ArrayList<TgChannel>()
        for (i in 0 until tgArr.length()) {
            val o = tgArr.getJSONObject(i)
            tgList.add(
                TgChannel(
                    o.getString("name"),
                    o.getString("username"),
                    o.getString("kind"),
                    if (o.isNull("members")) null else o.optInt("members"),
                    o.getString("content")
                )
            )
        }
        tg = tgList
        loaded = true
    }

    fun seriesById(id: String): Series? = series.firstOrNull { it.id == id }

    fun persons(): List<Pair<String, Int>> {
        val map = LinkedHashMap<String, Int>()
        for (s in series) {
            val p = s.person ?: continue
            map[p] = (map[p] ?: 0) + s.count
        }
        return map.entries.sortedByDescending { it.value }.map { it.key to it.value }
    }

    fun seriesForLanguage(lang: String): List<Series> =
        series.filter { it.language == lang }.sortedBy { it.title.lowercase() }

    fun seriesForPerson(person: String): List<Series> =
        series.filter { it.person == person }.sortedBy { it.title.lowercase() }

    fun searchSeries(q: String): List<Series> {
        val needle = q.trim().lowercase()
        if (needle.isEmpty()) return emptyList()
        return series.filter { it.title.lowercase().contains(needle) }.take(80)
    }

    fun languageStats(): List<Triple<String, Int, Int>> {
        val map = LinkedHashMap<String, Pair<Int, Int>>()
        for (s in series) {
            val cur = map[s.language] ?: (0 to 0)
            map[s.language] = (cur.first + 1) to (cur.second + s.count)
        }
        return map.entries.map { Triple(it.key, it.value.first, it.value.second) }
    }
}
