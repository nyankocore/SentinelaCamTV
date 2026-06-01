package com.sentinela.camtv.config

import com.sentinela.camtv.domain.Camera
import com.sentinela.camtv.domain.IntelbrasDvrChannel

const val MOSAIC_SUBTYPE = 1
const val FULLSCREEN_SUBTYPE = 0

const val SHOW_APP_HEADER = false

fun defaultMosaicCameras(): List<Camera> = (1..5).map { channel ->
    Camera(
        id = "cam-$channel",
        name = "CAM$channel",
        source = IntelbrasDvrChannel(
            channel = channel,
            subtype = MOSAIC_SUBTYPE,
        ),
    )
}
