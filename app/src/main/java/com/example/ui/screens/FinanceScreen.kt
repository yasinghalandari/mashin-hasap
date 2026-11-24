package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.UserPreferences
import com.example.domain.FinanceCalculator
import com.example.domain.PersianFormatter
import kotlin.math.roundToLong

private enum class FinanceMode(val titleFa: String, val icon: ImageVector) {
    LOAN("وام و قسط", Icons.Outlined.AccountBalance),
    GOLD("طلا و اجرت", Icons.Outlined.Diamond),
    VAT("تومان و مالیات", Icons.Outlined.Percent),
    SAVINGS("سود سپرده", Icons.Outlined.Savings)
}

@Composable
fun FinanceScreen(
    prefs: UserPreferences,
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableStateOf(FinanceMode.LOAN) }
    var useToman by rememberSaveable { mutableStateOf(true) }
    val currencyLabel = if (useToman) "تومان" else "ریال"

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
            // Header + Toman / Rial toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ابزارهای مالی ایرانی",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "محاسبه دقیق وام، طلا، مالیات و سپرده",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = useToman,
                        onClick = { useToman = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("unit_toman_button")
                    ) {
                        Text("تومان", style = MaterialTheme.typography.labelMedium)
                    }
                    SegmentedButton(
                        selected = !useToman,
                        onClick = { useToman = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("unit_rial_button")
                    ) {
                        Text("ریال", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Mode selector chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FinanceMode.entries.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { mode = item },
                        label = { Text(item.titleFa) },
                        leadingIcon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.titleFa,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("finance_mode_${item.name.lowercase()}")
                    )
                }
            }

            AnimatedContent(targetState = mode, label = "finance_mode_content") { activeMode ->
                when (activeMode) {
                    FinanceMode.LOAN -> LoanCalculatorSection(prefs.usePersianDigits, currencyLabel)
                    FinanceMode.GOLD -> GoldCalculatorSection(prefs.usePersianDigits, currencyLabel)
                    FinanceMode.VAT -> VatDiscountSection(prefs.usePersianDigits, currencyLabel)
                    FinanceMode.SAVINGS -> SavingsCalculatorSection(prefs.usePersianDigits, currencyLabel)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LoanCalculatorSection(usePersianDigits: Boolean, currencyLabel: String) {
    var amountText by rememberSaveable { mutableStateOf("100000000") }
    var rateText by rememberSaveable { mutableStateOf("23") }
    var monthsText by rememberSaveable { mutableStateOf("36") }

    val amount = PersianFormatter.parseDoubleOrNull(amountText) ?: 0.0
    val rate = PersianFormatter.parseDoubleOrNull(rateText) ?: 0.0
    val months = (PersianFormatter.parseDoubleOrNull(monthsText) ?: 0.0).toInt()
    val summary = remember(amount, rate, months) {
        FinanceCalculator.calculateLoan(amount, rate, months)
    }

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
                NumericInputField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "مبلغ اصل وام ($currencyLabel)",
                    testTag = "loan_amount_input"
                )
                // Quick presets
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "۵۰ میلیون" to "50000000",
                        "۱۰۰ میلیون" to "100000000",
                        "۳۰۰ میلیون" to "300000000",
                        "۵۰۰ میلیون" to "500000000"
                    ).forEach { (label, raw) ->
                        FilterChip(
                            selected = amountText == raw,
                            onClick = { amountText = raw },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NumericInputField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = "نرخ سود سالانه (٪)",
                        modifier = Modifier.weight(1f),
                        testTag = "loan_rate_input"
                    )
                    NumericInputField(
                        value = monthsText,
                        onValueChange = { monthsText = it },
                        label = "مدت (ماه)",
                        modifier = Modifier.weight(1f),
                        testTag = "loan_months_input"
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("قرض‌الحسنه ۴٪" to "4", "بانکی ۱۸٪" to "18", "بانکی ۲۳٪" to "23").forEach { (label, r) ->
                        FilterChip(
                            selected = rateText == r,
                            onClick = { rateText = r },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }

        if (summary != null) {
            ResultSummaryCard(
                highlightLabel = "مبلغ هر قسط ماهانه",
                highlightValue = "${PersianFormatter.formatCurrency(summary.monthlyPayment, usePersianDigits)} $currencyLabel",
                wordsCaption = "${PersianFormatter.numberToPersianWords(summary.monthlyPayment.roundToLong())} $currencyLabel",
                rows = listOf(
                    "کل بازپرداخت" to "${PersianFormatter.formatCurrency(summary.totalPayment, usePersianDigits)} $currencyLabel",
                    "مجموع سود بانکی" to "${PersianFormatter.formatCurrency(summary.totalInterest, usePersianDigits)} $currencyLabel"
                )
            ) {
                // Visual principal vs interest split bar
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "اصل وام (${PersianFormatter.localizeDigits("${(summary.principalRatio * 100).toInt()}%", usePersianDigits)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "سود (${PersianFormatter.localizeDigits("${100 - (summary.principalRatio * 100).toInt()}%", usePersianDigits)})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(summary.principalRatio)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoldCalculatorSection(usePersianDigits: Boolean, currencyLabel: String) {
    var weightText by rememberSaveable { mutableStateOf("4.5") }
    var gramPriceText by rememberSaveable { mutableStateOf("4500000") }
    var usePercentMaking by rememberSaveable { mutableStateOf(true) }
    var makingFeeText by rememberSaveable { mutableStateOf("12") }
    var profitText by rememberSaveable { mutableStateOf("7") }
    var vatText by rememberSaveable { mutableStateOf("10") }

    val weight = PersianFormatter.parseDoubleOrNull(weightText) ?: 0.0
    val gramPrice = PersianFormatter.parseDoubleOrNull(gramPriceText) ?: 0.0
    val makingVal = PersianFormatter.parseDoubleOrNull(makingFeeText) ?: 0.0
    val profit = PersianFormatter.parseDoubleOrNull(profitText) ?: 0.0
    val vat = PersianFormatter.parseDoubleOrNull(vatText) ?: 0.0

    val gold = remember(weight, gramPrice, usePercentMaking, makingVal, profit, vat) {
        FinanceCalculator.calculateGoldPrice(
            weightGrams = weight,
            pricePerGram18k = gramPrice,
            makingFeePercent = if (usePercentMaking) makingVal else 0.0,
            makingFeeFixedPerGram = if (usePercentMaking) 0.0 else makingVal,
            usePercentMakingFee = usePercentMaking,
            sellerProfitPercent = profit,
            vatPercent = vat
        )
    }

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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NumericInputField(
                        value = weightText,
                        onValueChange = { weightText = it },
                        label = "وزن طلا (گرم)",
                        modifier = Modifier.weight(1f),
                        testTag = "gold_weight_input"
                    )
                    NumericInputField(
                        value = gramPriceText,
                        onValueChange = { gramPriceText = it },
                        label = "قیمت هر گرم ۱۸ عیار",
                        modifier = Modifier.weight(1.4f),
                        testTag = "gold_gram_price_input"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = usePercentMaking,
                        onClick = { usePercentMaking = true; makingFeeText = "12" },
                        label = { Text("اجرت درصدی (٪)") }
                    )
                    FilterChip(
                        selected = !usePercentMaking,
                        onClick = { usePercentMaking = false; makingFeeText = "400000" },
                        label = { Text("اجرت هر گرم ($currencyLabel)") }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumericInputField(
                        value = makingFeeText,
                        onValueChange = { makingFeeText = it },
                        label = if (usePercentMaking) "اجرت (٪)" else "اجرت/گرم",
                        modifier = Modifier.weight(1f),
                        testTag = "gold_making_input"
                    )
                    NumericInputField(
                        value = profitText,
                        onValueChange = { profitText = it },
                        label = "سود فروش (٪)",
                        modifier = Modifier.weight(1f),
                        testTag = "gold_profit_input"
                    )
                    NumericInputField(
                        value = vatText,
                        onValueChange = { vatText = it },
                        label = "مالیات (٪)",
                        modifier = Modifier.weight(1f),
                        testTag = "gold_vat_input"
                    )
                }
            }
        }

        if (gold != null) {
            ResultSummaryCard(
                highlightLabel = "قیمت نهایی خرید طلا",
                highlightValue = "${PersianFormatter.formatCurrency(gold.totalPrice, usePersianDigits)} $currencyLabel",
                wordsCaption = "${PersianFormatter.numberToPersianWords(gold.totalPrice.roundToLong())} $currencyLabel",
                rows = listOf(
                    "ارزش طلای خام" to "${PersianFormatter.formatCurrency(gold.rawGoldPrice, usePersianDigits)} $currencyLabel",
                    "اجرت ساخت" to "${PersianFormatter.formatCurrency(gold.makingFeeAmount, usePersianDigits)} $currencyLabel",
                    "سود فروشنده" to "${PersianFormatter.formatCurrency(gold.profitAmount, usePersianDigits)} $currencyLabel",
                    "مالیات ارزش افزوده (روی اجرت و سود)" to "${PersianFormatter.formatCurrency(gold.vatAmount, usePersianDigits)} $currencyLabel"
                )
            )
        }
    }
}

@Composable
private fun VatDiscountSection(usePersianDigits: Boolean, currencyLabel: String) {
    var amountText by rememberSaveable { mutableStateOf("2500000") }
    var discountText by rememberSaveable { mutableStateOf("15") }
    var vatText by rememberSaveable { mutableStateOf("10") }
    var splitCount by rememberSaveable { mutableIntStateOf(2) }

    val amount = PersianFormatter.parseDoubleOrNull(amountText) ?: 0.0
    val discount = PersianFormatter.parseDoubleOrNull(discountText) ?: 0.0
    val vat = PersianFormatter.parseDoubleOrNull(vatText) ?: 0.0
    val res = remember(amount, discount, vat, splitCount) {
        FinanceCalculator.calculateVatAndDiscount(amount, discount, vat, splitCount)
    }

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
                NumericInputField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = "مبلغ کل فاکتور ($currencyLabel)",
                    testTag = "vat_amount_input"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NumericInputField(
                        value = discountText,
                        onValueChange = { discountText = it },
                        label = "تخفیف (٪)",
                        modifier = Modifier.weight(1f),
                        testTag = "vat_discount_input"
                    )
                    NumericInputField(
                        value = vatText,
                        onValueChange = { vatText = it },
                        label = "ارزش افزوده (٪)",
                        modifier = Modifier.weight(1f),
                        testTag = "vat_percent_input"
                    )
                }

                // Split bill (دنگ)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تقسیم دنگ بین چند نفر:", style = MaterialTheme.typography.bodyLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = { if (splitCount > 1) splitCount-- },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("−", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                        Text(
                            text = PersianFormatter.localizeDigits("$splitCount نفر", usePersianDigits),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            onClick = { if (splitCount < 50) splitCount++ },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "+",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (res != null) {
            val altUnitLabel = if (currencyLabel == "تومان") "ریال" else "تومان"
            val altValue = if (currencyLabel == "تومان") res.finalTotal * 10.0 else res.finalTotal / 10.0
            ResultSummaryCard(
                highlightLabel = "مبلغ نهایی قابل پرداخت",
                highlightValue = "${PersianFormatter.formatCurrency(res.finalTotal, usePersianDigits)} $currencyLabel",
                wordsCaption = "${PersianFormatter.numberToPersianWords(res.finalTotal.roundToLong())} $currencyLabel",
                rows = listOf(
                    "معادل به $altUnitLabel" to "${PersianFormatter.formatCurrency(altValue, usePersianDigits)} $altUnitLabel",
                    "سود شما از تخفیف" to "${PersianFormatter.formatCurrency(res.discountAmount, usePersianDigits)} $currencyLabel",
                    "مالیات ارزش افزوده" to "${PersianFormatter.formatCurrency(res.vatAmount, usePersianDigits)} $currencyLabel",
                    "سهم هر نفر (دنگ)" to "${PersianFormatter.formatCurrency(res.perPersonShare, usePersianDigits)} $currencyLabel"
                )
            )
        }
    }
}

@Composable
private fun SavingsCalculatorSection(usePersianDigits: Boolean, currencyLabel: String) {
    var initialText by rememberSaveable { mutableStateOf("50000000") }
    var monthlyText by rememberSaveable { mutableStateOf("5000000") }
    var rateText by rememberSaveable { mutableStateOf("22") }
    var monthsText by rememberSaveable { mutableStateOf("12") }

    val initial = PersianFormatter.parseDoubleOrNull(initialText) ?: 0.0
    val monthly = PersianFormatter.parseDoubleOrNull(monthlyText) ?: 0.0
    val rate = PersianFormatter.parseDoubleOrNull(rateText) ?: 0.0
    val months = (PersianFormatter.parseDoubleOrNull(monthsText) ?: 0.0).toInt()

    val res = remember(initial, monthly, rate, months) {
        FinanceCalculator.calculateSavings(initial, monthly, rate, months)
    }

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
                NumericInputField(
                    value = initialText,
                    onValueChange = { initialText = it },
                    label = "سپرده اولیه ($currencyLabel)",
                    testTag = "savings_initial_input"
                )
                NumericInputField(
                    value = monthlyText,
                    onValueChange = { monthlyText = it },
                    label = "افزایش ماهانه ($currencyLabel)",
                    testTag = "savings_monthly_input"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NumericInputField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = "نرخ سود سالانه (٪)",
                        modifier = Modifier.weight(1f),
                        testTag = "savings_rate_input"
                    )
                    NumericInputField(
                        value = monthsText,
                        onValueChange = { monthsText = it },
                        label = "مدت (ماه)",
                        modifier = Modifier.weight(1f),
                        testTag = "savings_months_input"
                    )
                }
            }
        }

        if (res != null) {
            ResultSummaryCard(
                highlightLabel = "موجودی نهایی در پایان دوره",
                highlightValue = "${PersianFormatter.formatCurrency(res.finalBalance, usePersianDigits)} $currencyLabel",
                wordsCaption = "${PersianFormatter.numberToPersianWords(res.finalBalance.roundToLong())} $currencyLabel",
                rows = listOf(
                    "کل مبلغ واریزی شما" to "${PersianFormatter.formatCurrency(res.totalDeposited, usePersianDigits)} $currencyLabel",
                    "سود مرکب کسب‌شده" to "${PersianFormatter.formatCurrency(res.totalProfit, usePersianDigits)} $currencyLabel"
                )
            )
        }
    }
}

@Composable
fun NumericInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = PersianFormatter.normalizeToLatin(raw)
                .filter { it.isDigit() || it == '.' }
            onValueChange(filtered)
        },
        label = { Text(label, maxLines = 1) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
    )
}

@Composable
private fun ResultSummaryCard(
    highlightLabel: String,
    highlightValue: String,
    wordsCaption: String,
    rows: List<Pair<String, String>>,
    extraContent: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = highlightLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = highlightValue,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (wordsCaption.isNotEmpty()) {
                Text(
                    text = wordsCaption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            rows.forEach { (title, valStr) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = valStr,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (extraContent != null) {
                extraContent()
            }
        }
    }
}
