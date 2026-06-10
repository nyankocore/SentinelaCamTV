package com.sentinela.camtv.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sentinela.camtv.data.camera.CameraRepository
import com.sentinela.camtv.data.camera.RoomCameraRepository
import com.sentinela.camtv.data.db.SentinelaDatabase
import com.sentinela.camtv.data.mosaic.MosaicLayoutRepository
import com.sentinela.camtv.data.mosaic.RoomMosaicLayoutRepository
import com.sentinela.camtv.data.onvif.AndroidWsDiscoveryClient
import com.sentinela.camtv.data.onvif.DefaultOnvifRepository
import com.sentinela.camtv.data.onvif.OnvifRepository
import com.sentinela.camtv.data.security.CredentialCipher
import com.sentinela.camtv.data.update.AppUpdateInstaller
import com.sentinela.camtv.data.update.GitHubReleaseUpdateRepository
import com.sentinela.camtv.data.update.UpdateRepository
import com.sentinela.camtv.logging.CrashReporter
import com.sentinela.camtv.logging.FileTimberTree
import com.sentinela.camtv.logging.LogRepository
import com.sentinela.camtv.logging.LocalLogRepository
import com.sentinela.camtv.player.Media3RtspConnectionTester
import com.sentinela.camtv.player.RtspConnectionTester
import com.sentinela.camtv.preferences.SettingsRepository
import com.sentinela.camtv.preferences.playerPreferencesRepository
import com.sentinela.camtv.ui.cameras.RtspCameraDraftRepository
import com.sentinela.camtv.ui.cameras.rtspCameraDraftRepository
import com.sentinela.onvif.OnvifSoapClient

class AppContainer(
    context: Context,
) {
    private val appContext = context.applicationContext

    val database: SentinelaDatabase = Room.databaseBuilder(
        appContext,
        SentinelaDatabase::class.java,
        "sentinela.db",
    )
        .addMigrations(MIGRATION_1_2)
        .build()

    val fileTimberTree: FileTimberTree = FileTimberTree(appContext)
    val crashReporter: CrashReporter = CrashReporter(appContext)

    val logRepository: LogRepository = LocalLogRepository(
        context = appContext,
        fileTimberTree = fileTimberTree,
        crashReporter = crashReporter,
    )

    private val credentialCipher = CredentialCipher()

    val cameraRepository: CameraRepository = RoomCameraRepository(
        cameraDao = database.cameraDao(),
        credentialCipher = credentialCipher,
    )

    val mosaicLayoutRepository: MosaicLayoutRepository = RoomMosaicLayoutRepository(
        mosaicSlotDao = database.mosaicSlotDao(),
    )

    val settingsRepository: SettingsRepository = playerPreferencesRepository(appContext)

    val rtspCameraDraftRepository: RtspCameraDraftRepository =
        rtspCameraDraftRepository(appContext)

    val rtspConnectionTester: RtspConnectionTester = Media3RtspConnectionTester(appContext)

    val updateRepository: UpdateRepository = GitHubReleaseUpdateRepository(appContext)

    val appUpdateInstaller: AppUpdateInstaller = AppUpdateInstaller(appContext)

    val onvifRepository: OnvifRepository = DefaultOnvifRepository(
        wsDiscoveryClient = AndroidWsDiscoveryClient(appContext),
        soapClient = OnvifSoapClient(),
    )

    private companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `mosaic_slots` (
                        `mosaicIndex` INTEGER NOT NULL,
                        `slotIndex` INTEGER NOT NULL,
                        `cameraId` TEXT NOT NULL,
                        PRIMARY KEY(`mosaicIndex`, `slotIndex`),
                        FOREIGN KEY(`cameraId`) REFERENCES `cameras`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_mosaic_slots_cameraId` " +
                        "ON `mosaic_slots` (`cameraId`)",
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `mosaic_slots` (`mosaicIndex`, `slotIndex`, `cameraId`)
                    SELECT 0, `position`, `id`
                    FROM `cameras`
                    WHERE `enabled` = 1 AND `position` >= 0 AND `position` < 15
                    ORDER BY `position` ASC
                    """.trimIndent(),
                )
            }
        }
    }
}
