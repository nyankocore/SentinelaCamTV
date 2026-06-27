package com.sentinela.camtv.recording

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class RecordingProbeStorage(
    private val context: Context,
) {
    fun createTempFile(fileName: String): File {
        val directory = File(context.cacheDir, TEMP_DIRECTORY)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, "$fileName.tmp")
    }

    suspend fun publishVideo(
        tempFile: File,
        fileName: String,
    ): RecordingProbeStorageResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishToMediaStore(tempFile, fileName)
        } else {
            publishToAppExternalMovies(tempFile, fileName)
        }
    }

    private fun publishToMediaStore(
        tempFile: File,
        fileName: String,
    ): RecordingProbeStorageResult {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, VIDEO_MIME_TYPE)
            put(
                MediaStore.Video.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_MOVIES}/$VIDEO_DIRECTORY",
            )
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return RecordingProbeStorageResult.Failure

        return try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                tempFile.inputStream().use { input -> input.copyTo(output) }
            } ?: run {
                resolver.delete(uri, null, null)
                return RecordingProbeStorageResult.Failure
            }
            ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }.also { completedValues ->
                resolver.update(uri, completedValues, null, null)
            }
            RecordingProbeStorageResult.Success(
                fileName = fileName,
                locationLabel = RecordingLocationLabels.STANDARD_VIDEOS,
                uri = uri,
            )
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            RecordingProbeStorageResult.Failure
        }
    }

    private fun publishToAppExternalMovies(
        tempFile: File,
        fileName: String,
    ): RecordingProbeStorageResult {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: return RecordingProbeStorageResult.Failure
        val directory = File(baseDir, VIDEO_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            return RecordingProbeStorageResult.Failure
        }

        val finalFile = File(directory, fileName)
        val pendingFile = File(directory, "$fileName.tmp")
        return try {
            tempFile.inputStream().use { input ->
                pendingFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (finalFile.exists()) {
                finalFile.delete()
            }
            if (!pendingFile.renameTo(finalFile)) {
                pendingFile.delete()
                return RecordingProbeStorageResult.Failure
            }
            RecordingProbeStorageResult.Success(
                fileName = fileName,
                locationLabel = finalFile.absolutePath,
                uri = Uri.fromFile(finalFile),
            )
        } catch (_: Exception) {
            pendingFile.delete()
            RecordingProbeStorageResult.Failure
        }
    }

    private companion object {
        const val TEMP_DIRECTORY = "recording-probe"
        const val VIDEO_DIRECTORY = "Sentinela Cam TV"
        const val VIDEO_MIME_TYPE = "video/mp4"
    }
}

sealed interface RecordingProbeStorageResult {
    data class Success(
        val fileName: String,
        val locationLabel: String,
        val uri: Uri?,
    ) : RecordingProbeStorageResult

    data object Failure : RecordingProbeStorageResult
}
