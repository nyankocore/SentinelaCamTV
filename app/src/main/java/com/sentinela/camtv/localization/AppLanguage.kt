package com.sentinela.camtv.localization

import androidx.annotation.StringRes
import com.sentinela.camtv.R
import java.util.Locale

enum class AppLanguage(
    val tag: String?,
    @param:StringRes val labelRes: Int,
) {
    System(
        tag = null,
        labelRes = R.string.language_system,
    ),
    PortugueseBrazil(
        tag = "pt-BR",
        labelRes = R.string.language_portuguese_brazil,
    ),
    English(
        tag = "en",
        labelRes = R.string.language_english,
    ),
    Spanish(
        tag = "es",
        labelRes = R.string.language_spanish,
    ),
    Turkish(
        tag = "tr",
        labelRes = R.string.language_turkish,
    ),
    Arabic(
        tag = "ar",
        labelRes = R.string.language_arabic,
    ),
    Russian(
        tag = "ru",
        labelRes = R.string.language_russian,
    );

    val storageTag: String?
        get() = tag

    val isExplicitRtl: Boolean
        get() = this == Arabic

    fun localeOrNull(): Locale? = tag?.let(Locale::forLanguageTag)

    companion object {
        val selectable: List<AppLanguage> = entries.toList()

        fun fromTag(tag: String?): AppLanguage {
            val normalized = tag?.trim().orEmpty()
            if (normalized.isBlank() || normalized.equals("system", ignoreCase = true)) {
                return System
            }
            return entries.firstOrNull { language ->
                language.tag.equals(normalized, ignoreCase = true)
            } ?: System
        }
    }
}
