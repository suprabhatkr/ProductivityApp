package com.example.productivityapp.run

sealed class RunReplayExportException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class RunNotFound(runId: Long) :
        RunReplayExportException("Run $runId could not be found.")

    class InsufficientRouteData :
        RunReplayExportException("This run does not have enough route data to export a replay.")

    class SnapshotRenderingFailed(message: String, cause: Throwable? = null) :
        RunReplayExportException(message, cause)

    class VideoEncodingFailed(message: String, cause: Throwable? = null) :
        RunReplayExportException(message, cause)

    class SharePreparationFailed(message: String, cause: Throwable? = null) :
        RunReplayExportException(message, cause)
}
