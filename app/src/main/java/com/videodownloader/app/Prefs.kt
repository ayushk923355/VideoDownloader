package com.videodownloader.app

import android.content.Context

object Prefs {
    val qualities = listOf(144, 240, 360, 480, 720, 1080, 1440)
    private fun sp(c: Context) = c.getSharedPreferences("vd", Context.MODE_PRIVATE)
    fun quality(c: Context): Int = sp(c).getInt("quality", 1080)
    fun setQuality(c: Context, q: Int) = sp(c).edit().putInt("quality", q.coerceAtMost(1440)).apply()
}
