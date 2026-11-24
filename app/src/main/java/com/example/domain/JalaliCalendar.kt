package com.example.domain

import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.math.pow

data class JalaliDate(val year: Int, val month: Int, val day: Int)

data class GregorianDate(val year: Int, val month: Int, val day: Int)

data class AgeBreakdown(
    val years: Int,
    val months: Int,
    val days: Int,
    val totalDays: Long,
    val daysToNextBirthday: Int
)

data class BmiResult(
    val bmi: Double,
    val categoryFa: String,
    val idealMinKg: Double,
    val idealMaxKg: Double,
    val progressFraction: Float
)

object JalaliCalendar {

    val jalaliMonths = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    val gregorianMonthsFa = listOf(
        "ژانویه", "فوریه", "مارس",
        "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر",
        "اکتبر", "نوامبر", "دسامبر"
    )

    private val persianWeekdays = arrayOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
    )

    fun todayJalali(): JalaliDate {
        val cal = Calendar.getInstance()
        return gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun todayGregorian(): GregorianDate {
        val cal = Calendar.getInstance()
        return GregorianDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun isJalaliLeapYear(jy: Int): Boolean {
        val breaks = intArrayOf(
            -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181,
            1210, 1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
        )
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until breaks.size) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            jp = jm
        }
        var n = jy - jp
        if (jump - n < 6) n = n - jump + ((jump + 4) / 33) * 33
        var leap = ((n + 1) % 33 - 1) % 4
        if (leap == -1) leap = 4
        return leap == 0
    }

    fun jalaliMonthDays(jy: Int, jm: Int): Int = when (jm) {
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> if (isJalaliLeapYear(jy)) 30 else 29
        else -> 30
    }

    fun gregorianMonthDays(gy: Int, gm: Int): Int {
        val isLeap = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        return when (gm) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeap) 29 else 28
            else -> 30
        }
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gdm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) +
            ((gy2 + 399) / 400) + gd + gdm[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return JalaliDate(jy, jm, jd)
    }

    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): GregorianDate {
        val jyAdj = jy + 1595
        var days = -355668 + (365 * jyAdj) + ((jyAdj / 33) * 8) +
            (((jyAdj % 33) + 3) / 4) + jd +
            if (jm < 7) (jm - 1) * 31 else ((jm - 7) * 30) + 186
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val salA = intArrayOf(
            0, 31,
            if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return GregorianDate(gy, gm, gd)
    }

    fun weekdayNamePersian(g: GregorianDate): String {
        val cal = GregorianCalendar(g.year, g.month - 1, g.day)
        val dow = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday .. 7 = Saturday
        return persianWeekdays[(dow - 1).coerceIn(0, 6)]
    }

    fun calculateAge(birthJalali: JalaliDate, todayJalali: JalaliDate = todayJalali()): AgeBreakdown? {
        val birthG = jalaliToGregorian(birthJalali.year, birthJalali.month, birthJalali.day)
        val todayG = jalaliToGregorian(todayJalali.year, todayJalali.month, todayJalali.day)

        val birthCal = GregorianCalendar(birthG.year, birthG.month - 1, birthG.day)
        val todayCal = GregorianCalendar(todayG.year, todayG.month - 1, todayG.day)
        if (birthCal.after(todayCal)) return null

        val diffMillis = todayCal.timeInMillis - birthCal.timeInMillis
        val totalDays = diffMillis / (24L * 60L * 60L * 1000L)

        var years = todayJalali.year - birthJalali.year
        var months = todayJalali.month - birthJalali.month
        var days = todayJalali.day - birthJalali.day

        if (days < 0) {
            months--
            val prevMonth = if (todayJalali.month == 1) 12 else todayJalali.month - 1
            val prevMonthYear = if (todayJalali.month == 1) todayJalali.year - 1 else todayJalali.year
            days += jalaliMonthDays(prevMonthYear, prevMonth)
        }
        if (months < 0) {
            years--
            months += 12
        }

        // Next birthday
        var nextBYear = todayJalali.year
        if (birthJalali.month < todayJalali.month ||
            (birthJalali.month == todayJalali.month && birthJalali.day < todayJalali.day)
        ) {
            nextBYear++
        }
        val maxDayInMonth = jalaliMonthDays(nextBYear, birthJalali.month)
        val nextBDay = birthJalali.day.coerceAtMost(maxDayInMonth)
        val nextBG = jalaliToGregorian(nextBYear, birthJalali.month, nextBDay)
        val nextBCal = GregorianCalendar(nextBG.year, nextBG.month - 1, nextBG.day)
        val daysToNext = ((nextBCal.timeInMillis - todayCal.timeInMillis) / (24L * 60L * 60L * 1000L)).toInt()

        return AgeBreakdown(
            years = years,
            months = months,
            days = days,
            totalDays = totalDays,
            daysToNextBirthday = daysToNext
        )
    }

    fun calculateBmi(weightKg: Double, heightCm: Double): BmiResult? {
        if (weightKg <= 0 || heightCm <= 0) return null
        val heightM = heightCm / 100.0
        val bmi = weightKg / heightM.pow(2)
        val category = when {
            bmi < 18.5 -> "کمبود وزن"
            bmi < 25.0 -> "وزن ایده‌آل و سالم"
            bmi < 30.0 -> "اضافه وزن"
            bmi < 35.0 -> "چاقی درجه ۱"
            else -> "چاقی درجه ۲+"
        }
        val idealMin = 18.5 * heightM.pow(2)
        val idealMax = 24.9 * heightM.pow(2)
        val fraction = ((bmi - 14.0) / (38.0 - 14.0)).toFloat().coerceIn(0f, 1f)
        return BmiResult(
            bmi = bmi,
            categoryFa = category,
            idealMinKg = idealMin,
            idealMaxKg = idealMax,
            progressFraction = fraction
        )
    }
}
