package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LoanStatus {
    DRAFT,
    AGREEMENT_PENDING,
    ACTIVE,
    COMPLETED,
    DEFAULTED
}

enum class InstallmentStatus {
    PAID,
    DUE_SOON,
    UPCOMING,
    OVERDUE
}

enum class ReminderType {
    UPCOMING_7_DAYS,
    UPCOMING_3_DAYS,
    DUE_TODAY,
    OVERDUE
}

@Entity(tableName = "loans")
data class LoanApplication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val borrowerName: String,
    val borrowerEmail: String,
    val borrowerPhone: String,
    val borrowerIdNumber: String,
    val employmentStatus: String,
    val monthlyIncome: Double,
    val loanPurpose: String,
    val principalAmount: Double,
    val termMonths: Int,
    val calculatedInterestRate: Double, // annual rate in percent (e.g. 8.25)
    val monthlyPayment: Double,
    val totalRepayment: Double,
    val totalInterest: Double,
    val status: LoanStatus = LoanStatus.AGREEMENT_PENDING,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val agreementSignedAtMillis: Long? = null,
    val signatureName: String? = null,
    val disbursementDateMillis: Long? = null
)

@Entity(tableName = "amortization_installments")
data class AmortizationInstallment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val installmentNumber: Int,
    val dueDateMillis: Long,
    val beginningBalance: Double,
    val principalPart: Double,
    val interestPart: Double,
    val totalPayment: Double,
    val endingBalance: Double,
    val isPaid: Boolean = false,
    val paidDateMillis: Long? = null,
    val paidAmount: Double = 0.0
)

@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val installmentId: Long? = null,
    val installmentNumber: Int? = null,
    val amountPaid: Double,
    val paymentDateMillis: Long = System.currentTimeMillis(),
    val paymentMethod: String, // e.g. "Bank Transfer (ACH)", "Debit Card", "Cash"
    val referenceNote: String = "",
    val receiptNumber: String = ""
)

@Entity(tableName = "loan_reminders")
data class LoanReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loanId: Long,
    val installmentNumber: Int,
    val borrowerName: String,
    val amountDue: Double,
    val dueDateMillis: Long,
    val reminderType: ReminderType,
    val title: String,
    val message: String,
    val isDismissed: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val sentNotification: Boolean = false
)
