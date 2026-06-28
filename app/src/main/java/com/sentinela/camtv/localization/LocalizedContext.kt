package com.sentinela.camtv.localization

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.text.TextUtils
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

fun Context.withAppLanguage(language: AppLanguage): Context {
    val locale = language.localeOrNull() ?: return this
    val configuration = Configuration(resources.configuration)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.setLocales(LocaleList(locale))
    } else {
        @Suppress("DEPRECATION")
        configuration.setLocale(locale)
    }
    return createConfigurationContext(configuration)
}

fun Configuration.appLayoutDirection(language: AppLanguage): LayoutDirection {
    if (language.isExplicitRtl) {
        return LayoutDirection.Rtl
    }
    if (language != AppLanguage.System) {
        return LayoutDirection.Ltr
    }
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        locales.get(0)
    } else {
        @Suppress("DEPRECATION")
        this.locale
    } ?: Locale.getDefault()
    return if (TextUtils.getLayoutDirectionFromLocale(locale) == android.view.View.LAYOUT_DIRECTION_RTL) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
}
