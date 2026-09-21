package com.example.data.remote

import android.util.Log
import com.example.data.model.ApprovedLoan
import com.example.data.model.AuditLog
import com.example.data.model.Borrower
import com.example.data.model.ConsentStatus
import com.example.data.model.LendingDocument
import com.example.data.model.LoanAgreement
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.PaymentRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Service to handle remote cloud database operations for Deetrend Global Enterprise.
 * Interacts with Firebase Firestore collections:
 * - "loan_applications"
 * - "approved_loans"
 * - "loan_agreements"
 * - "borrowers"
 * - "repayments"
 * - "audit_logs"
 * - "lending_documents"
 */
class DeetrendFirestoreService(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        private const val TAG = "DeetrendFirestore"

        const val COLLECTION_APPLICATIONS = "loan_applications"
        const val COLLECTION_APPROVED_LOANS = "approved_loans"
        const val COLLECTION_AGREEMENTS = "loan_agreements"
        const val COLLECTION_BORROWERS = "borrowers"
        const val COLLECTION_REPAYMENTS = "repayments"
        const val COLLECTION_AUDIT_LOGS = "audit_logs"
        const val COLLECTION_DOCUMENTS = "lending_documents"
    }

    // ==========================================
    // 1. LOAN APPLICATIONS
    // ==========================================

    suspend fun saveLoanApplication(application: LoanApplicationEntity): Result<Unit> {
        return try {
            val docRef = firestore.collection(COLLECTION_APPLICATIONS)
                .document(application.application_id)

            val appData = hashMapOf(
                "application_id" to application.application_id,
                "borrower_id" to application.borrower_id,
                "full_name" to application.full_name,
                "phone_number" to application.phone_number,
                "occupation_type" to application.occupation_type,
                "home_address" to application.home_address,
                "work_or_business_location" to application.work_or_business_location,
                "amount_requested" to application.amount_requested,
                "loan_purpose" to application.loan_purpose,
                "repayment_plan" to application.repayment_plan,
                "collateral_provided" to application.collateral_provided,
                "collateral_description" to application.collateral_description,
                "loan_tenure_months" to application.loan_tenure_months,
                "account_name" to application.account_name,
                "bank_name" to application.bank_name,
                "account_number" to application.account_number,
                "application_status" to application.application_status.name,
                "submitted_at" to application.submitted_at,
                "reviewed_at" to application.reviewed_at,
                "rejection_reason" to application.rejection_reason,
                "updated_at" to System.currentTimeMillis()
            )

            docRef.set(appData, SetOptions.merge()).await()
            Log.d(TAG, "Successfully synced application ${application.application_id} to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing application to Firestore", e)
            Result.failure(e)
        }
    }

    fun observeLoanApplications(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_APPLICATIONS)
            .orderBy("submitted_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing loan applications", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val applications = snapshot.documents.mapNotNull { it.data }
                    trySend(applications)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun fetchAllLoanApplications(): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_APPLICATIONS).get().await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching loan applications from Firestore", e)
            emptyList()
        }
    }

    // ==========================================
    // 2. APPROVED LOANS
    // ==========================================

    suspend fun saveApprovedLoan(loan: ApprovedLoan): Result<Unit> {
        return try {
            val docRef = firestore.collection(COLLECTION_APPROVED_LOANS)
                .document(loan.loan_id)

            val loanData = hashMapOf(
                "loan_id" to loan.loan_id,
                "application_id" to loan.application_id,
                "borrower_id" to loan.borrower_id,
                "borrower_name" to loan.borrower_name,
                "phone_number" to loan.phone_number,
                "principal_amount" to loan.principal_amount,
                "interest_rate_monthly" to loan.interest_rate_monthly,
                "amortization_type" to loan.amortization_type.name,
                "repayment_frequency" to loan.repayment_frequency.name,
                "loan_tenure_months" to loan.loan_tenure_months,
                "date_issued" to loan.date_issued,
                "due_date" to loan.due_date,
                "total_repayment" to loan.total_repayment,
                "outstanding_balance" to loan.outstanding_balance,
                "payment_status" to loan.payment_status.name,
                "loan_status" to loan.loan_status.name,
                "collateral_description" to loan.collateral_description,
                "created_at" to loan.created_at,
                "updated_at" to System.currentTimeMillis()
            )

            docRef.set(loanData, SetOptions.merge()).await()
            Log.d(TAG, "Successfully synced approved loan ${loan.loan_id} to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving approved loan to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun updateLoanOutstanding(loanId: String, newBalance: Double, paymentStatus: String, loanStatus: String): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_APPROVED_LOANS)
                .document(loanId)
                .update(
                    mapOf(
                        "outstanding_balance" to newBalance,
                        "payment_status" to paymentStatus,
                        "loan_status" to loanStatus,
                        "updated_at" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating loan balance in Firestore", e)
            Result.failure(e)
        }
    }

    fun observeApprovedLoans(): Flow<List<Map<String, Any>>> = callbackFlow {
        val listenerRegistration = firestore.collection(COLLECTION_APPROVED_LOANS)
            .orderBy("date_issued", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error observing approved loans", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val loans = snapshot.documents.mapNotNull { it.data }
                    trySend(loans)
                }
            }
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun fetchAllApprovedLoans(): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_APPROVED_LOANS).get().await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching approved loans from Firestore", e)
            emptyList()
        }
    }

    // ==========================================
    // 3. LOAN AGREEMENTS & ELECTRONIC CONSENT
    // ==========================================

    suspend fun saveLoanAgreement(agreement: LoanAgreement): Result<Unit> {
        return try {
            val docRef = firestore.collection(COLLECTION_AGREEMENTS)
                .document(agreement.agreement_id)

            val agreementData = hashMapOf(
                "agreement_id" to agreement.agreement_id,
                "loan_id" to agreement.loan_id,
                "borrower_id" to agreement.borrower_id,
                "secure_token" to agreement.secure_token,
                "agreement_version" to agreement.agreement_version,
                "agreement_content" to agreement.agreement_content,
                "consent_status" to agreement.consent_status.name,
                "borrower_name" to agreement.borrower_name,
                "consent_timestamp" to agreement.consent_timestamp,
                "signature_consent_data" to agreement.signature_consent_data,
                "created_at" to agreement.created_at,
                "updated_at" to System.currentTimeMillis()
            )

            docRef.set(agreementData, SetOptions.merge()).await()
            Log.d(TAG, "Successfully synced loan agreement ${agreement.agreement_id} to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving loan agreement to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun recordAgreementConsent(agreementId: String, signature: String, timestamp: Long): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_AGREEMENTS)
                .document(agreementId)
                .update(
                    mapOf(
                        "consent_status" to ConsentStatus.Consented.name,
                        "signature_consent_data" to signature,
                        "consent_timestamp" to timestamp,
                        "updated_at" to System.currentTimeMillis()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error recording agreement consent in Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAllLoanAgreements(): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_AGREEMENTS).get().await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching loan agreements from Firestore", e)
            emptyList()
        }
    }

    // ==========================================
    // 4. BORROWERS
    // ==========================================

    suspend fun saveBorrower(borrower: Borrower): Result<Unit> {
        return try {
            val docRef = firestore.collection(COLLECTION_BORROWERS)
                .document(borrower.borrower_id.toString())

            val borrowerData = hashMapOf(
                "borrower_id" to borrower.borrower_id,
                "full_name" to borrower.full_name,
                "phone_number" to borrower.phone_number,
                "occupation_type" to borrower.occupation_type,
                "home_address" to borrower.home_address,
                "work_or_business_location" to borrower.work_or_business_location,
                "account_name" to borrower.account_name,
                "bank_name" to borrower.bank_name,
                "account_number" to borrower.account_number,
                "created_at" to borrower.created_at,
                "updated_at" to System.currentTimeMillis()
            )

            docRef.set(borrowerData, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving borrower to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAllBorrowers(): List<Map<String, Any>> {
        return try {
            val snapshot = firestore.collection(COLLECTION_BORROWERS).get().await()
            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching borrowers from Firestore", e)
            emptyList()
        }
    }

    // ==========================================
    // 5. REPAYMENTS
    // ==========================================

    suspend fun saveRepaymentRecord(payment: PaymentRecord): Result<Unit> {
        return try {
            val docRef = firestore.collection(COLLECTION_REPAYMENTS)
                .document("${payment.loan_id}_${payment.payment_date}")

            val paymentData = hashMapOf(
                "loan_id" to payment.loan_id,
                "borrower_id" to payment.borrower_id,
                "amount_paid" to payment.amount_paid,
                "payment_date" to payment.payment_date,
                "payment_method" to payment.payment_method,
                "reference" to payment.reference,
                "recorded_by" to payment.recorded_by,
                "notes" to payment.notes,
                "created_at" to System.currentTimeMillis()
            )

            docRef.set(paymentData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving repayment record to Firestore", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // 6. AUDIT TRAIL
    // ==========================================

    suspend fun logAuditEvent(log: AuditLog): Result<Unit> {
        return try {
            val docRef = firestore.collection(COLLECTION_AUDIT_LOGS).document()
            val logData = hashMapOf(
                "user_name" to log.user_name,
                "action" to log.action,
                "record_affected" to log.record_affected,
                "details" to log.details,
                "timestamp" to log.timestamp
            )
            docRef.set(logData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error logging audit event to Firestore", e)
            Result.failure(e)
        }
    }

    // ==========================================
    // 7. LENDING DOCUMENTS METADATA
    // ==========================================

    suspend fun saveDocumentMetadata(doc: LendingDocument): Result<Unit> {
        return try {
            val docRef = firestore.collection(COLLECTION_DOCUMENTS).document()
            val docData = hashMapOf(
                "title" to doc.title,
                "document_type" to doc.document_type,
                "file_reference" to doc.file_reference,
                "borrower_id" to doc.borrower_id,
                "loan_id" to doc.loan_id,
                "agreement_id" to doc.agreement_id,
                "created_at" to doc.created_at
            )
            docRef.set(docData).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving document metadata to Firestore", e)
            Result.failure(e)
        }
    }
}
