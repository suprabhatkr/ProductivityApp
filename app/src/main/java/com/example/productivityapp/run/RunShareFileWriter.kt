package com.example.productivityapp.run

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

interface RunShareFileWriter {
    fun prepareShareAsset(fileName: String): File
    fun buildShareAsset(outputFile: File): RunReplayShareAsset
}

class FileProviderRunShareFileWriter(
    private val context: Context,
) : RunShareFileWriter {
    private val appContext = context.applicationContext

    override fun prepareShareAsset(fileName: String): File {
        val exportDir = File(appContext.cacheDir, "run-replays")
        if (!exportDir.exists() && !exportDir.mkdirs()) {
            throw RunReplayExportException.SharePreparationFailed("Replay share storage could not be prepared.")
        }
        return File(exportDir, fileName)
    }

    override fun buildShareAsset(outputFile: File): RunReplayShareAsset {
        if (!outputFile.exists()) {
            throw RunReplayExportException.SharePreparationFailed("Replay video file was not found after export.")
        }
        val uri = try {
            FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", outputFile)
        } catch (error: IllegalArgumentException) {
            throw RunReplayExportException.SharePreparationFailed("Replay share file could not be exposed to Android sharing.", error)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Run replay")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return RunReplayShareAsset(
            uri = uri,
            fileName = outputFile.name,
            shareIntent = Intent.createChooser(intent, "Share run replay"),
        )
    }
}
