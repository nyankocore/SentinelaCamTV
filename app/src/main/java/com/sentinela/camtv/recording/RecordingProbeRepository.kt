package com.sentinela.camtv.recording

import android.os.Build
import com.sentinela.camtv.capture.CaptureFileNameFormatter
import com.sentinela.camtv.diagnostics.DiagnosticsSanitizer
import com.sentinela.camtv.recording.rtsp.RtspRecordingEngine
import com.sentinela.camtv.recording.rtsp.RtspRecordingResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class RecordingProbeRepository(
    private val storage: RecordingProbeStorage,
    private val fileNameFormatter: CaptureFileNameFormatter = CaptureFileNameFormatter(),
    private val rtspRecordingEngine: RtspRecordingEngine = RtspRecordingEngine(),
) {
    suspend fun recordVideoProbe(
        request: RecordingProbeRequest,
        stopSignal: RecordingStopSignal,
    ): RecordingProbeResult = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return@withContext RecordingProbeResult.Failure(RecordingProbeError.UnsupportedAndroid)
        }

        val fileName = fileNameFormatter.videoProbeFileName(request.cameraName)
        val tempFile = storage.createTempFile(fileName)
        tempFile.delete()

        val engineResult = try {
            recordRtspVideoToTempFile(
                rtspUrl = request.rtspUrl,
                outputFile = tempFile,
                maxDurationMs = request.maxDurationMs,
                stopSignal = stopSignal,
            )
        } catch (exception: CancellationException) {
            tempFile.delete()
            throw exception
        } catch (throwable: Throwable) {
            Timber.tag(RECORDING_LOG_TAG).w(
                throwable,
                "falha no teste de gravacao: ${DiagnosticsSanitizer.sanitize(throwable.message.orEmpty())}",
            )
            RtspRecordingResult.Failure(RecordingProbeError.WriteFailed)
        }

        if (engineResult is RtspRecordingResult.Failure) {
            tempFile.delete()
            return@withContext RecordingProbeResult.Failure(engineResult.error)
        }

        when (val published = storage.publishVideo(tempFile, fileName)) {
            is RecordingProbeStorageResult.Success -> {
                tempFile.delete()
                RecordingProbeResult.Success(
                    fileName = published.fileName,
                    locationLabel = published.locationLabel,
                    uri = published.uri,
                    warning = (engineResult as RtspRecordingResult.Success).warning,
                )
            }
            RecordingProbeStorageResult.Failure -> {
                tempFile.delete()
                RecordingProbeResult.Failure(RecordingProbeError.WriteFailed)
            }
        }
    }

    private suspend fun recordRtspVideoToTempFile(
        rtspUrl: String,
        outputFile: java.io.File,
        maxDurationMs: Long,
        stopSignal: RecordingStopSignal,
    ): RtspRecordingResult = rtspRecordingEngine.record(
        rtspUrl = rtspUrl,
        outputFile = outputFile,
        maxDurationMs = maxDurationMs,
        stopSignal = stopSignal,
    )

    private companion object {
        const val RECORDING_LOG_TAG = "SentinelaRecording"
    }
}
