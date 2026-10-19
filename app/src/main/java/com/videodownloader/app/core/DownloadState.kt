package com.videodownloader.app.core

import java.io.File

sealed interface DownloadState {
    data object Queued : DownloadState
    /** fraction == null means indeterminate (total size unknown). */
    data class Downloading(val fraction: Float?, val speedBps: Long? = null, val etaSec: Long? = null) : DownloadState
    data object Merging : DownloadState
    data object Verifying : DownloadState
    data class Completed(val path: String) : DownloadState
    data class Failed(val message: String) : DownloadState
    data object Cancelled : DownloadState

    val isTerminal get() = this is Completed || this is Failed || this is Cancelled
}

object StateMachine {
    fun canTransition(from: DownloadState, to: DownloadState): Boolean {
        if (from.isTerminal) return false
        if (to is DownloadState.Failed || to is DownloadState.Cancelled) return true
        return when (from) {
            is DownloadState.Queued -> to is DownloadState.Downloading
            is DownloadState.Downloading -> to is DownloadState.Downloading ||
                to is DownloadState.Merging || to is DownloadState.Verifying
            is DownloadState.Merging -> to is DownloadState.Verifying
            is DownloadState.Verifying -> to is DownloadState.Completed
            else -> false
        }
    }
}

object CompletionVerifier {
    /** Completed (100%) is allowed only if the final file exists and is non-empty. */
    fun verify(file: File): Boolean = file.isFile && file.length() > 0
}
