package com.example.data.remote

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await

/**
 * Service to handle Firebase Cloud Storage operations for Deetrend Global Enterprise.
 * Stores:
 * - Promissory agreements and legal contracts (`agreements/{agreement_id}.txt` or `.pdf`)
 * - Borrower Identity documents (`borrowers/{borrower_id}/id_card.jpg`)
 * - Collateral inspection photos (`collateral/{loan_id}/collateral_evidence.jpg`)
 * - Repayment receipts and proof of payment (`repayments/{loan_id}_{timestamp}.pdf`)
 */
class DeetrendFirebaseStorageService(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {
    companion object {
        private const val TAG = "DeetrendStorage"
        private const val BUCKET_AGREEMENTS = "agreements"
        private const val BUCKET_DOCUMENTS = "lending_documents"
        private const val BUCKET_RECEIPTS = "repayment_receipts"
        private const val BUCKET_COLLATERAL = "collateral_records"
    }

    /**
     * Upload promissory note text or contract document to Firebase Storage.
     * Returns the remote public download URL.
     */
    suspend fun uploadAgreementDocument(
        agreementId: String,
        content: String,
        borrowerName: String
    ): Result<String> {
        return try {
            val storageRef = storage.reference.child("$BUCKET_AGREEMENTS/$agreementId.txt")
            val bytes = content.toByteArray(Charsets.UTF_8)

            val metadata = StorageMetadata.Builder()
                .setContentType("text/plain")
                .setCustomMetadata("borrowerName", borrowerName)
                .setCustomMetadata("agreementId", agreementId)
                .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                .build()

            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d(TAG, "Uploaded agreement to Cloud Storage: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload agreement to Cloud Storage", e)
            Result.failure(e)
        }
    }

    /**
     * Upload borrower document (e.g. proof of identity or collateral image) from device URI.
     */
    suspend fun uploadBorrowerFile(
        borrowerId: Long,
        fileName: String,
        fileUri: Uri,
        mimeType: String = "image/jpeg"
    ): Result<String> {
        return try {
            val storageRef = storage.reference.child("$BUCKET_DOCUMENTS/borrower_$borrowerId/$fileName")
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType)
                .setCustomMetadata("borrowerId", borrowerId.toString())
                .build()

            storageRef.putFile(fileUri, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload borrower file to Cloud Storage", e)
            Result.failure(e)
        }
    }

    /**
     * Upload repayment proof or receipt data.
     */
    suspend fun uploadRepaymentReceipt(
        loanId: String,
        receiptNumber: String,
        receiptText: String
    ): Result<String> {
        return try {
            val storageRef = storage.reference.child("$BUCKET_RECEIPTS/${loanId}_$receiptNumber.txt")
            val bytes = receiptText.toByteArray(Charsets.UTF_8)
            val metadata = StorageMetadata.Builder()
                .setContentType("text/plain")
                .setCustomMetadata("loanId", loanId)
                .setCustomMetadata("receiptNumber", receiptNumber)
                .build()

            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload repayment receipt to Cloud Storage", e)
            Result.failure(e)
        }
    }
}
