package com.example.domain.calculator

import com.example.data.model.AmortizationInstallment
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.roundToInt

object LoanCalculator {

    /**
     * Calculates the dynamic annual interest rate (in percent) based on principal amount and term length.
     * Higher principal gets volume discounts, longer terms carry a term liquidity premium,
     * while small micro-loans incur higher servicing margins.
     */
    fun calculateAnnualInterestRate(principal: Double, termMonths: Int): Double {
        var baseRate = 7.50

        // Principal tier adjustment
        val principalAdjustment = when {
            principal < 5_000.0 -> 2.50 // Micro loans (+2.5%)
            principal <= 15_000.0 -> 1.25 // Standard retail (+1.25%)
            principal <= 40_000.0 -> 0.00 // Prime tier (+0.0%)
            principal <= 80_000.0 -> -0.50 // Large loan discount (-0.5%)
            else -> -1.00 // Jumbo prime discount (-1.0%)
        }

        // Term length adjustment (in months)
        val termAdjustment = when {
            termMonths <= 6 -> -0.75 // Ultra-short term discount
            termMonths <= 12 -> -0.25 // 1 year discount
            termMonths <= 24 -> 0.00 // 2 year baseline
            termMonths <= 36 -> 0.50 // 3 year premium
            termMonths <= 48 -> 1.00 // 4 year premium
            else -> 1.50 // 5+ year term premium
        }

        val totalRate = baseRate + principalAdjustment + termAdjustment
        // Clamp between 4.5% and 24.0%
        val clampedRate = totalRate.coerceIn(4.50, 24.00)
        return (clampedRate * 100).roundToInt() / 100.0
    }

    /**
     * Calculates the monthly payment (EMI) using the standard amortization formula:
     * M = P * [r(1 + r)^n] / [(1 + r)^n - 1]
     */
    fun calculateMonthlyPayment(principal: Double, annualRatePercent: Double, termMonths: Int): Double {
        if (principal <= 0 || termMonths <= 0) return 0.0
        val monthlyRate = (annualRatePercent / 100.0) / 12.0
        if (monthlyRate <= 0.0) {
            return roundTwoDecimals(principal / termMonths)
        }

        val factor = (1.0 + monthlyRate).pow(termMonths.toDouble())
        val monthlyPayment = principal * (monthlyRate * factor) / (factor - 1.0)
        return roundTwoDecimals(monthlyPayment)
    }

    /**
     * Generates a full month-by-month amortization schedule for a loan.
     */
    fun generateAmortizationSchedule(
        loanId: Long,
        principal: Double,
        annualRatePercent: Double,
        termMonths: Int,
        startDateMillis: Long = System.currentTimeMillis()
    ): List<AmortizationInstallment> {
        val schedule = mutableListOf<AmortizationInstallment>()
        val monthlyPayment = calculateMonthlyPayment(principal, annualRatePercent, termMonths)
        val monthlyRate = (annualRatePercent / 100.0) / 12.0

        var currentBalance = principal
        val calendar = Calendar.getInstance().apply {
            timeInMillis = startDateMillis
        }

        for (i in 1..termMonths) {
            // Next month due date
            calendar.add(Calendar.MONTH, 1)
            val dueDateMillis = calendar.timeInMillis

            val beginningBalance = currentBalance
            val interestPart = roundTwoDecimals(beginningBalance * monthlyRate)
            var principalPart = roundTwoDecimals(monthlyPayment - interestPart)

            // Adjust for final installment rounding so ending balance hits exact 0
            if (i == termMonths || principalPart > currentBalance) {
                principalPart = roundTwoDecimals(currentBalance)
            }

            val actualPayment = roundTwoDecimals(principalPart + interestPart)
            val endingBalance = roundTwoDecimals((beginningBalance - principalPart).coerceAtLeast(0.0))
            currentBalance = endingBalance

            schedule.add(
                AmortizationInstallment(
                    loanId = loanId,
                    installmentNumber = i,
                    dueDateMillis = dueDateMillis,
                    beginningBalance = beginningBalance,
                    principalPart = principalPart,
                    interestPart = interestPart,
                    totalPayment = actualPayment,
                    endingBalance = endingBalance,
                    isPaid = false
                )
            )
        }

        return schedule
    }

    fun roundTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
