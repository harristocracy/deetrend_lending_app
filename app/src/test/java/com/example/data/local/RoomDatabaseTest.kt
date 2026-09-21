package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AmortizationInstallment
import com.example.data.model.LoanApplication
import com.example.data.model.LoanReminder
import com.example.data.model.LoanStatus
import com.example.data.model.PaymentTransaction
import com.example.data.model.ReminderType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseTest {

    private lateinit var db: LoanDatabase
    private lateinit var loanDao: LoanDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LoanDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        loanDao = db.loanDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndRetrieveLoanApplication() = runBlocking {
        val loan = LoanApplication(
            borrowerName = "John Doe",
            borrowerEmail = "john@example.com",
            borrowerPhone = "1234567890",
            borrowerIdNumber = "ID-998877",
            employmentStatus = "Employed",
            monthlyIncome = 5000.0,
            loanPurpose = "Education",
            principalAmount = 10000.0,
            termMonths = 12,
            calculatedInterestRate = 8.5,
            monthlyPayment = 872.20,
            totalRepayment = 10466.40,
            totalInterest = 466.40,
            status = LoanStatus.AGREEMENT_PENDING
        )

        val id = loanDao.insertLoan(loan)
        assertTrue(id > 0)

        val retrieved = loanDao.getLoanByIdOnce(id)
        assertNotNull(retrieved)
        assertEquals("John Doe", retrieved?.borrowerName)
        assertEquals(10000.0, retrieved?.principalAmount ?: 0.0, 0.01)

        val allLoans = loanDao.getAllLoans().first()
        assertEquals(1, allLoans.size)
    }

    @Test
    fun insertAndRetrieveInstallments() = runBlocking {
        val installments = listOf(
            AmortizationInstallment(
                loanId = 1L,
                installmentNumber = 1,
                dueDateMillis = System.currentTimeMillis() + 86400000L,
                beginningBalance = 5000.0,
                principalPart = 400.0,
                interestPart = 35.0,
                totalPayment = 435.0,
                endingBalance = 4600.0,
                isPaid = false
            ),
            AmortizationInstallment(
                loanId = 1L,
                installmentNumber = 2,
                dueDateMillis = System.currentTimeMillis() + 86400000L * 30,
                beginningBalance = 4600.0,
                principalPart = 405.0,
                interestPart = 30.0,
                totalPayment = 435.0,
                endingBalance = 4195.0,
                isPaid = false
            )
        )

        loanDao.insertInstallments(installments)

        val retrieved = loanDao.getInstallmentsForLoanOnce(1L)
        assertEquals(2, retrieved.size)
        assertEquals(1, retrieved[0].installmentNumber)
        assertEquals(2, retrieved[1].installmentNumber)

        // Update installment as paid
        val updated = retrieved[0].copy(isPaid = true, paidAmount = 435.0)
        loanDao.updateInstallment(updated)

        val unpaid = loanDao.getAllUnpaidInstallments().first()
        assertEquals(1, unpaid.size)
        assertEquals(2, unpaid[0].installmentNumber)
    }

    @Test
    fun insertAndRetrievePayments() = runBlocking {
        val payment = PaymentTransaction(
            loanId = 1L,
            installmentId = 1L,
            installmentNumber = 1,
            amountPaid = 435.0,
            paymentMethod = "Bank Transfer",
            referenceNote = "Month 1 payment",
            receiptNumber = "REC-12345"
        )

        val paymentId = loanDao.insertPayment(payment)
        assertTrue(paymentId > 0)

        val payments = loanDao.getPaymentsForLoan(1L).first()
        assertEquals(1, payments.size)
        assertEquals("REC-12345", payments[0].receiptNumber)
    }

    @Test
    fun insertAndDismissReminders() = runBlocking {
        val reminder = LoanReminder(
            loanId = 1L,
            installmentNumber = 1,
            borrowerName = "John Doe",
            amountDue = 435.0,
            dueDateMillis = System.currentTimeMillis() + 86400000L,
            reminderType = ReminderType.UPCOMING_3_DAYS,
            title = "Upcoming Due Date",
            message = "Payment due soon"
        )

        loanDao.insertReminders(listOf(reminder))

        val active = loanDao.getActiveReminders().first()
        assertEquals(1, active.size)

        loanDao.dismissReminder(active[0].id)
        val activeAfterDismiss = loanDao.getActiveReminders().first()
        assertEquals(0, activeAfterDismiss.size)
    }
}
