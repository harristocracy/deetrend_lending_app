package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Borrower entity representing customer identity and primary bank details.
 */
@Entity(tableName = "borrowers")
data class Borrower(
    @PrimaryKey(autoGenerate = true)
    val borrower_id: Long = 0,
    val full_name: String,
    val phone_number: String,
    val occupation_type: String, // "Salary Earner" or "Business Person"
    val home_address: String,
    val work_or_business_location: String,
    val account_name: String,
    val bank_name: String,
    val account_number: String,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

/**
 * Application status enum
 */
enum class ApplicationStatus {
    Pending,
    Approved,
    Rejected
}

/**
 * Loan application entity submitted via public or admin channels.
 */
@Entity(
    tableName = "loan_applications",
    foreignKeys = [
        ForeignKey(
            entity = Borrower::class,
            parentColumns = ["borrower_id"],
            childColumns = ["borrower_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["application_id"], unique = true), Index(value = ["borrower_id"])]
)
data class LoanApplicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val application_id: String, // format: APP-YYYY-XXXX
    val borrower_id: Long?,
    val full_name: String,
    val phone_number: String,
    val occupation_type: String, // "Salary Earner" or "Business Person"
    val home_address: String,
    val work_or_business_location: String,
    val amount_requested: Double,
    val loan_purpose: String,
    val repayment_plan: String, // "Weekly" or "Monthly"
    val collateral_provided: Boolean,
    val collateral_description: String,
    val loan_tenure_months: Int,
    val account_name: String,
    val bank_name: String,
    val account_number: String,
    val application_status: ApplicationStatus = ApplicationStatus.Pending,
    val submitted_at: Long = System.currentTimeMillis(),
    val reviewed_at: Long? = null,
    val rejection_reason: String = "",
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

/**
 * Amortization Type for approved loans.
 */
enum class AmortizationType {
    FLAT_INTEREST,
    REDUCING_BALANCE
}

/**
 * Repayment Frequency for approved loans.
 */
enum class RepaymentFrequency {
    WEEKLY,
    MONTHLY
}

/**
 * Loan Status determined dynamically or persisted.
 */
enum class LoanRecordStatus {
    Active,
    Overdue,
    Settled
}

/**
 * Payment Status for V1 tracking.
 */
enum class PaymentStatus {
    Active,
    Paid
}

/**
 * Approved Loan Record created when admin configures and confirms loan approval.
 */
@Entity(
    tableName = "approved_loans",
    foreignKeys = [
        ForeignKey(
            entity = Borrower::class,
            parentColumns = ["borrower_id"],
            childColumns = ["borrower_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loan_id"], unique = true), Index(value = ["borrower_id"])]
)
data class ApprovedLoan(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val loan_id: String, // format: DGE-YYYY-XXXX
    val application_id: String,
    val borrower_id: Long,
    val borrower_name: String,
    val phone_number: String,
    val principal_amount: Double,
    val interest_rate_monthly: Double, // e.g. 7.0 for 7% per month
    val amortization_type: AmortizationType,
    val repayment_frequency: RepaymentFrequency,
    val loan_tenure_months: Int,
    val date_issued: Long, // timestamp
    val due_date: Long, // calendar-month calculated timestamp
    val total_repayment: Double,
    val outstanding_balance: Double,
    val payment_status: PaymentStatus = PaymentStatus.Active,
    val loan_status: LoanRecordStatus = LoanRecordStatus.Active,
    val collateral_description: String,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

/**
 * Consent status for agreements.
 */
enum class ConsentStatus {
    Pending,
    Consented
}

/**
 * Loan Agreement generated for borrower electronic consent.
 */
@Entity(
    tableName = "loan_agreements",
    foreignKeys = [
        ForeignKey(
            entity = ApprovedLoan::class,
            parentColumns = ["id"],
            childColumns = ["approved_loan_primary_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["agreement_id"], unique = true), Index(value = ["secure_token"], unique = true), Index(value = ["approved_loan_primary_id"])]
)
data class LoanAgreement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val agreement_id: String, // format: AGR-YYYY-XXXX
    val loan_id: String, // DGE-YYYY-XXXX
    val approved_loan_primary_id: Long,
    val borrower_id: Long,
    val secure_token: String, // Secure UUID token for /agreement/[token]
    val agreement_version: Int = 1,
    val agreement_content: String, // Reproducible snapshot of terms and conditions
    val consent_status: ConsentStatus = ConsentStatus.Pending,
    val borrower_name: String,
    val consent_timestamp: Long? = null,
    val signature_consent_data: String? = null,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

/**
 * Amortization Schedule row for reducing balance & flat interest repayments.
 */
@Entity(
    tableName = "deetrend_installments",
    foreignKeys = [
        ForeignKey(
            entity = ApprovedLoan::class,
            parentColumns = ["id"],
            childColumns = ["approved_loan_primary_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["approved_loan_primary_id"])]
)
data class DeetrendInstallment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val approved_loan_primary_id: Long,
    val loan_id: String,
    val installment_number: Int,
    val due_date_millis: Long,
    val opening_balance: Double,
    val principal_component: Double,
    val interest_component: Double,
    val total_installment: Double,
    val closing_balance: Double,
    val is_paid: Boolean = false,
    val paid_date_millis: Long? = null
)

/**
 * Future-ready payment tracking table.
 */
@Entity(
    tableName = "payments",
    indices = [Index(value = ["loan_id"]), Index(value = ["borrower_id"])]
)
data class PaymentRecord(
    @PrimaryKey(autoGenerate = true)
    val payment_id: Long = 0,
    val loan_id: String,
    val borrower_id: Long,
    val payment_date: Long = System.currentTimeMillis(),
    val amount_paid: Double,
    val principal_paid: Double = 0.0,
    val interest_paid: Double = 0.0,
    val penalty_paid: Double = 0.0,
    val payment_method: String = "Bank Transfer",
    val reference: String = "",
    val recorded_by: String = "Admin",
    val notes: String = ""
)

/**
 * Staff and Administrator Roles (Designed for V1 with extensibility)
 */
enum class UserRole {
    ADMINISTRATOR,
    LOAN_OFFICER,
    ACCOUNTANT,
    COLLECTION_OFFICER,
    MANAGER
}

/**
 * User account entity for administrative authentication and staff roles.
 */
@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserAccount(
    @PrimaryKey(autoGenerate = true)
    val user_id: Long = 0,
    val name: String,
    val email: String,
    val role: UserRole = UserRole.ADMINISTRATOR,
    val password_hash: String = "",
    val is_active: Boolean = true,
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis()
)

/**
 * System Audit Trail entity for logging crucial actions across the lending lifecycle.
 * Actions: Application submitted, Application approved, Application rejected,
 * Loan created, Loan terms configured, Agreement generated, Agreement viewed,
 * Agreement consented, Repayments recorded.
 */
@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["timestamp"]), Index(value = ["record_affected"])]
)
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user_name: String,
    val action: String,
    val record_affected: String, // e.g. "APP-2026-0001", "DGE-2026-0001", "AGR-2026-0001"
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Document record metadata for file storage (agreements, borrower ID, collateral, receipts).
 */
@Entity(
    tableName = "lending_documents",
    indices = [Index(value = ["borrower_id"]), Index(value = ["loan_id"])]
)
data class LendingDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val document_type: String, // "Signed Agreement", "Borrower Identification", "Collateral Document", "Payment Receipt"
    val file_reference: String,
    val borrower_id: Long?,
    val loan_id: String?,
    val agreement_id: String?,
    val created_at: Long = System.currentTimeMillis()
)

