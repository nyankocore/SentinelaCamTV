package com.sentinela.camtv

import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import com.sentinela.camtv.localization.AppLanguage
import com.sentinela.camtv.localization.appLayoutDirection
import com.sentinela.camtv.localization.withAppLanguage
import com.sentinela.camtv.preferences.PlayerUiPreferences
import com.sentinela.camtv.ui.app.SentinelaAppScreen
import com.sentinela.camtv.ui.theme.SentinelaCamTVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val application = applicationContext as SentinelaApplication
            val preferences by application.container.settingsRepository
                .observePreferences()
                .collectAsState(initial = PlayerUiPreferences())
            val appLanguage = AppLanguage.fromTag(preferences.appLanguageTag)
            val localizedContext = remember(this@MainActivity, appLanguage) {
                this@MainActivity.withAppLanguage(appLanguage)
            }
            val localizedConfiguration = remember(localizedContext) {
                Configuration(localizedContext.resources.configuration)
            }
            val layoutDirection = remember(appLanguage, localizedConfiguration) {
                localizedConfiguration.appLayoutDirection(appLanguage)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
                LocalLayoutDirection provides layoutDirection,
            ) {
                SentinelaCamTVTheme {
                    SentinelaAppScreen()
                }
            }
        }
    }
}
