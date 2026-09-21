package com.example

import com.example.data.model.AmortizationType
import com.example.data.model.RepaymentFrequency
import com.example.domain.calculator.DeetrendLoanCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DeetrendLoanCalculatorTest {

    @Test
    fun testFlatInterestCalculation_6Months_Monthly() {
        // Principal 100,000, 7% monthly interest rate, 6 months tenure
        // Monthly interest = 100,000 * 0.07 = 7,000
        // Total interest = 7,000 * 6 = 42,000
        // Total repayment = 142,000
        // Monthly installment = 142,000 / 6 = 23,666.67
        val result = DeetrendLoanCalculator.calculateLoan(
            loanId = "DGE-2026-0001",
            principal = 100000.0,
            monthlyInterestRatePercent = 7.0,
            tenureMonths = 6,
            amortizationType = AmortizationType.FLAT_INTEREST,
            repaymentFrequency = RepaymentFrequency.MONTHLY
        )

        assertEquals(42000.0, result.totalInterest, 0.01)
        assertEquals(142000.0, result.totalRepayment, 0.01)
        assertEquals(6, result.schedule.size)
        assertEquals(23666.67, result.periodicPayment, 0.01)
    }

    @Test
    fun testReducingBalanceCalculation_6Months_Monthly() {
        val result = DeetrendLoanCalculator.calculateLoan(
            loanId = "DGE-2026-0002",
            principal = 100000.0,
            monthlyInterestRatePercent = 7.0,
            tenureMonths = 6,
            amortizationType = AmortizationType.REDUCING_BALANCE,
            repaymentFrequency = RepaymentFrequency.MONTHLY
        )

        assertTrue("Reducing balance total interest should be positive", result.totalInterest > 0.0)
        assertTrue("Total repayment should exceed principal", result.totalRepayment > 100000.0)
        assertEquals(6, result.schedule.size)
        // Check final closing balance is near zero
        assertEquals(0.0, result.schedule.last().closing_balance, 0.05)
    }

    @Test
    fun testDueDateCalculation_ExactCalendarMonths() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.JANUARY, 15, 0, 0, 0)
        val issueDate = cal.timeInMillis

        val dueDate = DeetrendLoanCalculator.calculateDueDate(issueDate, 3)
        val dueCal = Calendar.getInstance()
        dueCal.timeInMillis = dueDate

        assertEquals(2026, dueCal.get(Calendar.YEAR))
        assertEquals(Calendar.APRIL, dueCal.get(Calendar.MONTH))
        assertEquals(15, dueCal.get(Calendar.DAY_OF_MONTH))
    }
}
