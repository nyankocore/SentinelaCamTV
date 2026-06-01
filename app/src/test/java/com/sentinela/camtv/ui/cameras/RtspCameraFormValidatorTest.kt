package com.sentinela.camtv.ui.cameras

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RtspCameraFormValidatorTest {
    @Test
    fun rejectsBlankMainRtspUrl() {
        val validation = RtspCameraFormValidator.validate(
            name = "Portao",
            mainRtspUrl = "",
            subRtspUrl = "",
            username = "",
            password = "",
        )

        assertTrue(validation is RtspCameraFormValidation.Invalid)
    }

    @Test
    fun rejectsMainUrlWithoutRtspScheme() {
        val validation = RtspCameraFormValidator.validate(
            name = "Portao",
            mainRtspUrl = "http://198.51.100.10/live",
            subRtspUrl = "",
            username = "",
            password = "",
        )

        assertTrue(validation is RtspCameraFormValidation.Invalid)
    }

    @Test
    fun rejectsIncompleteMainRtspUrlWithoutHost() {
        val validation = RtspCameraFormValidator.validate(
            name = "DVR",
            mainRtspUrl = "rtsp://198.51.100",
            subRtspUrl = "",
            username = "",
            password = "",
        )

        assertEquals(
            "Informe uma URL RTSP principal válida.",
            (validation as RtspCameraFormValidation.Invalid).message,
        )
    }

    @Test
    fun rejectsIncompleteSubRtspUrlWithoutHost() {
        val validation = RtspCameraFormValidator.validate(
            name = "DVR",
            mainRtspUrl = "rtsp://198.51.100.10/live",
            subRtspUrl = "rtsp://198.51.100",
            username = "",
            password = "",
        )

        assertEquals(
            "Informe uma URL RTSP secundária válida ou deixe o campo vazio.",
            (validation as RtspCameraFormValidation.Invalid).message,
        )
    }

    @Test
    fun rejectsMainUrlWithEmbeddedCredentials() {
        val validation = RtspCameraFormValidator.validate(
            name = "Portao",
            mainRtspUrl = "rtsp://" + "admin:secret@" + "198.51.100.10/live",
            subRtspUrl = "",
            username = "",
            password = "",
        )

        assertEquals(
            RtspCameraFormValidator.CREDENTIALS_IN_URL_MESSAGE,
            (validation as RtspCameraFormValidation.Invalid).message,
        )
    }

    @Test
    fun rejectsSubUrlWithEmbeddedCredentials() {
        val validation = RtspCameraFormValidator.validate(
            name = "Portao",
            mainRtspUrl = "rtsp://198.51.100.10/live",
            subRtspUrl = "rtsp://" + "admin:secret@" + "198.51.100.10/sub",
            username = "",
            password = "",
        )

        assertEquals(
            RtspCameraFormValidator.CREDENTIALS_IN_URL_MESSAGE,
            (validation as RtspCameraFormValidation.Invalid).message,
        )
    }

    @Test
    fun usesRtspFallbackNameWhenNameIsBlank() {
        val validation = RtspCameraFormValidator.validate(
            name = " ",
            mainRtspUrl = " rtsp://198.51.100.10/live ",
            subRtspUrl = "",
            username = "",
            password = "",
        )

        val form = (validation as RtspCameraFormValidation.Valid).form
        assertEquals("RTSP", form.name)
        assertEquals("rtsp://198.51.100.10/live", form.mainRtspUrl)
    }

    @Test
    fun keepsOptionalRtspCredentialsFromSeparateFields() {
        val validation = RtspCameraFormValidator.validate(
            name = "DVR",
            mainRtspUrl = "rtsp://198.51.100.10/live",
            subRtspUrl = "rtsp://198.51.100.10/sub",
            username = " admin ",
            password = "secret",
        )

        val form = (validation as RtspCameraFormValidation.Valid).form
        assertEquals("admin", form.username)
        assertEquals("secret", form.password)
        assertEquals("rtsp://198.51.100.10/sub", form.subRtspUrl)
    }

    @Test
    fun allowsSameRtspUrlInMainAndSubFields() {
        val validation = RtspCameraFormValidator.validate(
            name = "DVR",
            mainRtspUrl = "rtsp://198.51.100.10/live",
            subRtspUrl = "rtsp://198.51.100.10/live",
            username = "",
            password = "",
        )

        val form = (validation as RtspCameraFormValidation.Valid).form
        assertEquals("rtsp://198.51.100.10/live", form.mainRtspUrl)
        assertEquals("rtsp://198.51.100.10/live", form.subRtspUrl)
    }
}
