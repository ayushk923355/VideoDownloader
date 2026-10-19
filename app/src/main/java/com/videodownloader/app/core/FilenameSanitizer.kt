package com.videodownloader.app.core

object FilenameSanitizer {
    private val illegal = Regex("""[\\/:*?"<>|\u0000-\u001F]""")

    fun sanitize(raw: String?, fallback: String = "video", maxLen: Int = 120): String {
        var s = (raw ?: "").replace(illegal, "_").replace("..", "_").trim().trim('.', ' ')
        if (s.length > maxLen) s = s.take(maxLen).trim().trimEnd('.')
        return s.ifBlank { fallback }
    }

    /** Appends " (n)" before the extension until the name is unused. */
    fun uniqueName(base: String, ext: String, exists: (String) -> Boolean): String {
        var name = "$base.$ext"
        var n = 1
        while (exists(name)) name = "$base ($n).$ext".also { n++ }
        return name
    }
}
