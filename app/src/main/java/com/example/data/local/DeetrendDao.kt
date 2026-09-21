package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ApplicationStatus
import com.example.data.model.ApprovedLoan
import com.example.data.model.Borrower
import com.example.data.model.ConsentStatus
import com.example.data.model.DeetrendInstallment
import com.example.data.model.LoanAgreement
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.PaymentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DeetrendDao {

    // --- BORROWERS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBorrower(borrower: Borrower): Long

    @Query("SELECT * FROM borrowers ORDER BY created_at DESC")
    fun getAllBorrowers(): Flow<List<Borrower>>

    @Query("SELECT * FROM borrowers ORDER BY created_at DESC")
    suspend fun getAllBorrowersList(): List<Borrower>

    @Query("SELECT * FROM borrowers WHERE borrower_id = :borrowerId LIMIT 1")
    suspend fun getBorrowerById(borrowerId: Long): Borrower?

    @Query("SELECT * FROM borrowers WHERE phone_number = :phone LIMIT 1")
    suspend fun findBorrowerByPhone(phone: String): Borrower?

    // --- LOAN APPLICATIONS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(app: LoanApplicationEntity): Long

    @Update
    suspend fun updateApplication(app: LoanApplicationEntity)

    @Query("SELECT * FROM loan_applications ORDER BY submitted_at DESC")
    fun getAllApplications(): Flow<List<LoanApplicationEntity>>

    @Query("SELECT * FROM loan_applications ORDER BY submitted_at DESC")
    suspend fun getAllApplicationsList(): List<LoanApplicationEntity>

    @Query("SELECT * FROM loan_applications WHERE application_id = :applicationId LIMIT 1")
    suspend fun getApplicationByApplicationId(applicationId: String): LoanApplicationEntity?

    @Query("SELECT * FROM loan_applications WHERE id = :id LIMIT 1")
    suspend fun getApplicationById(id: Long): LoanApplicationEntity?

    @Query("SELECT COUNT(*) FROM loan_applications WHERE application_id LIKE :prefix")
    suspend fun countApplicationsWithPrefix(prefix: String): Int

    @Query("UPDATE loan_applications SET application_status = :status, reviewed_at = :reviewedAt, rejection_reason = :reason, updated_at = :reviewedAt WHERE application_id = :applicationId")
    suspend fun updateApplicationStatus(applicationId: String, status: ApplicationStatus, reviewedAt: Long, reason: String = "")

    // --- APPROVED LOANS (PORTFOLIO) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApprovedLoan(loan: ApprovedLoan): Long

    @Update
    suspend fun updateApprovedLoan(loan: ApprovedLoan)

    @Query("SELECT * FROM approved_loans ORDER BY date_issued DESC")
    fun getAllApprovedLoans(): Flow<List<ApprovedLoan>>

    @Query("SELECT * FROM approved_loans ORDER BY date_issued DESC")
    suspend fun getAllApprovedLoansList(): List<ApprovedLoan>

    @Query("SELECT * FROM approved_loans WHERE id = :id LIMIT 1")
    suspend fun getApprovedLoanById(id: Long): ApprovedLoan?

    @Query("SELECT * FROM approved_loans WHERE loan_id = :loanId LIMIT 1")
    suspend fun getApprovedLoanByLoanId(loanId: String): ApprovedLoan?

    @Query("SELECT COUNT(*) FROM approved_loans WHERE loan_id LIKE :prefix")
    suspend fun countApprovedLoansWithPrefix(prefix: String): Int

    @Query("UPDATE approved_loans SET outstanding_balance = :newBalance, payment_status = :paymentStatus, loan_status = :loanStatus, updated_at = :now WHERE id = :id")
    suspend fun updateLoanBalances(id: Long, newBalance: Double, paymentStatus: String, loanStatus: String, now: Long)

    // --- LOAN AGREEMENTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanAgreement(agreement: LoanAgreement): Long

    @Update
    suspend fun updateLoanAgreement(agreement: LoanAgreement)

    @Query("SELECT * FROM loan_agreements ORDER BY created_at DESC")
    fun getAllAgreements(): Flow<List<LoanAgreement>>

    @Query("SELECT * FROM loan_agreements ORDER BY created_at DESC")
    suspend fun getAllAgreementsList(): List<LoanAgreement>

    @Query("SELECT * FROM loan_agreements WHERE agreement_id = :agreementId LIMIT 1")
    suspend fun getAgreementByAgreementId(agreementId: String): LoanAgreement?

    @Query("SELECT * FROM loan_agreements WHERE secure_token = :token LIMIT 1")
    suspend fun getAgreementBySecureToken(token: String): LoanAgreement?

    @Query("SELECT * FROM loan_agreements WHERE loan_id = :loanId ORDER BY agreement_version DESC LIMIT 1")
    suspend fun getLatestAgreementForLoan(loanId: String): LoanAgreement?

    @Query("SELECT COUNT(*) FROM loan_agreements WHERE agreement_id LIKE :prefix")
    suspend fun countAgreementsWithPrefix(prefix: String): Int

    @Query("UPDATE loan_agreements SET consent_status = :status, consent_timestamp = :timestamp, signature_consent_data = :signature, updated_at = :timestamp WHERE secure_token = :token")
    suspend fun submitAgreementConsent(token: String, status: ConsentStatus, timestamp: Long, signature: String)

    // --- INSTALLMENTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<DeetrendInstallment>)

    @Query("SELECT * FROM deetrend_installments WHERE approved_loan_primary_id = :loanPrimaryId ORDER BY installment_number ASC")
    fun getInstallmentsForApprovedLoan(loanPrimaryId: Long): Flow<List<DeetrendInstallment>>

    @Query("SELECT * FROM deetrend_installments WHERE approved_loan_primary_id = :loanPrimaryId ORDER BY installment_number ASC")
    suspend fun getInstallmentsForApprovedLoanOnce(loanPrimaryId: Long): List<DeetrendInstallment>

    @Query("SELECT * FROM deetrend_installments WHERE loan_id = :loanId ORDER BY installment_number ASC")
    suspend fun getInstallmentsByLoanId(loanId: String): List<DeetrendInstallment>

    // --- FUTURE REPAYMENTS PREPARATION ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentRecord): Long

    @Query("SELECT * FROM payments WHERE loan_id = :loanId ORDER BY payment_date DESC")
    fun getPaymentsForLoan(loanId: String): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments ORDER BY payment_date DESC")
    fun getAllPayments(): Flow<List<PaymentRecord>>

    // --- USER ACCOUNTS (ADMIN AUTHENTICATION) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: com.example.data.model.UserAccount): Long

    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): com.example.data.model.UserAccount?

    @Query("SELECT * FROM user_accounts ORDER BY created_at ASC")
    fun getAllUsers(): Flow<List<com.example.data.model.UserAccount>>

    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun countUsers(): Int

    // --- AUDIT TRAIL ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: com.example.data.model.AuditLog): Long

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAuditLogs(limit: Int = 100): Flow<List<com.example.data.model.AuditLog>>

    @Query("SELECT * FROM audit_logs WHERE record_affected = :recordId ORDER BY timestamp DESC")
    fun getAuditLogsForRecord(recordId: String): Flow<List<com.example.data.model.AuditLog>>

    // --- FILE STORAGE / DOCUMENTS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: com.example.data.model.LendingDocument): Long

    @Query("SELECT * FROM lending_documents ORDER BY created_at DESC")
    fun getAllDocuments(): Flow<List<com.example.data.model.LendingDocument>>

    @Query("SELECT * FROM lending_documents WHERE loan_id = :loanId")
    fun getDocumentsForLoan(loanId: String): Flow<List<com.example.data.model.LendingDocument>>
}

