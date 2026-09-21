package com.example.domain.calculator

import com.example.data.model.AmortizationType
import com.example.data.model.DeetrendInstallment
import com.example.data.model.RepaymentFrequency
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.pow
import kotlin.math.roundToInt

data class LoanCalculationResult(
    val principalAmount: Double,
    val monthlyInterestRatePercent: Double,
    val tenureMonths: Int,
    val amortizationType: AmortizationType,
    val repaymentFrequency: RepaymentFrequency,
    val totalInterest: Double,
    val totalRepayment: Double,
    val periodicPayment: Double,
    val numberOfInstallments: Int,
    val schedule: List<DeetrendInstallment>
)

object DeetrendLoanCalculator {

    /**
     * Calculates the due date using proper calendar month arithmetic:
     * Due Date = Loan Issue Date + Loan Tenure (in calendar months)
     */
    fun calculateDueDate(issueDateMillis: Long, tenureMonths: Int): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = issueDateMillis
            add(Calendar.MONTH, tenureMonths)
        }
        return calendar.timeInMillis
    }

    /**
     * Computes Days Remaining relative to the current date:
     * - > 0: active days remaining
     * - 0: due today
     * - < 0: days overdue
     */
    fun getDaysDifference(dueDateMillis: Long, currentDateMillis: Long = System.currentTimeMillis()): Long {
        val dueCal = Calendar.getInstance().apply {
            timeInMillis = dueDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentCal = Calendar.getInstance().apply {
            timeInMillis = currentDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = dueCal.timeInMillis - currentCal.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    /**
     * Formats Days Remaining status cleanly:
     * e.g. "45 days remaining", "Due Today", or "12 days overdue"
     */
    fun formatDaysRemaining(dueDateMillis: Long, currentDateMillis: Long = System.currentTimeMillis()): String {
        val days = getDaysDifference(dueDateMillis, currentDateMillis)
        return when {
            days > 0 -> "$days days remaining"
            days == 0L -> "Due Today"
            else -> "${-days} days overdue"
        }
    }

    /**
     * Performs financial calculation for Flat Interest or Reducing Balance loans.
     */
    fun calculateLoan(
        loanId: String,
        approvedLoanPrimaryId: Long = 0,
        principal: Double,
        monthlyInterestRatePercent: Double,
        tenureMonths: Int,
        amortizationType: AmortizationType,
        repaymentFrequency: RepaymentFrequency,
        issueDateMillis: Long = System.currentTimeMillis()
    ): LoanCalculationResult {
        val clampedTenure = tenureMonths.coerceAtLeast(1)
        val clampedPrincipal = principal.coerceAtLeast(0.0)
        val monthlyRateDecimal = monthlyInterestRatePercent / 100.0

        val numberOfInstallments = if (repaymentFrequency == RepaymentFrequency.WEEKLY) {
            clampedTenure * 4
        } else {
            clampedTenure
        }

        val schedule = mutableListOf<DeetrendInstallment>()

        when (amortizationType) {
            AmortizationType.FLAT_INTEREST -> {
                // Flat Interest: Interest = Principal * MonthlyRate * TenureMonths
                val totalInterest = roundTwoDecimals(clampedPrincipal * monthlyRateDecimal * clampedTenure)
                val totalRepayment = roundTwoDecimals(clampedPrincipal + totalInterest)
                val installmentAmount = roundTwoDecimals(totalRepayment / numberOfInstallments)
                val periodicPrincipal = roundTwoDecimals(clampedPrincipal / numberOfInstallments)
                val periodicInterest = roundTwoDecimals(totalInterest / numberOfInstallments)

                var currentBalance = clampedPrincipal
                val calendar = Calendar.getInstance().apply { timeInMillis = issueDateMillis }

                for (i in 1..numberOfInstallments) {
                    if (repaymentFrequency == RepaymentFrequency.WEEKLY) {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    } else {
                        calendar.add(Calendar.MONTH, 1)
                    }

                    val openBalance = currentBalance
                    val isLast = (i == numberOfInstallments)

                    val actualPrincipal = if (isLast) roundTwoDecimals(openBalance) else periodicPrincipal
                    val actualInstallment = if (isLast) {
                        roundTwoDecimals(totalRepayment - (installmentAmount * (numberOfInstallments - 1)))
                    } else {
                        installmentAmount
                    }
                    val actualInterest = roundTwoDecimals(actualInstallment - actualPrincipal)
                    val closeBalance = if (isLast) 0.0 else roundTwoDecimals((openBalance - actualPrincipal).coerceAtLeast(0.0))
                    currentBalance = closeBalance

                    schedule.add(
                        DeetrendInstallment(
                            approved_loan_primary_id = approvedLoanPrimaryId,
                            loan_id = loanId,
                            installment_number = i,
                            due_date_millis = calendar.timeInMillis,
                            opening_balance = openBalance,
                            principal_component = actualPrincipal,
                            interest_component = actualInterest,
                            total_installment = actualInstallment,
                            closing_balance = closeBalance
                        )
                    )
                }

                return LoanCalculationResult(
                    principalAmount = clampedPrincipal,
                    monthlyInterestRatePercent = monthlyInterestRatePercent,
                    tenureMonths = clampedTenure,
                    amortizationType = amortizationType,
                    repaymentFrequency = repaymentFrequency,
                    totalInterest = totalInterest,
                    totalRepayment = totalRepayment,
                    periodicPayment = installmentAmount,
                    numberOfInstallments = numberOfInstallments,
                    schedule = schedule
                )
            }

            AmortizationType.REDUCING_BALANCE -> {
                // Reducing Balance Amortization formula
                val periodicRate = if (repaymentFrequency == RepaymentFrequency.WEEKLY) {
                    monthlyRateDecimal / 4.0
                } else {
                    monthlyRateDecimal
                }

                val periodicPayment = if (periodicRate <= 0.0) {
                    roundTwoDecimals(clampedPrincipal / numberOfInstallments)
                } else {
                    val factor = (1.0 + periodicRate).pow(numberOfInstallments.toDouble())
                    roundTwoDecimals(clampedPrincipal * (periodicRate * factor) / (factor - 1.0))
                }

                var currentBalance = clampedPrincipal
                var accumulatedInterest = 0.0
                val calendar = Calendar.getInstance().apply { timeInMillis = issueDateMillis }

                for (i in 1..numberOfInstallments) {
                    if (repaymentFrequency == RepaymentFrequency.WEEKLY) {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    } else {
                        calendar.add(Calendar.MONTH, 1)
                    }

                    val openBalance = currentBalance
                    val interestComponent = roundTwoDecimals(openBalance * periodicRate)
                    val isLast = (i == numberOfInstallments)

                    var principalComponent = roundTwoDecimals(periodicPayment - interestComponent)
                    if (isLast || principalComponent > openBalance) {
                        principalComponent = roundTwoDecimals(openBalance)
                    }

                    val totalInstallment = roundTwoDecimals(principalComponent + interestComponent)
                    val closeBalance = if (isLast) 0.0 else roundTwoDecimals((openBalance - principalComponent).coerceAtLeast(0.0))
                    currentBalance = closeBalance
                    accumulatedInterest += interestComponent

                    schedule.add(
                        DeetrendInstallment(
                            approved_loan_primary_id = approvedLoanPrimaryId,
                            loan_id = loanId,
                            installment_number = i,
                            due_date_millis = calendar.timeInMillis,
                            opening_balance = openBalance,
                            principal_component = principalComponent,
                            interest_component = interestComponent,
                            total_installment = totalInstallment,
                            closing_balance = closeBalance
                        )
                    )
                }

                val totalInterest = roundTwoDecimals(accumulatedInterest)
                val totalRepayment = roundTwoDecimals(clampedPrincipal + totalInterest)

                return LoanCalculationResult(
                    principalAmount = clampedPrincipal,
                    monthlyInterestRatePercent = monthlyInterestRatePercent,
                    tenureMonths = clampedTenure,
                    amortizationType = amortizationType,
                    repaymentFrequency = repaymentFrequency,
                    totalInterest = totalInterest,
                    totalRepayment = totalRepayment,
                    periodicPayment = periodicPayment,
                    numberOfInstallments = numberOfInstallments,
                    schedule = schedule
                )
            }
        }
    }

    fun roundTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
