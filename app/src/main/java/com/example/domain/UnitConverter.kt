package com.example.domain

enum class UnitCategory(val titleFa: String) {
    LENGTH("طول"),
    WEIGHT("وزن و مثقال"),
    AREA("مساحت"),
    TEMPERATURE("دما"),
    VOLUME("حجم"),
    DATA("حجم داده"),
    SPEED("سرعت"),
    TIME("زمان")
}

data class UnitDefinition(
    val id: String,
    val nameFa: String,
    val symbol: String,
    val toBaseFactor: Double // value * toBaseFactor = value in base unit
)

object UnitConverterEngine {

    fun unitsFor(category: UnitCategory): List<UnitDefinition> = when (category) {
        UnitCategory.LENGTH -> listOf(
            UnitDefinition("m", "متر", "m", 1.0),
            UnitDefinition("km", "کیلومتر", "km", 1000.0),
            UnitDefinition("cm", "سانتی‌متر", "cm", 0.01),
            UnitDefinition("mm", "میلی‌متر", "mm", 0.001),
            UnitDefinition("inch", "اینچ", "in", 0.0254),
            UnitDefinition("ft", "فوت", "ft", 0.3048),
            UnitDefinition("yd", "یارد", "yd", 0.9144),
            UnitDefinition("mi", "مایل", "mi", 1609.344)
        )
        UnitCategory.WEIGHT -> listOf(
            UnitDefinition("kg", "کیلوگرم", "kg", 1000.0),
            UnitDefinition("g", "گرم", "g", 1.0),
            UnitDefinition("mg", "میلی‌گرم", "mg", 0.001),
            UnitDefinition("soot", "سوت (طلا)", "سوت", 0.001),
            UnitDefinition("mesghal", "مثقال صیرفی", "مثقال", 4.608),
            UnitDefinition("ton", "تن متریک", "t", 1_000_000.0),
            UnitDefinition("lb", "پوند", "lb", 453.59237),
            UnitDefinition("oz", "اونس", "oz", 28.349523125)
        )
        UnitCategory.AREA -> listOf(
            UnitDefinition("sqm", "متر مربع", "m²", 1.0),
            UnitDefinition("ha", "هکتار", "ha", 10_000.0),
            UnitDefinition("sqkm", "کیلومتر مربع", "km²", 1_000_000.0),
            UnitDefinition("sqcm", "سانتی‌متر مربع", "cm²", 0.0001),
            UnitDefinition("sqft", "فوت مربع", "ft²", 0.09290304),
            UnitDefinition("acre", "ایکر", "ac", 4046.8564224)
        )
        UnitCategory.TEMPERATURE -> listOf(
            UnitDefinition("c", "سلسیوس", "°C", 1.0),
            UnitDefinition("f", "فارنهایت", "°F", 1.0),
            UnitDefinition("k", "کلوین", "K", 1.0)
        )
        UnitCategory.VOLUME -> listOf(
            UnitDefinition("l", "لیتر", "L", 1.0),
            UnitDefinition("ml", "میلی‌لیتر (سی‌سی)", "mL", 0.001),
            UnitDefinition("m3", "متر مکعب", "m³", 1000.0),
            UnitDefinition("gal", "گالن (US)", "gal", 3.785411784),
            UnitDefinition("cup", "پیمانه", "cup", 0.24)
        )
        UnitCategory.DATA -> listOf(
            UnitDefinition("b", "بایت", "B", 1.0),
            UnitDefinition("kb", "کیلوبایت", "KB", 1024.0),
            UnitDefinition("mb", "مگابایت", "MB", 1024.0 * 1024.0),
            UnitDefinition("gb", "گیگابایت", "GB", 1024.0 * 1024.0 * 1024.0),
            UnitDefinition("tb", "ترابایت", "TB", 1024.0 * 1024.0 * 1024.0 * 1024.0),
            UnitDefinition("bit", "بیت", "bit", 0.125)
        )
        UnitCategory.SPEED -> listOf(
            UnitDefinition("kmh", "کیلومتر بر ساعت", "km/h", 1.0 / 3.6),
            UnitDefinition("ms", "متر بر ثانیه", "m/s", 1.0),
            UnitDefinition("mph", "مایل بر ساعت", "mph", 0.44704),
            UnitDefinition("knot", "گره دریایی", "kn", 0.514444444)
        )
        UnitCategory.TIME -> listOf(
            UnitDefinition("sec", "ثانیه", "s", 1.0),
            UnitDefinition("min", "دقیقه", "min", 60.0),
            UnitDefinition("hr", "ساعت", "h", 3600.0),
            UnitDefinition("day", "روز", "d", 86_400.0),
            UnitDefinition("wk", "هفته", "wk", 604_800.0),
            UnitDefinition("yr", "سال (۳۶۵ روز)", "yr", 31_536_000.0)
        )
    }

    fun convert(
        value: Double,
        category: UnitCategory,
        from: UnitDefinition,
        to: UnitDefinition
    ): Double {
        if (from.id == to.id) return value
        if (category == UnitCategory.TEMPERATURE) {
            val celsius = when (from.id) {
                "c" -> value
                "f" -> (value - 32.0) * 5.0 / 9.0
                "k" -> value - 273.15
                else -> value
            }
            return when (to.id) {
                "c" -> celsius
                "f" -> celsius * 9.0 / 5.0 + 32.0
                "k" -> celsius + 273.15
                else -> celsius
            }
        }
        val baseValue = value * from.toBaseFactor
        return baseValue / to.toBaseFactor
    }
}
