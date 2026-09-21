package com.example.domain.export

import com.example.data.model.AmortizationInstallment
import com.example.data.model.LoanApplication
import com.example.data.model.LoanStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoanAgreementExporterTest {

    @Test
    fun toDocument_createsValidAgreementDocumentStructure() {
        val loan = LoanApplication(
            id = 42L,
            borrowerName = "Alice Smith",
            borrowerEmail = "alice@example.com",
            borrowerPhone = "555-0199",
            borrowerIdNumber = "ID-112233",
            employmentStatus = "Employed",
            monthlyIncome = 6500.0,
            loanPurpose = "Home Improvement",
            principalAmount = 15000.0,
            termMonths = 24,
            calculatedInterestRate = 7.5,
            monthlyPayment = 675.0,
            totalRepayment = 16200.0,
            totalInterest = 1200.0,
            status = LoanStatus.ACTIVE,
            signatureName = "Alice Smith",
            agreementSignedAtMillis = 1700000000000L
        )

        val installments = listOf(
            AmortizationInstallment(
                loanId = 42L,
                installmentNumber = 1,
                dueDateMillis = 1702500000000L,
                beginningBalance = 15000.0,
                principalPart = 581.25,
                interestPart = 93.75,
                totalPayment = 675.0,
                endingBalance = 14418.75,
                isPaid = true
            )
        )

        val doc = LoanAgreementExporter.toDocument(loan, installments)

        assertEquals("LN-000042", doc.contractNumber)
        assertEquals("Alice Smith", doc.borrowerName)
        assertEquals("alice@example.com", doc.borrowerEmail)
        assertEquals("Home Improvement", doc.loanPurpose)
        assertEquals("$15,000.00", doc.principalAmountFormatted)
        assertEquals("7.5%", doc.calculatedAnnualRateFormatted)
        assertEquals(24, doc.termMonths)
        assertEquals("$675.00", doc.monthlyPaymentFormatted)
        assertEquals("$16,200.00", doc.totalRepaymentFormatted)
        assertTrue(doc.isSigned)
        assertEquals("Alice Smith", doc.signatureName)
        assertNotNull(doc.signedAtFormatted)
        assertEquals(5, doc.clauses.size)
        assertEquals(1, doc.amortizationPreview.size)
        assertEquals("PAID", doc.amortizationPreview[0].status)
    }

    @Test
    fun exportToFormattedText_generatesCleanContractFormat() {
        val loan = LoanApplication(
            id = 101L,
            borrowerName = "Bob Johnson",
            borrowerEmail = "bob@example.com",
            borrowerPhone = "555-8822",
            borrowerIdNumber = "ID-999000",
            employmentStatus = "Self-Employed",
            monthlyIncome = 4000.0,
            loanPurpose = "Business Equipment",
            principalAmount = 5000.0,
            termMonths = 12,
            calculatedInterestRate = 8.0,
            monthlyPayment = 434.94,
            totalRepayment = 5219.28,
            totalInterest = 219.28,
            status = LoanStatus.AGREEMENT_PENDING
        )

        val doc = LoanAgreementExporter.toDocument(loan)
        assertFalse(doc.isSigned)

        val text = LoanAgreementExporter.exportToFormattedText(doc)
        assertTrue(text.contains("MASTER PROMISSORY NOTE & LOAN AGREEMENT"))
        assertTrue(text.contains("Contract No: LN-000101"))
        assertTrue(text.contains("Bob Johnson"))
        assertTrue(text.contains("Business Equipment"))
        assertTrue(text.contains("$5,000.00"))
        assertTrue(text.contains("1. PROMISE TO PAY"))
        assertTrue(text.contains("2. PREPAYMENT PRIVILEGE"))
        assertTrue(text.contains("PENDING SIGNATURE"))
    }

    @Test
    fun exportToHtml_generatesValidHtmlDocument() {
        val loan = LoanApplication(
            id = 5L,
            borrowerName = "Clara Oswald",
            borrowerEmail = "clara@tardis.org",
            borrowerPhone = "555-3141",
            borrowerIdNumber = "ID-778899",
            employmentStatus = "Teacher",
            monthlyIncome = 4800.0,
            loanPurpose = "Education",
            principalAmount = 8000.0,
            termMonths = 18,
            calculatedInterestRate = 7.0,
            monthlyPayment = 469.12,
            totalRepayment = 8444.16,
            totalInterest = 444.16,
            status = LoanStatus.ACTIVE,
            signatureName = "Clara Oswald",
            agreementSignedAtMillis = 1700000000000L
        )

        val doc = LoanAgreementExporter.toDocument(loan)
        val html = LoanAgreementExporter.exportToHtml(doc)

        assertTrue(html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("LN-000005"))
        assertTrue(html.contains("Clara Oswald"))
        assertTrue(html.contains("Digitally Signed & Executed"))
        assertTrue(html.contains("</html>"))
    }
}
