package com.sentinela.camtv.data.camera

import com.sentinela.camtv.data.db.CameraDao
import com.sentinela.camtv.data.db.CameraEntity
import com.sentinela.camtv.data.security.CredentialCipher
import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.CameraSource
import com.sentinela.camtv.domain.CameraSourceType
import com.sentinela.camtv.domain.DvrRtspChannel
import com.sentinela.camtv.domain.OnvifCameraSource
import com.sentinela.camtv.domain.RtspCameraSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCameraRepository(
    private val cameraDao: CameraDao,
    private val credentialCipher: CredentialCipher,
) : CameraRepository {
    override fun observeAllCameras(): Flow<List<Camera>> =
        cameraDao.observeAll().map { entities -> entities.map(::toDomain) }

    override fun observeEnabledCameras(): Flow<List<Camera>> =
        cameraDao.observeEnabled().map { entities -> entities.map(::toDomain) }

    override suspend fun hasEnabledCameras(): Boolean =
        cameraDao.enabledCount() > 0

    override suspend fun seedDebugCamerasIfEmpty(cameras: List<Camera>) {
        if (cameraDao.enabledCount() > 0) return
        cameraDao.upsertAll(cameras.mapIndexed { index, camera -> camera.toEntity(index) })
    }

    override suspend fun saveManualRtspCamera(
        id: String,
        name: String,
        rtspUrl: String,
        subRtspUrl: String?,
        username: String?,
        password: String?,
        position: Int,
    ) {
        val main = RtspUrlSanitizer.sanitize(rtspUrl)
        val sub = subRtspUrl?.let(RtspUrlSanitizer::sanitize)
        val credentials = RtspCredentialResolver.resolve(
            main = main,
            sub = sub,
            username = username,
            password = password,
        )

        cameraDao.upsert(
            CameraEntity(
                id = id,
                name = name,
                sourceType = CameraSourceType.RTSP_MANUAL.name,
                endpoint = main.urlWithoutUserInfo,
                onvifDeviceServiceUrl = null,
                mainRtspUrl = main.urlWithoutUserInfo,
                subRtspUrl = sub?.urlWithoutUserInfo,
                intelbrasChannel = null,
                usernameCipherText = credentialCipher.encrypt(credentials.username),
                passwordCipherText = credentialCipher.encrypt(credentials.password),
                position = position,
                enabled = true,
                authFailure = false,
            ),
        )
    }

    override suspend fun saveOnvifCamera(
        id: String,
        name: String,
        endpoint: String,
        onvifDeviceServiceUrl: String,
        mainRtspUrl: String,
        subRtspUrl: String?,
        username: String?,
        password: String?,
        position: Int,
    ) {
        val main = RtspUrlSanitizer.sanitize(mainRtspUrl)
        val sub = subRtspUrl?.let(RtspUrlSanitizer::sanitize)
        val resolvedUsername = main.username ?: sub?.username ?: username
        val resolvedPassword = main.password ?: sub?.password ?: password

        cameraDao.upsert(
            CameraEntity(
                id = id,
                name = name,
                sourceType = CameraSourceType.ONVIF.name,
                endpoint = endpoint,
                onvifDeviceServiceUrl = onvifDeviceServiceUrl,
                mainRtspUrl = main.urlWithoutUserInfo,
                subRtspUrl = sub?.urlWithoutUserInfo,
                intelbrasChannel = null,
                usernameCipherText = credentialCipher.encrypt(resolvedUsername),
                passwordCipherText = credentialCipher.encrypt(resolvedPassword),
                position = position,
                enabled = true,
                authFailure = false,
            ),
        )
    }

    override suspend fun updateCameraOrder(cameraIds: List<String>) {
        cameraIds.forEachIndexed { index, cameraId ->
            cameraDao.updatePosition(cameraId, index)
        }
    }

    override suspend fun setAuthenticationFailure(cameraId: String, hasFailure: Boolean) {
        cameraDao.updateAuthFailure(cameraId, hasFailure)
    }

    override suspend fun deleteCamera(cameraId: String) {
        val remainingIds = CameraOrder.remainingIdsAfterRemoval(
            orderedIds = cameraDao.orderedIds(),
            removedId = cameraId,
        )
        cameraDao.deleteById(cameraId)
        remainingIds.forEachIndexed { index, id ->
            cameraDao.updatePosition(id, index)
        }
    }

    private fun Camera.toEntity(positionFallback: Int): CameraEntity {
        val sourceType = when (source) {
            is DvrRtspChannel -> CameraSourceType.INTELBRAS_DVR_CHANNEL
            is OnvifCameraSource -> CameraSourceType.ONVIF
            is RtspCameraSource -> CameraSourceType.RTSP_MANUAL
        }

        return CameraEntity(
            id = id,
            name = name,
            sourceType = sourceType.name,
            endpoint = source.endpoint(),
            onvifDeviceServiceUrl = (source as? OnvifCameraSource)?.deviceServiceUrl,
            mainRtspUrl = when (source) {
                is OnvifCameraSource -> source.mainRtspUrl
                is RtspCameraSource -> source.mainRtspUrl
                is DvrRtspChannel -> null
            },
            subRtspUrl = when (source) {
                is OnvifCameraSource -> source.subRtspUrl
                is RtspCameraSource -> source.subRtspUrl
                is DvrRtspChannel -> null
            },
            intelbrasChannel = (source as? DvrRtspChannel)?.channel,
            usernameCipherText = null,
            passwordCipherText = null,
            position = if (position > 0) position else positionFallback,
            enabled = enabled,
            authFailure = hasAuthenticationFailure,
        )
    }

    private fun CameraSource.endpoint(): String = when (this) {
        is DvrRtspChannel -> "dvr-rtsp://channel/$channel"
        is OnvifCameraSource -> deviceServiceUrl
        is RtspCameraSource -> mainRtspUrl
    }

    private fun toDomain(entity: CameraEntity): Camera {
        val username = credentialCipher.decrypt(entity.usernameCipherText)
        val password = credentialCipher.decrypt(entity.passwordCipherText)
        val source = when (CameraSourceType.valueOf(entity.sourceType)) {
            CameraSourceType.INTELBRAS_DVR_CHANNEL -> DvrRtspChannel(
                channel = entity.intelbrasChannel ?: 1,
                subtype = 0,
            )

            CameraSourceType.ONVIF -> OnvifCameraSource(
                deviceServiceUrl = entity.onvifDeviceServiceUrl ?: entity.endpoint,
                mainRtspUrl = RtspUrlSanitizer.withCredentials(
                    sanitizedUrl = entity.mainRtspUrl ?: entity.endpoint,
                    username = username,
                    password = password,
                ),
                subRtspUrl = entity.subRtspUrl?.let { url ->
                    RtspUrlSanitizer.withCredentials(url, username, password)
                },
            )

            CameraSourceType.RTSP_MANUAL -> RtspCameraSource(
                mainRtspUrl = RtspUrlSanitizer.withCredentials(
                    sanitizedUrl = entity.mainRtspUrl ?: entity.endpoint,
                    username = username,
                    password = password,
                ),
                subRtspUrl = entity.subRtspUrl?.let { url ->
                    RtspUrlSanitizer.withCredentials(url, username, password)
                },
            )
        }

        return Camera(
            id = entity.id,
            name = entity.name,
            source = source,
            position = entity.position,
            enabled = entity.enabled,
            hasAuthenticationFailure = entity.authFailure,
        )
    }
}
