package com.sentinela.camtv.capture

import com.sentinela.camtv.R
import com.sentinela.camtv.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureUserMessageTest {
    @Test
    fun unsupportedAndroidUsesClearMessage() {
        assertEquals(
            UiText.Resource(R.string.capture_unsupported_android),
            CaptureError.UnsupportedAndroid.userMessage(),
        )
    }

    @Test
    fun firstFrameMissingAsksUserToWaitForImage() {
        assertEquals(
            UiText.Resource(R.string.capture_first_frame_missing),
            CaptureError.FirstFrameMissing.userMessage(),
        )
    }

    @Test
    fun sourceNoDataUsesFriendlyFailure() {
        assertEquals(
            UiText.Resource(R.string.capture_failed_now),
            CaptureError.SourceNoData.userMessage(),
        )
    }

    @Test
    fun successMessageIsShort() {
        assertEquals(
            UiText.Resource(R.string.capture_photo_saved),
            CaptureResult.Success(
                fileName = "foto.jpg",
                locationLabel = CaptureLocationLabels.STANDARD_PHOTOS,
                uri = null,
            ).userMessage(),
        )
    }
}
