package com.sentinela.camtv.ui.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

sealed interface UiText {
    data class Resource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class Plural(
        @PluralsRes val id: Int,
        val quantity: Int,
        val args: List<Any> = emptyList(),
    ) : UiText

    data class Raw(
        val value: String,
    ) : UiText
}

@Composable
fun UiText.asString(): String =
    when (this) {
        is UiText.Resource -> stringResource(id, *args.toTypedArray())
        is UiText.Plural -> pluralStringResource(id, quantity, *args.toTypedArray())
        is UiText.Raw -> value
    }

fun UiText.asString(context: Context): String =
    when (this) {
        is UiText.Resource -> context.getString(id, *args.toTypedArray())
        is UiText.Plural -> context.resources.getQuantityString(id, quantity, *args.toTypedArray())
        is UiText.Raw -> value
    }
