package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.domain.GregorianDate
import com.example.domain.JalaliCalendar
import com.example.domain.JalaliDate
import com.example.domain.PersianFormatter

private enum class ToolSubMode(val titleFa: String) {
    JALALI_AND_AGE("تقویم شمسی و سن"),
    HEALTH_BMI("شاخص توده بدنی (BMI)")
}

@Composable
fun ToolsScreen(
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableStateOf(ToolSubMode.JALALI_AND_AGE) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    text = "تقویم شمسی و سلامت",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "تبدیل تاریخ، محاسبه سن دقیق و شاخص سلامت",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ToolSubMode.entries.forEachIndexed { idx, item ->
                    SegmentedButton(
                        selected = mode == item,
                        onClick = { mode = item },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = ToolSubMode.entries.size),
                        icon = {
                            Icon(
                                imageVector = if (item == ToolSubMode.JALALI_AND_AGE) {
                                    Icons.Outlined.CalendarMonth
                                } else {
                                    Icons.Outlined.MonitorWeight
                                },
                                contentDescription = item.titleFa,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("tool_mode_${item.name.lowercase()}")
                    ) {
                        Text(item.titleFa, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            AnimatedContent(targetState = mode, label = "tool_submode") { active ->
                when (active) {
                    ToolSubMode.JALALI_AND_AGE -> JalaliAndAgeSection(prefs.usePersianDigits)
                    ToolSubMode.HEALTH_BMI -> HealthBmiSection(prefs.usePersianDigits)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun JalaliAndAgeSection(usePersianDigits: Boolean) {
    val todayJ = remember { JalaliCalendar.todayJalali() }
    val todayG = remember { JalaliCalendar.todayGregorian() }
    val todayWeekday = remember { JalaliCalendar.weekdayNamePersian(todayG) }

    var jalaliToGregorian by rememberSaveable { mutableStateOf(true) }

    var inputYearText by rememberSaveable { mutableStateOf("1378") }
    var inputMonth by rememberSaveable { mutableIntStateOf(1) }
    var inputDayText by rememberSaveable { mutableStateOf("15") }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val year = (PersianFormatter.parseDoubleOrNull(inputYearText)?.toInt() ?: 1378).coerceIn(1200, 2200)
    val maxDays = if (jalaliToGregorian) {
        JalaliCalendar.jalaliMonthDays(year, inputMonth)
    } else {
        JalaliCalendar.gregorianMonthDays(year, inputMonth)
    }
    val day = (PersianFormatter.parseDoubleOrNull(inputDayText)?.toInt() ?: 1).coerceIn(1, maxDays)

    val convertedGregorian: GregorianDate
    val convertedJalali: JalaliDate
    if (jalaliToGregorian) {
        convertedJalali = JalaliDate(year, inputMonth, day)
        convertedGregorian = JalaliCalendar.jalaliToGregorian(year, inputMonth, day)
    } else {
        convertedGregorian = GregorianDate(year, inputMonth, day)
        convertedJalali = JalaliCalendar.gregorianToJalali(year, inputMonth, day)
    }

    val weekday = remember(convertedGregorian) { JalaliCalendar.weekdayNamePersian(convertedGregorian) }
    val isLeap = remember(convertedJalali.year) { JalaliCalendar.isJalaliLeapYear(convertedJalali.year) }
    val age = remember(convertedJalali, todayJ) {
        JalaliCalendar.calculateAge(convertedJalali, todayJ)
    }

    val d: (String) -> String = { PersianFormatter.localizeDigits(it, usePersianDigits) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Today Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "امروز ($todayWeekday)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = d("${todayJ.day} ${JalaliCalendar.jalaliMonths[todayJ.month - 1]} ${todayJ.year}"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = d("${todayG.year}/${todayG.month.toString().padStart(2, '0')}/${todayG.day.toString().padStart(2, '0')}"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        // Direction Toggle + Date Input
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = jalaliToGregorian,
                        onClick = {
                            jalaliToGregorian = true
                            inputYearText = "1378"
                            inputMonth = 1
                            inputDayText = "15"
                        },
                        label = { Text("شمسی به میلادی") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("dir_jalali_to_greg")
                    )
                    FilterChip(
                        selected = !jalaliToGregorian,
                        onClick = {
                            jalaliToGregorian = false
                            inputYearText = "1999"
                            inputMonth = 4
                            inputDayText = "4"
                        },
                        label = { Text("میلادی به شمسی") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("dir_greg_to_jalali")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumericInputField(
                        value = inputYearText,
                        onValueChange = { if (it.length <= 4) inputYearText = it },
                        label = "سال",
                        modifier = Modifier.weight(1.1f),
                        testTag = "date_year_input"
                    )

                    Box(modifier = Modifier.weight(1.3f)) {
                        val monthLabel = if (jalaliToGregorian) {
                            JalaliCalendar.jalaliMonths[inputMonth - 1]
                        } else {
                            JalaliCalendar.gregorianMonthsFa[inputMonth - 1]
                        }
                        Surface(
                            onClick = { showMonthDropdown = true },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("date_month_selector")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text(
                                    text = monthLabel,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false }
                        ) {
                            val list = if (jalaliToGregorian) {
                                JalaliCalendar.jalaliMonths
                            } else {
                                JalaliCalendar.gregorianMonthsFa
                            }
                            list.forEachIndexed { index, mName ->
                                DropdownMenuItem(
                                    text = { Text("${d((index + 1).toString())} - $mName") },
                                    onClick = {
                                        inputMonth = index + 1
                                        showMonthDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    NumericInputField(
                        value = inputDayText,
                        onValueChange = { if (it.length <= 2) inputDayText = it },
                        label = "روز",
                        modifier = Modifier.weight(0.9f),
                        testTag = "date_day_input"
                    )
                }
            }
        }

        // Conversion Result Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (jalaliToGregorian) "معادل تاریخ میلادی" else "معادل تاریخ هجری شمسی",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val mainResult = if (jalaliToGregorian) {
                    d("${convertedGregorian.day} ${JalaliCalendar.gregorianMonthsFa[convertedGregorian.month - 1]} ${convertedGregorian.year}")
                } else {
                    d("${convertedJalali.day} ${JalaliCalendar.jalaliMonths[convertedJalali.month - 1]} ${convertedJalali.year}")
                }

                Text(
                    text = mainResult,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("date_conversion_result")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "روز هفته: $weekday",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = if (isLeap) "سال شمسی کبیسه است" else "سال شمسی عادی",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (age != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    Text(
                        text = "محاسبه سن دقیق (اگر این تاریخ تولد شما باشد):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = d("${age.years} سال و ${age.months} ماه و ${age.days} روز"),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("age_result_text")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "کل روزهای سپری‌شده: ${PersianFormatter.formatInteger(age.totalDays, usePersianDigits)} روز",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (age.daysToNextBirthday == 0) {
                            "امروز تولدتان است! تبریک"
                        } else {
                            d("${age.daysToNextBirthday} روز تا تولد بعدی")
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthBmiSection(usePersianDigits: Boolean) {
    var weightKg by rememberSaveable { mutableFloatStateOf(72f) }
    var heightCm by rememberSaveable { mutableFloatStateOf(175f) }

    val bmiResult = remember(weightKg, heightCm) {
        JalaliCalendar.calculateBmi(weightKg.toDouble(), heightCm.toDouble())
    }
    val d: (String) -> String = { PersianFormatter.localizeDigits(it, usePersianDigits) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("وزن", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = d("${weightKg.toInt()} کیلوگرم"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    valueRange = 30f..160f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("bmi_weight_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("قد", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = d("${heightCm.toInt()} سانتی‌متر"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Slider(
                    value = heightCm,
                    onValueChange = { heightCm = it },
                    valueRange = 120f..220f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("bmi_height_slider")
                )
            }
        }

        if (bmiResult != null) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "شاخص توده بدنی (BMI)",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = PersianFormatter.formatNumber(bmiResult.bmi, usePersianDigits, maxFractionDigits = 1),
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("bmi_value_text")
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = bmiResult.categoryFa,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Canvas BMI Spectrum Bar + Marker
                    val markerColor = MaterialTheme.colorScheme.onSurface
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    ) {
                        val barTop = size.height * 0.3f
                        val barHeight = size.height * 0.4f
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF38BDF8), // Underweight
                                    Color(0xFF2DD4BF), // Normal
                                    Color(0xFFFBBF24), // Overweight
                                    Color(0xFFFB7185)  // Obese
                                )
                            ),
                            topLeft = Offset(0f, barTop),
                            size = Size(size.width, barHeight),
                            cornerRadius = CornerRadius(barHeight / 2f, barHeight / 2f)
                        )
                        val markerX = (size.width * bmiResult.progressFraction).coerceIn(10f, size.width - 10f)
                        drawCircle(
                            color = markerColor,
                            radius = 10.dp.toPx(),
                            center = Offset(markerX, size.height / 2f)
                        )
                    }

                    Text(
                        text = "محدوده وزن ایده‌آل برای قد شما: ${
                            PersianFormatter.formatNumber(bmiResult.idealMinKg, usePersianDigits, 1)
                        } تا ${
                            PersianFormatter.formatNumber(bmiResult.idealMaxKg, usePersianDigits, 1)
                        } کیلوگرم",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
