package com.sentinela.camtv.ui.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavigatorTest {
    @Test
    fun startsAtMosaicWhenThereAreCameras() {
        val navigator = AppNavigator()

        navigator.initialize(hasCameras = true)

        assertEquals(AppDestination.Mosaic, navigator.state.destination)
        assertTrue(navigator.state.hasCameras)
    }

    @Test
    fun startsAtHomeWhenThereAreNoCameras() {
        val navigator = AppNavigator()

        navigator.initialize(hasCameras = false)

        assertEquals(AppDestination.Home, navigator.state.destination)
        assertFalse(navigator.state.hasCameras)
    }

    @Test
    fun settingsReturnsToMosaicWhenOpenedFromMosaic() {
        val navigator = AppNavigator()
        navigator.initialize(hasCameras = true)

        navigator.openSettings()
        navigator.goBack()

        assertEquals(AppDestination.Mosaic, navigator.state.destination)
    }

    @Test
    fun cameraManagerReturnsToMosaicWhenOpenedFromMosaic() {
        val navigator = AppNavigator()
        navigator.initialize(hasCameras = true)

        navigator.openCameras()
        navigator.goBack()

        assertEquals(AppDestination.Mosaic, navigator.state.destination)
    }

    @Test
    fun settingsReturnsToHomeWhenOpenedFromHome() {
        val navigator = AppNavigator()
        navigator.initialize(hasCameras = false)

        navigator.openSettings()
        navigator.goBack()

        assertEquals(AppDestination.Home, navigator.state.destination)
    }

    @Test
    fun capturesReturnsToMosaicWhenOpenedFromMosaic() {
        val navigator = AppNavigator()
        navigator.initialize(hasCameras = true)

        navigator.openCaptures()
        navigator.goBack()

        assertEquals(AppDestination.Mosaic, navigator.state.destination)
    }

    @Test
    fun capturesReturnsToHomeWhenOpenedFromHome() {
        val navigator = AppNavigator()
        navigator.initialize(hasCameras = false)

        navigator.openCaptures()
        navigator.goBack()

        assertEquals(AppDestination.Home, navigator.state.destination)
    }

    @Test
    fun returnToMosaicStaysValidWhenCamerasDisappear() {
        val navigator = AppNavigator()
        navigator.initialize(hasCameras = true)

        navigator.openSettings()
        navigator.setCameraAvailability(hasCameras = false)
        navigator.goBack()

        assertEquals(AppDestination.Mosaic, navigator.state.destination)
    }

    @Test
    fun opensMosaicWithoutCameras() {
        val navigator = AppNavigator()
        navigator.initialize(hasCameras = false)

        navigator.openMosaic()

        assertEquals(AppDestination.Mosaic, navigator.state.destination)
    }
}
