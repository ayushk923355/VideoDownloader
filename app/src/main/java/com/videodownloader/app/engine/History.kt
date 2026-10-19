package com.videodownloader.app.engine

import android.content.Context
import com.videodownloader.app.core.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Download history, saved as history.json in app-private storage so it survives restarts. */
object History {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items.asStateFlow()

    private fun file(c: Context) = File(c.filesDir, "history.json")

    fun load(c: Context) {
        val f = file(c)
        if (!f.exists()) return
        runCatching {
            val a = JSONArray(f.readText())
            _items.value = (0 until a.length()).map {
                val o = a.getJSONObject(it)
                Item(o.getString("id"), o.getString("title"), o.getString("uri"),
                    o.getString("res"), o.getLong("size"), o.getLong("date"))
            }
        }
    }

    private fun save(c: Context) {
        val a = JSONArray()
        _items.value.forEach {
            a.put(JSONObject().put("id", it.id).put("title", it.title).put("uri", it.uri)
                .put("res", it.res).put("size", it.size).put("date", it.dateMs))
        }
        file(c).writeText(a.toString())
    }

    fun add(c: Context, item: Item) { _items.value = listOf(item) + _items.value; save(c) }
    fun remove(c: Context, id: String) { _items.value = _items.value.filter { it.id != id }; save(c) }
}
