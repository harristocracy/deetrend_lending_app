package com.example.domain.export

import com.example.data.model.AmortizationInstallment
import com.example.data.model.LoanApplication
import com.example.data.model.PaymentTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Structured document representing the finalized loan agreement.
 */
data class LoanAgreementDocument(
    val agreementTitle: String,
    val contractNumber: String,
    val executionDateFormatted: String,
    val statusLabel: String,
    val lenderName: String,
    val lenderLicense: String,
    val lenderContact: String,
    val borrowerName: String,
    val borrowerEmail: String,
    val borrowerPhone: String,
    val borrowerIdNumber: String,
    val employmentStatus: String,
    val monthlyIncomeFormatted: String,
    val loanPurpose: String,
    val principalAmountFormatted: String,
    val calculatedAnnualRateFormatted: String,
    val termMonths: Int,
    val monthlyPaymentFormatted: String,
    val totalInterestFormatted: String,
    val totalRepaymentFormatted: String,
    val clauses: List<Pair<String, String>>,
    val signatureName: String?,
    val signedAtFormatted: String?,
    val isSigned: Boolean,
    val amortizationPreview: List<AmortizationRowSummary> = emptyList()
)

data class AmortizationRowSummary(
    val installmentNumber: Int,
    val dueDateFormatted: String,
    val principalPartFormatted: String,
    val interestPartFormatted: String,
    val totalPaymentFormatted: String,
    val remainingBalanceFormatted: String,
    val status: String
)

object LoanAgreementExporter {

    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
    private val shortDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Maps a LoanApplication domain model to an immutable formatted LoanAgreementDocument structure.
     */
    fun toDocument(
        loan: LoanApplication,
        installments: List<AmortizationInstallment> = emptyList()
    ): LoanAgreementDocument {
        val dateCreated = dateFormatter.format(Date(loan.createdAtMillis))
        val dateSigned = loan.agreementSignedAtMillis?.let { dateFormatter.format(Date(it)) }

        val clauses = listOf(
            "1. PROMISE TO PAY" to "For value received, the Borrower unconditionally promises to pay to the order of the Lender the principal sum of \$${String.format(Locale.US, "%,.2f", loan.principalAmount)} with interest computed on the unpaid principal balance at the fixed annual rate of ${loan.calculatedInterestRate}%. Repayment shall be executed in ${loan.termMonths} consecutive monthly installments of \$${String.format(Locale.US, "%,.2f", loan.monthlyPayment)}.",
            "2. PREPAYMENT PRIVILEGE" to "The Borrower shall have the full right to prepay the unpaid principal balance, in whole or in part, at any scheduled payment date or intermediate time without incurrence of penalty, premium, or prepayment surcharge.",
            "3. DELINQUENCY & LATE PENALTY" to "In the event any installment is not tendered within fifteen (15) calendar days following its designated due date, a late charge equal to 5.0% of the delinquent payment installment shall be assessed and added to the balance.",
            "4. COVENANT OF INTENDED PURPOSE" to "The Borrower explicitly warrants and covenants that all proceeds disbursed under this lending agreement shall be utilized exclusively for the designated purpose of: ${loan.loanPurpose}.",
            "5. GOVERNING LAW & JURISDICTION" to "This agreement and promissory note shall be governed by, construed, and enforced in accordance with the substantive financial regulations and applicable laws of the presiding jurisdiction."
        )

        val scheduleSummary = installments.map { inst ->
            AmortizationRowSummary(
                installmentNumber = inst.installmentNumber,
                dueDateFormatted = shortDateFormatter.format(Date(inst.dueDateMillis)),
                principalPartFormatted = String.format(Locale.US, "$%,.2f", inst.principalPart),
                interestPartFormatted = String.format(Locale.US, "$%,.2f", inst.interestPart),
                totalPaymentFormatted = String.format(Locale.US, "$%,.2f", inst.totalPayment),
                remainingBalanceFormatted = String.format(Locale.US, "$%,.2f", inst.endingBalance),
                status = if (inst.isPaid) "PAID" else "PENDING"
            )
        }

        return LoanAgreementDocument(
            agreementTitle = "MASTER PROMISSORY NOTE & LOAN AGREEMENT",
            contractNumber = "LN-${loan.id.toString().padStart(6, '0')}",
            executionDateFormatted = dateCreated,
            statusLabel = loan.status.name,
            lenderName = "Apex Lending Services Inc.",
            lenderLicense = "Financial Lending License #FL-94028",
            lenderContact = "support@apexlending.local",
            borrowerName = loan.borrowerName,
            borrowerEmail = loan.borrowerEmail,
            borrowerPhone = loan.borrowerPhone.ifEmpty { "Not Provided" },
            borrowerIdNumber = loan.borrowerIdNumber.ifEmpty { "N/A" },
            employmentStatus = loan.employmentStatus.ifEmpty { "Standard" },
            monthlyIncomeFormatted = String.format(Locale.US, "$%,.2f", loan.monthlyIncome),
            loanPurpose = loan.loanPurpose,
            principalAmountFormatted = String.format(Locale.US, "$%,.2f", loan.principalAmount),
            calculatedAnnualRateFormatted = "${loan.calculatedInterestRate}%",
            termMonths = loan.termMonths,
            monthlyPaymentFormatted = String.format(Locale.US, "$%,.2f", loan.monthlyPayment),
            totalInterestFormatted = String.format(Locale.US, "$%,.2f", loan.totalInterest),
            totalRepaymentFormatted = String.format(Locale.US, "$%,.2f", loan.totalRepayment),
            clauses = clauses,
            signatureName = loan.signatureName,
            signedAtFormatted = dateSigned,
            isSigned = loan.signatureName != null && loan.agreementSignedAtMillis != null,
            amortizationPreview = scheduleSummary
        )
    }

    /**
     * Exports the document into a clean, formatted plain text contract representation
     * suitable for sharing, clipboard copying, or document printing.
     */
    fun exportToFormattedText(document: LoanAgreementDocument): String {
        val divider = "=".repeat(68)
        val subDivider = "-".repeat(68)

        val builder = StringBuilder()
        builder.appendLine(divider)
        builder.appendLine("           ${document.agreementTitle}")
        builder.appendLine("                 Contract No: ${document.contractNumber}")
        builder.appendLine("                 Date of Issue: ${document.executionDateFormatted}")
        builder.appendLine("                 Status: [${document.statusLabel}]")
        builder.appendLine(divider)
        builder.appendLine()

        builder.appendLine("1. PARTIES")
        builder.appendLine(subDivider)
        builder.appendLine("LENDER:")
        builder.appendLine("  Entity:   ${document.lenderName}")
        builder.appendLine("  License:  ${document.lenderLicense}")
        builder.appendLine("  Contact:  ${document.lenderContact}")
        builder.appendLine()
        builder.appendLine("BORROWER:")
        builder.appendLine("  Full Name:       ${document.borrowerName}")
        builder.appendLine("  Email:           ${document.borrowerEmail}")
        builder.appendLine("  Phone:           ${document.borrowerPhone}")
        builder.appendLine("  National ID/SSN: ${document.borrowerIdNumber}")
        builder.appendLine("  Employment:      ${document.employmentStatus}")
        builder.appendLine("  Monthly Income:  ${document.monthlyIncomeFormatted}")
        builder.appendLine()

        builder.appendLine("2. TRUTH-IN-LENDING DISCLOSURES & TERMS")
        builder.appendLine(subDivider)
        builder.appendLine(String.format(Locale.US, "  %-32s : %s", "Principal Amount Financed", document.principalAmountFormatted))
        builder.appendLine(String.format(Locale.US, "  %-32s : %s", "Annual Percentage Rate (APR)", document.calculatedAnnualRateFormatted))
        builder.appendLine(String.format(Locale.US, "  %-32s : %d Months", "Repayment Term Duration", document.termMonths))
        builder.appendLine(String.format(Locale.US, "  %-32s : %s", "Monthly Installment (EMI)", document.monthlyPaymentFormatted))
        builder.appendLine(String.format(Locale.US, "  %-32s : %s", "Total Finance Charge (Interest)", document.totalInterestFormatted))
        builder.appendLine(String.format(Locale.US, "  %-32s : %s", "Total Payments Obligation", document.totalRepaymentFormatted))
        builder.appendLine(String.format(Locale.US, "  %-32s : %s", "Stated Purpose of Loan", document.loanPurpose))
        builder.appendLine()

        builder.appendLine("3. CONTRACTUAL TERMS & PROVISIONS")
        builder.appendLine(subDivider)
        for ((title, clause) in document.clauses) {
            builder.appendLine(title)
            builder.appendLine(clause)
            builder.appendLine()
        }

        if (document.amortizationPreview.isNotEmpty()) {
            builder.appendLine("4. AMORTIZATION REPAYMENT SCHEDULE SUMMARY")
            builder.appendLine(subDivider)
            builder.appendLine(
                String.format(
                    Locale.US,
                    "  %-4s  %-12s  %-12s  %-12s  %-12s  %-8s",
                    "#", "Due Date", "Principal", "Interest", "Payment", "Status"
                )
            )
            for (row in document.amortizationPreview) {
                builder.appendLine(
                    String.format(
                        Locale.US,
                        "  %-4d  %-12s  %-12s  %-12s  %-12s  %-8s",
                        row.installmentNumber,
                        row.dueDateFormatted,
                        row.principalPartFormatted,
                        row.interestPartFormatted,
                        row.totalPaymentFormatted,
                        row.status
                    )
                )
            }
            builder.appendLine()
        }

        builder.appendLine("5. EXECUTION & ELECTRONIC SIGNATURE")
        builder.appendLine(subDivider)
        if (document.isSigned) {
            builder.appendLine("  [X] ELECTRONICALLY SIGNED AND EXECUTED")
            builder.appendLine("  Signatory: ${document.signatureName}")
            builder.appendLine("  Signed On: ${document.signedAtFormatted}")
            builder.appendLine("  Verification: Cryptographically Logged & Stored Locally")
        } else {
            builder.appendLine("  [ ] PENDING SIGNATURE")
            builder.appendLine("  Borrower Signature: ____________________________")
            builder.appendLine("  Date:               ____________________________")
        }
        builder.appendLine()
        builder.appendLine(divider)
        builder.appendLine("           END OF LOAN AGREEMENT CONTRACT")
        builder.appendLine(divider)

        return builder.toString()
    }

    /**
     * Exports the document into a rich HTML formatted document structure
     * suitable for WebView rendering, web viewing, or exporting to PDF/print.
     */
    fun exportToHtml(document: LoanAgreementDocument): String {
        val scheduleHtml = if (document.amortizationPreview.isNotEmpty()) {
            buildString {
                append("""
                    <h3>4. Amortization Repayment Schedule</h3>
                    <table style="width:100%; border-collapse: collapse; margin-top: 10px; font-size: 13px;">
                        <thead>
                            <tr style="background-color: #f1f5f9; text-align: left;">
                                <th style="border: 1px solid #cbd5e1; padding: 8px;">#</th>
                                <th style="border: 1px solid #cbd5e1; padding: 8px;">Due Date</th>
                                <th style="border: 1px solid #cbd5e1; padding: 8px;">Principal</th>
                                <th style="border: 1px solid #cbd5e1; padding: 8px;">Interest</th>
                                <th style="border: 1px solid #cbd5e1; padding: 8px;">Total Due</th>
                                <th style="border: 1px solid #cbd5e1; padding: 8px;">Status</th>
                            </tr>
                        </thead>
                        <tbody>
                """.trimIndent())
                for (row in document.amortizationPreview) {
                    append("""
                        <tr>
                            <td style="border: 1px solid #cbd5e1; padding: 6px;">${row.installmentNumber}</td>
                            <td style="border: 1px solid #cbd5e1; padding: 6px;">${row.dueDateFormatted}</td>
                            <td style="border: 1px solid #cbd5e1; padding: 6px;">${row.principalPartFormatted}</td>
                            <td style="border: 1px solid #cbd5e1; padding: 6px;">${row.interestPartFormatted}</td>
                            <td style="border: 1px solid #cbd5e1; padding: 6px; font-weight: bold;">${row.totalPaymentFormatted}</td>
                            <td style="border: 1px solid #cbd5e1; padding: 6px;">${row.status}</td>
                        </tr>
                    """.trimIndent())
                }
                append("""
                        </tbody>
                    </table>
                """.trimIndent())
            }
        } else ""

        val clausesHtml = document.clauses.joinToString("\n") { (num, text) ->
            """
            <div style="margin-bottom: 12px;">
                <div style="font-weight: bold; color: #1e293b; margin-bottom: 2px;">$num</div>
                <div style="color: #475569; line-height: 1.5; font-size: 14px;">$text</div>
            </div>
            """.trimIndent()
        }

        return """<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>${document.contractNumber} - ${document.agreementTitle}</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
                        color: #0f172a;
                        margin: 24px;
                        line-height: 1.6;
                    }
                    .header {
                        text-align: center;
                        border-bottom: 2px solid #0284c7;
                        padding-bottom: 16px;
                        margin-bottom: 20px;
                    }
                    .badge {
                        display: inline-block;
                        background-color: #e0f2fe;
                        color: #0369a1;
                        padding: 4px 10px;
                        border-radius: 4px;
                        font-size: 12px;
                        font-weight: bold;
                    }
                    .grid {
                        display: flex;
                        gap: 20px;
                        margin-bottom: 20px;
                    }
                    .card {
                        flex: 1;
                        background: #f8fafc;
                        border: 1px solid #e2e8f0;
                        padding: 14px;
                        border-radius: 6px;
                    }
                    .metric-table td {
                        padding: 6px 12px;
                    }
                    .metric-title {
                        color: #64748b;
                        font-size: 13px;
                    }
                    .metric-value {
                        font-size: 16px;
                        font-weight: bold;
                        color: #0f172a;
                    }
                    .signature-box {
                        margin-top: 24px;
                        border: 2px dashed #0284c7;
                        background: #f0f9ff;
                        padding: 16px;
                        border-radius: 8px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2 style="margin: 0 0 6px 0;">${document.agreementTitle}</h2>
                    <div>Contract No: <strong>${document.contractNumber}</strong> | Issued: ${document.executionDateFormatted}</div>
                    <div style="margin-top: 8px;"><span class="badge">${document.statusLabel}</span></div>
                </div>

                <div class="grid">
                    <div class="card">
                        <div style="font-weight: bold; color: #0284c7; margin-bottom: 6px;">LENDER</div>
                        <div><strong>${document.lenderName}</strong></div>
                        <div>${document.lenderLicense}</div>
                        <div>Email: ${document.lenderContact}</div>
                    </div>
                    <div class="card">
                        <div style="font-weight: bold; color: #0284c7; margin-bottom: 6px;">BORROWER</div>
                        <div><strong>${document.borrowerName}</strong></div>
                        <div>Govt ID: ${document.borrowerIdNumber}</div>
                        <div>Email: ${document.borrowerEmail} | Phone: ${document.borrowerPhone}</div>
                        <div>Income: ${document.monthlyIncomeFormatted}/mo (${document.employmentStatus})</div>
                    </div>
                </div>

                <h3>2. Truth-In-Lending Disclosures</h3>
                <div class="card">
                    <table class="metric-table" style="width: 100%;">
                        <tr>
                            <td><span class="metric-title">Principal Financed:</span></td>
                            <td><span class="metric-value">${document.principalAmountFormatted}</span></td>
                            <td><span class="metric-title">Annual Rate (APR):</span></td>
                            <td><span class="metric-value">${document.calculatedAnnualRateFormatted}</span></td>
                        </tr>
                        <tr>
                            <td><span class="metric-title">Monthly Installment:</span></td>
                            <td><span class="metric-value">${document.monthlyPaymentFormatted}</span></td>
                            <td><span class="metric-title">Repayment Term:</span></td>
                            <td><span class="metric-value">${document.termMonths} Months</span></td>
                        </tr>
                        <tr>
                            <td><span class="metric-title">Finance Charge (Interest):</span></td>
                            <td><span class="metric-value">${document.totalInterestFormatted}</span></td>
                            <td><span class="metric-title">Total Repayments:</span></td>
                            <td><span class="metric-value" style="color: #0369a1;">${document.totalRepaymentFormatted}</span></td>
                        </tr>
                    </table>
                </div>

                <h3>3. Terms & Conditions</h3>
                $clausesHtml

                $scheduleHtml

                <div class="signature-box">
                    <div style="font-weight: bold; color: #0369a1; font-size: 16px; margin-bottom: 4px;">
                        ${if (document.isSigned) "✓ Digitally Signed & Executed" else "Pending Execution"}
                    </div>
                    <div>Signatory: <strong>${document.signatureName ?: document.borrowerName}</strong></div>
                    <div>Date of Execution: ${document.signedAtFormatted ?: "Pending"}</div>
                    <div style="font-size: 12px; color: #64748b; margin-top: 4px;">
                        This document serves as an official legal record of the loan commitment and repayment obligation.
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
}
