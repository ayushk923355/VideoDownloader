package com.videodownloader.app.core

data class Item(
    val id: String,
    val title: String,
    val uri: String,
    val res: String,
    val size: Long,
    val dateMs: Long,
)

data class Job(
    val id: String,
    val url: String,
    val title: String = "Checking link…",
    val res: String = "",
    val state: DownloadState = DownloadState.Queued,
    val totalBytes: Long? = null,
    val doneBytes: Long? = null,
)
