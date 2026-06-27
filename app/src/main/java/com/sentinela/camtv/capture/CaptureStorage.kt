package com.sentinela.camtv.capture

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CaptureStorage(
    private val context: Context,
) {
    suspend fun savePhoto(
        bitmap: Bitmap,
        fileName: String,
        destination: PhotoCaptureDestination,
    ): CaptureStorageResult = withContext(Dispatchers.IO) {
        if (destination.customTreeUri != null) {
            saveToCustomTree(bitmap, fileName, destination.customTreeUri)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveToMediaStore(bitmap, fileName)
        } else {
            saveToAppExternalPictures(bitmap, fileName)
        }
    }

    private fun saveToCustomTree(
        bitmap: Bitmap,
        fileName: String,
        treeUri: Uri,
    ): CaptureStorageResult {
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: return CaptureStorageResult.Failure(CaptureError.CustomLocationUnavailable)
        if (!tree.exists() || !tree.canWrite()) {
            return CaptureStorageResult.Failure(CaptureError.CustomLocationUnavailable)
        }

        val document = tree.createFile(PHOTO_MIME_TYPE, fileName)
            ?: return CaptureStorageResult.Failure(CaptureError.CustomLocationUnavailable)
        return try {
            context.contentResolver.openOutputStream(document.uri, "w")?.use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, output)) {
                    document.delete()
                    return CaptureStorageResult.Failure(CaptureError.WriteFailed)
                }
            } ?: run {
                document.delete()
                return CaptureStorageResult.Failure(CaptureError.WriteFailed)
            }
            CaptureStorageResult.Success(
                fileName = fileName,
                locationLabel = CaptureLocationLabels.CUSTOM_PHOTOS,
                uri = document.uri,
            )
        } catch (_: Exception) {
            document.delete()
            CaptureStorageResult.Failure(CaptureError.WriteFailed)
        }
    }

    private fun saveToMediaStore(
        bitmap: Bitmap,
        fileName: String,
    ): CaptureStorageResult {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, PHOTO_MIME_TYPE)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/$PHOTO_DIRECTORY",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return CaptureStorageResult.Failure(CaptureError.WriteFailed)

        return try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, output)) {
                    resolver.delete(uri, null, null)
                    return CaptureStorageResult.Failure(CaptureError.WriteFailed)
                }
            } ?: run {
                resolver.delete(uri, null, null)
                return CaptureStorageResult.Failure(CaptureError.WriteFailed)
            }
            ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }.also { completedValues ->
                resolver.update(uri, completedValues, null, null)
            }
            CaptureStorageResult.Success(
                fileName = fileName,
                locationLabel = CaptureLocationLabels.STANDARD_PHOTOS,
                uri = uri,
            )
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            CaptureStorageResult.Failure(CaptureError.WriteFailed)
        }
    }

    private fun saveToAppExternalPictures(
        bitmap: Bitmap,
        fileName: String,
    ): CaptureStorageResult {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: return CaptureStorageResult.Failure(CaptureError.WriteFailed)
        val directory = File(baseDir, PHOTO_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            return CaptureStorageResult.Failure(CaptureError.WriteFailed)
        }

        val finalFile = File(directory, fileName)
        val tempFile = File(directory, "$fileName.tmp")
        return try {
            tempFile.outputStream().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, PHOTO_QUALITY, output)) {
                    tempFile.delete()
                    return CaptureStorageResult.Failure(CaptureError.WriteFailed)
                }
            }
            if (finalFile.exists()) {
                finalFile.delete()
            }
            if (!tempFile.renameTo(finalFile)) {
                tempFile.delete()
                return CaptureStorageResult.Failure(CaptureError.WriteFailed)
            }
            CaptureStorageResult.Success(
                fileName = fileName,
                locationLabel = CaptureLocationLabels.APP_EXTERNAL_PHOTOS,
                uri = Uri.fromFile(finalFile),
            )
        } catch (_: Exception) {
            tempFile.delete()
            CaptureStorageResult.Failure(CaptureError.WriteFailed)
        }
    }

    private companion object {
        const val PHOTO_MIME_TYPE = "image/jpeg"
        const val PHOTO_QUALITY = 92
        const val PHOTO_DIRECTORY = "Sentinela Cam TV"
    }
}

sealed interface CaptureStorageResult {
    data class Success(
        val fileName: String,
        val locationLabel: String,
        val uri: Uri?,
    ) : CaptureStorageResult

    data class Failure(val error: CaptureError) : CaptureStorageResult
}
