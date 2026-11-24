package com.example.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object PersianFormatter {

    private val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    fun normalizeToLatin(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '۰'..'۹' -> sb.append(('0' + (ch - '۰')))
                in '٠'..'٩' -> sb.append(('0' + (ch - '٠')))
                '٬' -> sb.append(',')
                '٫' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun localizeDigits(input: String, usePersianDigits: Boolean): String {
        if (!usePersianDigits) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '0'..'9' -> sb.append(persianDigits[ch - '0'])
                ',' -> sb.append('٬')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatNumber(
        value: Double,
        usePersianDigits: Boolean,
        maxFractionDigits: Int = 8,
        grouping: Boolean = true
    ): String {
        if (value.isNaN()) return "خطا"
        if (value.isInfinite()) return if (value > 0) "∞" else "-∞"

        val cleaned = if (abs(value) < 1e-12) 0.0 else value
        val absVal = abs(cleaned)

        val formattedLatin = if (absVal >= 1e13 || (absVal > 0 && absVal < 1e-8)) {
            val sciSymbols = DecimalFormatSymbols(Locale.US)
            DecimalFormat("0.######E0", sciSymbols).format(cleaned)
        } else {
            val bd = try {
                BigDecimal.valueOf(cleaned)
                    .setScale(maxFractionDigits, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
            } catch (_: NumberFormatException) {
                BigDecimal.ZERO
            }
            val plain = bd.toPlainString()
            if (!grouping) {
                plain
            } else {
                addGroupingSeparators(plain)
            }
        }

        return localizeDigits(formattedLatin, usePersianDigits)
    }

    fun formatInteger(value: Long, usePersianDigits: Boolean): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#,###", symbols)
        return localizeDigits(df.format(value), usePersianDigits)
    }

    fun formatCurrency(value: Double, usePersianDigits: Boolean): String {
        if (value.isNaN() || value.isInfinite()) return "—"
        return formatInteger(value.roundToLong(), usePersianDigits)
    }

    private fun addGroupingSeparators(plainDecimal: String): String {
        val negative = plainDecimal.startsWith("-")
        val unsigned = if (negative) plainDecimal.substring(1) else plainDecimal
        val parts = unsigned.split('.')
        val intPart = parts[0]
        val fracPart = parts.getOrNull(1)

        val groupedInt = StringBuilder()
        var count = 0
        for (i in intPart.length - 1 downTo 0) {
            groupedInt.append(intPart[i])
            count++
            if (count % 3 == 0 && i != 0) {
                groupedInt.append(',')
            }
        }
        groupedInt.reverse()

        return buildString {
            if (negative) append('-')
            append(groupedInt)
            if (fracPart != null && fracPart.isNotEmpty()) {
                append('.')
                append(fracPart)
            }
        }
    }

    fun formatExpressionForDisplay(expr: String, usePersianDigits: Boolean): String {
        val spaced = expr
            .replace("*", "×")
            .replace("/", "÷")
        return localizeDigits(spaced, usePersianDigits)
    }

    fun parseDoubleOrNull(text: String): Double? {
        val clean = normalizeToLatin(text).replace(",", "").trim()
        if (clean.isEmpty()) return null
        return clean.toDoubleOrNull()
    }

    private val ones = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val teens = arrayOf(
        "ده", "یازده", "دوازده", "سیزده", "چهارده",
        "پانزده", "شانزده", "هفده", "هجده", "نوزده"
    )
    private val tens = arrayOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val hundreds = arrayOf(
        "", "صد", "دویست", "سیصد", "چهارصد", "پانصد",
        "ششصد", "هفتصد", "هشتصد", "نهصد"
    )
    private val scales = arrayOf("", "هزار", "میلیون", "میلیارد", "تریلیون", "کوادریلیون")

    fun numberToPersianWords(value: Long): String {
        if (value == 0L) return "صفر"
        if (value == Long.MIN_VALUE) return "عدد بسیار بزرگ"

        val negative = value < 0
        var n = abs(value)
        val parts = mutableListOf<String>()
        var scaleIndex = 0

        while (n > 0) {
            val chunk = (n % 1000).toInt()
            if (chunk != 0) {
                val chunkWords = threeDigitToWords(chunk)
                val scale = scales.getOrElse(scaleIndex) { "" }
                val combined = if (scale.isNotEmpty()) "$chunkWords $scale" else chunkWords
                parts.add(0, combined)
            }
            n /= 1000
            scaleIndex++
        }

        val joined = parts.joinToString(" و ")
        return if (negative) "منفی $joined" else joined
    }

    private fun threeDigitToWords(n: Int): String {
        val h = n / 100
        val rem = n % 100
        val parts = mutableListOf<String>()
        if (h > 0) parts.add(hundreds[h])
        if (rem in 10..19) {
            parts.add(teens[rem - 10])
        } else {
            val t = rem / 10
            val o = rem % 10
            if (t > 0) parts.add(tens[t])
            if (o > 0) parts.add(ones[o])
        }
        return parts.joinToString(" و ")
    }
}
