package com.example

import com.example.domain.AngleUnit
import com.example.domain.EvalResult
import com.example.domain.FinanceCalculator
import com.example.domain.JalaliCalendar
import com.example.domain.JalaliDate
import com.example.domain.MathEvaluator
import com.example.domain.PersianFormatter
import com.example.domain.UnitCategory
import com.example.domain.UnitConverterEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainEnginesTest {

    @Test
    fun mathEvaluator_handlesPrecedenceTrigAndImplicitMult() {
        val r1 = MathEvaluator.evaluate("2+3×4") as EvalResult.Success
        assertEquals(14.0, r1.value, 1e-9)

        val r2 = MathEvaluator.evaluate("2sin(30)+cos(0)", AngleUnit.DEG) as EvalResult.Success
        assertEquals(2.0, r2.value, 1e-9)

        val r3 = MathEvaluator.evaluate("5!") as EvalResult.Success
        assertEquals(120.0, r3.value, 1e-9)

        val r4 = MathEvaluator.evaluate("200×15%") as EvalResult.Success
        assertEquals(30.0, r4.value, 1e-9)
    }

    @Test
    fun persianFormatter_formatsDigitsAndWords() {
        assertEquals("۱۲٬۵۰۰٬۰۰۰", PersianFormatter.formatCurrency(12_500_000.0, true))
        assertEquals("دوازده میلیون و پانصد هزار", PersianFormatter.numberToPersianWords(12_500_000L))
        assertEquals("1234.5", PersianFormatter.normalizeToLatin("۱۲۳۴.۵"))
    }

    @Test
    fun jalaliCalendar_convertsAccurately() {
        // 1403/01/01 (Nowruz 1403) == 2024-03-20
        val g = JalaliCalendar.jalaliToGregorian(1403, 1, 1)
        assertEquals(2024, g.year)
        assertEquals(3, g.month)
        assertEquals(20, g.day)

        val j = JalaliCalendar.gregorianToJalali(2024, 3, 20)
        assertEquals(JalaliDate(1403, 1, 1), j)
        assertTrue(JalaliCalendar.isJalaliLeapYear(1403))
    }

    @Test
    fun financeAndUnitConverters_computeExpectedValues() {
        val loan = FinanceCalculator.calculateLoan(100_000_000.0, 0.0, 10)
        assertNotNull(loan)
        assertEquals(10_000_000.0, loan!!.monthlyPayment, 1e-6)

        val gold = FinanceCalculator.calculateGoldPrice(
            weightGrams = 2.0,
            pricePerGram18k = 4_000_000.0,
            makingFeePercent = 10.0,
            makingFeeFixedPerGram = 0.0,
            usePercentMakingFee = true,
            sellerProfitPercent = 7.0,
            vatPercent = 10.0
        )
        assertNotNull(gold)
        assertEquals(8_000_000.0, gold!!.rawGoldPrice, 1e-6)

        val units = UnitConverterEngine.unitsFor(UnitCategory.WEIGHT)
        val mesghal = units.first { it.id == "mesghal" }
        val gram = units.first { it.id == "g" }
        assertEquals(4.608, UnitConverterEngine.convert(1.0, UnitCategory.WEIGHT, mesghal, gram), 1e-6)
    }
}
