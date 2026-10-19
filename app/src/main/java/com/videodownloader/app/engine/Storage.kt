package com.videodownloader.app.engine

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.videodownloader.app.core.FilenameSanitizer
import java.io.File
import java.io.IOException

object Storage {
    const val FOLDER = "Video Downloader"
    const val SHOWN_PATH = "Download/Video Downloader/"

    private fun mime(ext: String) = when (ext) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        else -> "video/*"
    }

    /** Copies the finished file into Download/Video Downloader/. MediaStore recreates the folder if it was deleted. */
    fun save(c: Context, src: File, baseName: String, ext: String): Uri {
        val name = FilenameSanitizer.sanitize(baseName)
        if (Build.VERSION.SDK_INT >= 29) {
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.$ext")
                put(MediaStore.MediaColumns.MIME_TYPE, mime(ext))
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = c.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                ?: throw IOException("insert failed")
            try {
                c.contentResolver.openOutputStream(uri)!!.use { o -> src.inputStream().use { it.copyTo(o) } }
                val len = c.contentResolver.openFileDescriptor(uri, "r")!!.use { it.statSize }
                if (len != src.length()) throw IOException("size mismatch")
                val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                c.contentResolver.update(uri, done, null, null)
            } catch (e: Exception) {
                c.contentResolver.delete(uri, null, null)
                throw e
            }
            return uri
        }
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER)
        if (!dir.exists() && !dir.mkdirs()) throw IOException("folder")
        val unique = FilenameSanitizer.uniqueName(name, ext) { File(dir, it).exists() }
        val dst = File(dir, unique)
        src.copyTo(dst)
        if (dst.length() != src.length()) { dst.delete(); throw IOException("size mismatch") }
        MediaScannerConnection.scanFile(c, arrayOf(dst.path), null, null)
        return Uri.fromFile(dst)
    }

    fun exists(c: Context, uri: String): Boolean = runCatching {
        c.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
    }.getOrDefault(false)

    fun delete(c: Context, uri: String) {
        val u = Uri.parse(uri)
        runCatching {
            if (u.scheme == "file") File(u.path!!).delete() else c.contentResolver.delete(u, null, null)
        }
    }
}
