package com.sentinela.camtv.ui.settings

import java.math.BigDecimal
import java.math.RoundingMode

internal fun annualSavingsText(
    monthlyPrice: String?,
    annualPrice: String?,
): String? {
    val monthly = monthlyPrice?.toParsedPrice() ?: return null
    val annual = annualPrice?.toParsedPrice() ?: return null
    val monthlyYear = monthly.amount.multiply(BigDecimal(12))
    val savings = monthlyYear.subtract(annual.amount)
    if (savings <= BigDecimal.ZERO) return null

    val percent = savings
        .multiply(BigDecimal(100))
        .divide(monthlyYear, 0, RoundingMode.HALF_UP)
        .toInt()
    val amount = annual.formatLike(savings)
    return "Economia aproximada: $percent% ($amount por ano)."
}

private data class ParsedPrice(
    val amount: BigDecimal,
    val prefix: String,
    val decimalSeparator: Char,
) {
    fun formatLike(value: BigDecimal): String {
        val normalized = value.setScale(2, RoundingMode.HALF_UP).toPlainString()
        val localized = if (decimalSeparator == ',') {
            normalized.replace('.', ',')
        } else {
            normalized
        }
        return "$prefix$localized".trim()
    }
}

private val priceNumberRegex = Regex("""\d+(?:[.,]\d{1,2})?""")

private fun String.toParsedPrice(): ParsedPrice? {
    val match = priceNumberRegex.find(this) ?: return null
    val rawNumber = match.value
    val decimalSeparator = if (rawNumber.contains(',')) ',' else '.'
    val amount = rawNumber.replace(',', '.').toBigDecimalOrNull() ?: return null
    return ParsedPrice(
        amount = amount,
        prefix = substring(0, match.range.first),
        decimalSeparator = decimalSeparator,
    )
}
