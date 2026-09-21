package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AmortizationInstallment
import com.example.data.model.LoanApplication
import com.example.data.model.LoanReminder
import com.example.data.model.PaymentTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {

    // Loans
    @Query("SELECT * FROM loans ORDER BY createdAtMillis DESC")
    fun getAllLoans(): Flow<List<LoanApplication>>

    @Query("SELECT * FROM loans WHERE id = :loanId LIMIT 1")
    fun getLoanById(loanId: Long): Flow<LoanApplication?>

    @Query("SELECT * FROM loans WHERE id = :loanId LIMIT 1")
    suspend fun getLoanByIdOnce(loanId: Long): LoanApplication?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoan(loan: LoanApplication): Long

    @Update
    suspend fun updateLoan(loan: LoanApplication)

    @Query("DELETE FROM loans WHERE id = :loanId")
    suspend fun deleteLoanById(loanId: Long)

    // Amortization Installments
    @Query("SELECT * FROM amortization_installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    fun getInstallmentsForLoan(loanId: Long): Flow<List<AmortizationInstallment>>

    @Query("SELECT * FROM amortization_installments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    suspend fun getInstallmentsForLoanOnce(loanId: Long): List<AmortizationInstallment>

    @Query("SELECT * FROM amortization_installments WHERE isPaid = 0 ORDER BY dueDateMillis ASC")
    fun getAllUnpaidInstallments(): Flow<List<AmortizationInstallment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<AmortizationInstallment>)

    @Update
    suspend fun updateInstallment(installment: AmortizationInstallment)

    @Query("DELETE FROM amortization_installments WHERE loanId = :loanId")
    suspend fun deleteInstallmentsByLoanId(loanId: Long)

    // Payments
    @Query("SELECT * FROM payment_transactions WHERE loanId = :loanId ORDER BY paymentDateMillis DESC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions ORDER BY paymentDateMillis DESC")
    fun getAllPayments(): Flow<List<PaymentTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentTransaction): Long

    // Reminders
    @Query("SELECT * FROM loan_reminders WHERE isDismissed = 0 ORDER BY dueDateMillis ASC")
    fun getActiveReminders(): Flow<List<LoanReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<LoanReminder>)

    @Query("UPDATE loan_reminders SET isDismissed = 1 WHERE id = :reminderId")
    suspend fun dismissReminder(reminderId: Long)

    @Query("DELETE FROM loan_reminders WHERE loanId = :loanId")
    suspend fun deleteRemindersByLoanId(loanId: Long)
}
