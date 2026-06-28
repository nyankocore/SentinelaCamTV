package com.sentinela.camtv.ui.settings

import com.sentinela.camtv.R
import com.sentinela.camtv.data.update.AppUpdateInstallResult
import com.sentinela.camtv.data.update.AvailableUpdate
import com.sentinela.camtv.data.update.DownloadedUpdate
import com.sentinela.camtv.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateUiStateReducerTest {
    @Test
    fun permissionRequiredKeepsDownloadedUpdate() {
        val downloaded = downloadedUpdate()

        val state = UpdateUiStateReducer.afterInstallResult(
            current = UpdateUiState(downloading = true),
            downloaded = downloaded,
            result = AppUpdateInstallResult.PermissionRequired,
        )

        assertFalse(state.downloading)
        assertTrue(state.showDialog)
        assertTrue(state.waitingForInstallPermission)
        assertEquals(downloaded, state.downloadedUpdate)
        assertEquals(
            UiText.Resource(R.string.update_permission_required),
            state.message,
        )
    }

    @Test
    fun returnWithPermissionGrantedRetriesInstaller() {
        val state = UpdateUiState(
            showDialog = true,
            downloadedUpdate = downloadedUpdate(),
            waitingForInstallPermission = true,
        )

        assertTrue(
            UpdateUiStateReducer.shouldRetryInstallerOnResume(
                state = state,
                canRequestPackageInstalls = true,
            ),
        )
    }

    @Test
    fun returnWithoutPermissionKeepsInstallButtonAvailable() {
        val state = UpdateUiState(
            showDialog = true,
            downloadedUpdate = downloadedUpdate(),
            waitingForInstallPermission = true,
        )

        assertFalse(
            UpdateUiStateReducer.shouldRetryInstallerOnResume(
                state = state,
                canRequestPackageInstalls = false,
            ),
        )
    }

    @Test
    fun installerOpenedClearsPermissionWaitButKeepsDownloadedUpdate() {
        val downloaded = downloadedUpdate()

        val state = UpdateUiStateReducer.afterInstallResult(
            current = UpdateUiState(waitingForInstallPermission = true),
            downloaded = downloaded,
            result = AppUpdateInstallResult.InstallerOpened,
        )

        assertFalse(state.waitingForInstallPermission)
        assertEquals(downloaded, state.downloadedUpdate)
        assertEquals(
            UiText.Resource(R.string.update_installer_opened),
            state.message,
        )
    }

    private fun downloadedUpdate(): DownloadedUpdate =
        DownloadedUpdate(
            update = AvailableUpdate(
                versionName = "1.1.0",
                assetName = "SentinelaCamTV-v1.1.0-armeabi-v7a.apk",
                downloadUrl = "https://example.invalid/SentinelaCamTV-v1.1.0-armeabi-v7a.apk",
                checksumUrl = "https://example.invalid/SHA256SUMS.txt",
                releasePageUrl = "https://example.invalid/release",
                changelog = "Teste",
            ),
            filePath = "cache/updates/SentinelaCamTV-v1.1.0-armeabi-v7a.apk",
        )
}
