package com.sentinela.camtv.capture

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import androidx.annotation.RequiresApi
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FrameCaptureEngine {
    suspend fun capture(playerView: PlayerView): FrameCaptureResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return FrameCaptureResult.Failure(CaptureError.UnsupportedAndroid)
        }

        val videoSurface = playerView.videoSurfaceView ?: return FrameCaptureResult.Failure(
            CaptureError.NoCaptureSource,
        )

        return when (videoSurface) {
            is TextureView -> captureTextureView(videoSurface)
            is SurfaceView -> captureSurfaceView(videoSurface)
            else -> FrameCaptureResult.Failure(CaptureError.NoCaptureSource)
        }
    }

    private fun captureTextureView(textureView: TextureView): FrameCaptureResult {
        val width = textureView.width
        val height = textureView.height
        if (width <= 0 || height <= 0) {
            return FrameCaptureResult.Failure(CaptureError.NoCaptureSource)
        }
        val bitmap = runCatching { textureView.getBitmap(width, height) }.getOrNull()
            ?: return FrameCaptureResult.Failure(CaptureError.SourceNoData)
        return FrameCaptureResult.Success(bitmap)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private suspend fun captureSurfaceView(surfaceView: SurfaceView): FrameCaptureResult {
        val width = surfaceView.width
        val height = surfaceView.height
        if (width <= 0 || height <= 0) {
            return FrameCaptureResult.Failure(CaptureError.NoCaptureSource)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        return suspendCancellableCoroutine { continuation ->
            PixelCopy.request(
                surfaceView,
                bitmap,
                { result ->
                    if (!continuation.isActive) {
                        bitmap.recycle()
                        return@request
                    }
                    continuation.resume(
                        if (result == PixelCopy.SUCCESS) {
                            FrameCaptureResult.Success(bitmap)
                        } else {
                            bitmap.recycle()
                            FrameCaptureResult.Failure(result.toCaptureError())
                        },
                    )
                },
                Handler(Looper.getMainLooper()),
            )
            continuation.invokeOnCancellation {
                bitmap.recycle()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private val PlayerView.videoSurfaceView
        get() = getVideoSurfaceView()
}

sealed interface FrameCaptureResult {
    data class Success(val bitmap: Bitmap) : FrameCaptureResult
    data class Failure(val error: CaptureError) : FrameCaptureResult
}

private fun Int.toCaptureError(): CaptureError =
    if (this == PixelCopy.ERROR_SOURCE_NO_DATA) {
        CaptureError.SourceNoData
    } else {
        CaptureError.CaptureFailed
    }
