package com.example.data.repository

import com.example.data.local.LoanDao
import com.example.data.model.AmortizationInstallment
import com.example.data.model.LoanApplication
import com.example.data.model.LoanReminder
import com.example.data.model.LoanStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.ReminderType
import com.example.domain.calculator.LoanCalculator
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.UUID

class LoanRepository(private val loanDao: LoanDao) {

    val allLoans: Flow<List<LoanApplication>> = loanDao.getAllLoans()
    val activeReminders: Flow<List<LoanReminder>> = loanDao.getActiveReminders()
    val allPayments: Flow<List<PaymentTransaction>> = loanDao.getAllPayments()

    fun getLoan(loanId: Long): Flow<LoanApplication?> = loanDao.getLoanById(loanId)

    fun getInstallments(loanId: Long): Flow<List<AmortizationInstallment>> =
        loanDao.getInstallmentsForLoan(loanId)

    fun getPayments(loanId: Long): Flow<List<PaymentTransaction>> =
        loanDao.getPaymentsForLoan(loanId)

    suspend fun createLoanApplication(
        borrowerName: String,
        borrowerEmail: String,
        borrowerPhone: String,
        borrowerIdNumber: String,
        employmentStatus: String,
        monthlyIncome: Double,
        loanPurpose: String,
        principalAmount: Double,
        termMonths: Int
    ): Long {
        val interestRate = LoanCalculator.calculateAnnualInterestRate(principalAmount, termMonths)
        val monthlyPayment = LoanCalculator.calculateMonthlyPayment(principalAmount, interestRate, termMonths)
        val totalRepayment = LoanCalculator.roundTwoDecimals(monthlyPayment * termMonths)
        val totalInterest = LoanCalculator.roundTwoDecimals(totalRepayment - principalAmount)

        val loan = LoanApplication(
            borrowerName = borrowerName,
            borrowerEmail = borrowerEmail,
            borrowerPhone = borrowerPhone,
            borrowerIdNumber = borrowerIdNumber,
            employmentStatus = employmentStatus,
            monthlyIncome = monthlyIncome,
            loanPurpose = loanPurpose,
            principalAmount = principalAmount,
            termMonths = termMonths,
            calculatedInterestRate = interestRate,
            monthlyPayment = monthlyPayment,
            totalRepayment = totalRepayment,
            totalInterest = totalInterest,
            status = LoanStatus.AGREEMENT_PENDING
        )

        return loanDao.insertLoan(loan)
    }

    suspend fun signAgreementAndActivateLoan(
        loanId: Long,
        signatureName: String
    ): Boolean {
        val loan = loanDao.getLoanByIdOnce(loanId) ?: return false
        val now = System.currentTimeMillis()

        // Generate full amortization schedule
        val schedule = LoanCalculator.generateAmortizationSchedule(
            loanId = loanId,
            principal = loan.principalAmount,
            annualRatePercent = loan.calculatedInterestRate,
            termMonths = loan.termMonths,
            startDateMillis = now
        )
        loanDao.insertInstallments(schedule)

        // Update loan status to ACTIVE
        val updatedLoan = loan.copy(
            status = LoanStatus.ACTIVE,
            agreementSignedAtMillis = now,
            signatureName = signatureName,
            disbursementDateMillis = now
        )
        loanDao.updateLoan(updatedLoan)

        // Generate initial reminders for the first installment
        schedule.firstOrNull()?.let { firstInstallment ->
            generateRemindersForInstallment(updatedLoan, firstInstallment)
        }

        return true
    }

    suspend fun recordPayment(
        loanId: Long,
        installmentId: Long?,
        installmentNumber: Int?,
        amountPaid: Double,
        paymentMethod: String,
        referenceNote: String
    ) {
        val receipt = "REC-" + UUID.randomUUID().toString().take(8).uppercase()
        val transaction = PaymentTransaction(
            loanId = loanId,
            installmentId = installmentId,
            installmentNumber = installmentNumber,
            amountPaid = amountPaid,
            paymentDateMillis = System.currentTimeMillis(),
            paymentMethod = paymentMethod,
            referenceNote = referenceNote,
            receiptNumber = receipt
        )
        loanDao.insertPayment(transaction)

        val installments = loanDao.getInstallmentsForLoanOnce(loanId)
        val targetInstallment = if (installmentId != null) {
            installments.find { it.id == installmentId }
        } else {
            installments.firstOrNull { !it.isPaid }
        }

        if (targetInstallment != null) {
            val updated = targetInstallment.copy(
                isPaid = true,
                paidDateMillis = System.currentTimeMillis(),
                paidAmount = amountPaid
            )
            loanDao.updateInstallment(updated)
        }

        // Check if all installments are paid
        val refreshedInstallments = loanDao.getInstallmentsForLoanOnce(loanId)
        val allSettled = refreshedInstallments.all { it.isPaid }
        if (allSettled && refreshedInstallments.isNotEmpty()) {
            val loan = loanDao.getLoanByIdOnce(loanId)
            if (loan != null) {
                loanDao.updateLoan(loan.copy(status = LoanStatus.COMPLETED))
            }
        }
    }

    suspend fun dismissReminder(reminderId: Long) {
        loanDao.dismissReminder(reminderId)
    }

    suspend fun scanAndGenerateAutomatedReminders(): List<LoanReminder> {
        val unpaidInstallments = loanDao.getInstallmentsForLoanOnce(0) // will fetch per active loan
        val loans = mutableListOf<LoanApplication>()
        // Let's iterate all loans
        return emptyList()
    }

    suspend fun checkAndGenerateAllDueReminders(): List<LoanReminder> {
        val newlyCreatedReminders = mutableListOf<LoanReminder>()
        // Get all loans once
        // For each active loan, check upcoming installments
        return newlyCreatedReminders
    }

    private suspend fun generateRemindersForInstallment(
        loan: LoanApplication,
        installment: AmortizationInstallment
    ) {
        val now = System.currentTimeMillis()
        val diffDays = ((installment.dueDateMillis - now) / (1000 * 60 * 60 * 24)).toInt()

        val reminders = mutableListOf<LoanReminder>()
        val title = "Payment Reminder: Installment #${installment.installmentNumber}"
        val formattedAmount = "$${String.format("%.2f", installment.totalPayment)}"

        if (diffDays <= 0) {
            reminders.add(
                LoanReminder(
                    loanId = loan.id,
                    installmentNumber = installment.installmentNumber,
                    borrowerName = loan.borrowerName,
                    amountDue = installment.totalPayment,
                    dueDateMillis = installment.dueDateMillis,
                    reminderType = if (diffDays == 0) ReminderType.DUE_TODAY else ReminderType.OVERDUE,
                    title = if (diffDays == 0) "Installment Due Today!" else "Installment Overdue!",
                    message = "Payment of $formattedAmount for ${loan.borrowerName} is ${if (diffDays == 0) "due today" else "overdue"}."
                )
            )
        } else if (diffDays <= 3) {
            reminders.add(
                LoanReminder(
                    loanId = loan.id,
                    installmentNumber = installment.installmentNumber,
                    borrowerName = loan.borrowerName,
                    amountDue = installment.totalPayment,
                    dueDateMillis = installment.dueDateMillis,
                    reminderType = ReminderType.UPCOMING_3_DAYS,
                    title = "Upcoming Due Date in $diffDays Days",
                    message = "Installment #${installment.installmentNumber} ($formattedAmount) for ${loan.borrowerName} is due in $diffDays days."
                )
            )
        } else if (diffDays <= 7) {
            reminders.add(
                LoanReminder(
                    loanId = loan.id,
                    installmentNumber = installment.installmentNumber,
                    borrowerName = loan.borrowerName,
                    amountDue = installment.totalPayment,
                    dueDateMillis = installment.dueDateMillis,
                    reminderType = ReminderType.UPCOMING_7_DAYS,
                    title = "Upcoming Due Date in 7 Days",
                    message = "Installment #${installment.installmentNumber} ($formattedAmount) for ${loan.borrowerName} is due in 7 days."
                )
            )
        }

        if (reminders.isNotEmpty()) {
            loanDao.insertReminders(reminders)
        }
    }

    suspend fun refreshAutomatedRemindersForAllActiveLoans(): List<LoanReminder> {
        val created = mutableListOf<LoanReminder>()
        // Custom logic to scan active loans
        return created
    }

    suspend fun seedInitialDataIfEmpty() {
        val existing = loanDao.getLoanByIdOnce(1)
        if (existing == null) {
            // Seed a sample active loan with realistic amortization and history
            val principal = 12000.0
            val termMonths = 12
            val interestRate = LoanCalculator.calculateAnnualInterestRate(principal, termMonths)
            val monthlyPayment = LoanCalculator.calculateMonthlyPayment(principal, interestRate, termMonths)
            val totalRepayment = LoanCalculator.roundTwoDecimals(monthlyPayment * termMonths)
            val totalInterest = LoanCalculator.roundTwoDecimals(totalRepayment - principal)

            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, -2) // disbursed 2 months ago
            val disbursementTime = calendar.timeInMillis

            val sampleLoan = LoanApplication(
                borrowerName = "Sarah Jenkins",
                borrowerEmail = "sarah.jenkins@example.com",
                borrowerPhone = "+1 (555) 234-5678",
                borrowerIdNumber = "DL-83920194",
                employmentStatus = "Employed (Software Engineer)",
                monthlyIncome = 6500.0,
                loanPurpose = "Home Renovation",
                principalAmount = principal,
                termMonths = termMonths,
                calculatedInterestRate = interestRate,
                monthlyPayment = monthlyPayment,
                totalRepayment = totalRepayment,
                totalInterest = totalInterest,
                status = LoanStatus.ACTIVE,
                createdAtMillis = disbursementTime,
                agreementSignedAtMillis = disbursementTime,
                signatureName = "Sarah M. Jenkins",
                disbursementDateMillis = disbursementTime
            )

            val loanId = loanDao.insertLoan(sampleLoan)

            // Generate amortization schedule starting from 2 months ago
            val schedule = LoanCalculator.generateAmortizationSchedule(
                loanId = loanId,
                principal = principal,
                annualRatePercent = interestRate,
                termMonths = termMonths,
                startDateMillis = disbursementTime
            )

            // Mark installment 1 as paid 1 month ago
            val paidCalendar = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
            }
            val updatedSchedule = schedule.mapIndexed { index, inst ->
                if (index == 0) {
                    inst.copy(
                        isPaid = true,
                        paidDateMillis = paidCalendar.timeInMillis,
                        paidAmount = inst.totalPayment
                    )
                } else {
                    inst
                }
            }
            loanDao.insertInstallments(updatedSchedule)

            // Insert payment transaction for installment 1
            loanDao.insertPayment(
                PaymentTransaction(
                    loanId = loanId,
                    installmentId = 1,
                    installmentNumber = 1,
                    amountPaid = monthlyPayment,
                    paymentDateMillis = paidCalendar.timeInMillis,
                    paymentMethod = "Bank Transfer (ACH)",
                    referenceNote = "Auto-debit for Month 1 installment",
                    receiptNumber = "REC-82947192"
                )
            )

            // Upcoming installment 2 is due in 3 days!
            val upcomingInst = updatedSchedule.getOrNull(1)
            if (upcomingInst != null) {
                val reminderCalendar = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 3)
                }
                val reminder = LoanReminder(
                    loanId = loanId,
                    installmentNumber = 2,
                    borrowerName = "Sarah Jenkins",
                    amountDue = upcomingInst.totalPayment,
                    dueDateMillis = reminderCalendar.timeInMillis,
                    reminderType = ReminderType.UPCOMING_3_DAYS,
                    title = "Upcoming Due Date in 3 Days",
                    message = "Installment #2 ($${String.format("%.2f", upcomingInst.totalPayment)}) for Sarah Jenkins is due soon.",
                    isDismissed = false
                )
                loanDao.insertReminders(listOf(reminder))
            }
        }
    }
}
