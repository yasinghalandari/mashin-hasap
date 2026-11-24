package com.example.domain

import kotlin.math.pow

data class LoanSummary(
    val monthlyPayment: Double,
    val totalPayment: Double,
    val totalInterest: Double,
    val principalRatio: Float
)

data class GoldSummary(
    val rawGoldPrice: Double,
    val makingFeeAmount: Double,
    val profitAmount: Double,
    val vatAmount: Double,
    val totalPrice: Double
)

data class VatDiscountSummary(
    val discountAmount: Double,
    val afterDiscount: Double,
    val vatAmount: Double,
    val finalTotal: Double,
    val perPersonShare: Double
)

data class SavingsSummary(
    val finalBalance: Double,
    val totalDeposited: Double,
    val totalProfit: Double
)

object FinanceCalculator {

    fun calculateLoan(
        principal: Double,
        annualRatePercent: Double,
        months: Int
    ): LoanSummary? {
        if (principal <= 0 || months <= 0 || annualRatePercent < 0) return null
        val monthlyRate = (annualRatePercent / 100.0) / 12.0
        val monthlyPayment = if (monthlyRate == 0.0) {
            principal / months
        } else {
            val factor = (1.0 + monthlyRate).pow(months)
            principal * (monthlyRate * factor) / (factor - 1.0)
        }
        val totalPayment = monthlyPayment * months
        val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)
        val ratio = if (totalPayment > 0) (principal / totalPayment).toFloat().coerceIn(0f, 1f) else 1f
        return LoanSummary(
            monthlyPayment = monthlyPayment,
            totalPayment = totalPayment,
            totalInterest = totalInterest,
            principalRatio = ratio
        )
    }

    fun calculateGoldPrice(
        weightGrams: Double,
        pricePerGram18k: Double,
        makingFeePercent: Double,
        makingFeeFixedPerGram: Double,
        usePercentMakingFee: Boolean,
        sellerProfitPercent: Double,
        vatPercent: Double
    ): GoldSummary? {
        if (weightGrams <= 0 || pricePerGram18k <= 0) return null
        val rawGold = weightGrams * pricePerGram18k
        val makingFee = if (usePercentMakingFee) {
            rawGold * (makingFeePercent / 100.0)
        } else {
            weightGrams * makingFeeFixedPerGram
        }
        val profit = (rawGold + makingFee) * (sellerProfitPercent / 100.0)
        // Standard Iranian jewelry VAT applies to making fee + seller profit
        val vat = (makingFee + profit) * (vatPercent / 100.0)
        val total = rawGold + makingFee + profit + vat
        return GoldSummary(
            rawGoldPrice = rawGold,
            makingFeeAmount = makingFee,
            profitAmount = profit,
            vatAmount = vat,
            totalPrice = total
        )
    }

    fun calculateVatAndDiscount(
        amount: Double,
        discountPercent: Double,
        vatPercent: Double,
        splitCount: Int
    ): VatDiscountSummary? {
        if (amount < 0 || splitCount <= 0) return null
        val discount = amount * (discountPercent.coerceIn(0.0, 100.0) / 100.0)
        val afterDiscount = amount - discount
        val vat = afterDiscount * (vatPercent.coerceAtLeast(0.0) / 100.0)
        val finalTotal = afterDiscount + vat
        return VatDiscountSummary(
            discountAmount = discount,
            afterDiscount = afterDiscount,
            vatAmount = vat,
            finalTotal = finalTotal,
            perPersonShare = finalTotal / splitCount
        )
    }

    fun calculateSavings(
        initialDeposit: Double,
        monthlyDeposit: Double,
        annualRatePercent: Double,
        months: Int
    ): SavingsSummary? {
        if (initialDeposit < 0 || monthlyDeposit < 0 || annualRatePercent < 0 || months <= 0) return null
        val r = (annualRatePercent / 100.0) / 12.0
        var balance = initialDeposit
        for (m in 1..months) {
            balance = balance * (1.0 + r) + monthlyDeposit
        }
        val totalDeposited = initialDeposit + monthlyDeposit * months
        val totalProfit = (balance - totalDeposited).coerceAtLeast(0.0)
        return SavingsSummary(
            finalBalance = balance,
            totalDeposited = totalDeposited,
            totalProfit = totalProfit
        )
    }
}
