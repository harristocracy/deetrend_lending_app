package com.example.domain.agreement

import com.example.data.model.AmortizationType
import com.example.data.model.ApprovedLoan
import com.example.data.model.DeetrendInstallment
import com.example.data.model.RepaymentFrequency
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DeetrendAgreementTemplate {

    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.US)

    /**
     * Generates an immutable snapshot of the legal loan agreement for Deetrend Global Enterprise.
     */
    fun generateAgreementContent(
        agreementId: String,
        loan: ApprovedLoan,
        installments: List<DeetrendInstallment>
    ): String {
        val issueDateStr = dateFormatter.format(Date(loan.date_issued))
        val dueDateStr = dateFormatter.format(Date(loan.due_date))
        val divider = "=".repeat(68)
        val subDivider = "-".repeat(68)

        val builder = StringBuilder()
        builder.appendLine(divider)
        builder.appendLine("             DEETREND GLOBAL ENTERPRISE")
        builder.appendLine("             LOAN AGREEMENT & PROMISSORY NOTE")
        builder.appendLine("           Agreement Reference: $agreementId")
        builder.appendLine("           Loan Reference:      ${loan.loan_id}")
        builder.appendLine(divider)
        builder.appendLine()

        builder.appendLine("1. PARTIES TO THIS AGREEMENT")
        builder.appendLine(subDivider)
        builder.appendLine("LENDER:  Deetrend Global Enterprise")
        builder.appendLine("         Commercial Lending & Financing Unit")
        builder.appendLine()
        builder.appendLine("BORROWER: ${loan.borrower_name}")
        builder.appendLine("          Phone: ${loan.phone_number}")
        builder.appendLine()

        builder.appendLine("2. APPROVED LOAN TERMS")
        builder.appendLine(subDivider)
        builder.appendLine(String.format(Locale.US, "  %-28s : ₦%,.2f", "Principal Amount Financed", loan.principal_amount))
        builder.appendLine(String.format(Locale.US, "  %-28s : %.2f%% per month", "Monthly Interest Rate", loan.interest_rate_monthly))
        builder.appendLine(String.format(Locale.US, "  %-28s : %s", "Amortization Type", if (loan.amortization_type == AmortizationType.FLAT_INTEREST) "Flat Interest" else "Reducing Balance"))
        builder.appendLine(String.format(Locale.US, "  %-28s : %s", "Repayment Frequency", if (loan.repayment_frequency == RepaymentFrequency.WEEKLY) "Weekly" else "Monthly"))
        builder.appendLine(String.format(Locale.US, "  %-28s : %d Months", "Loan Tenure", loan.loan_tenure_months))
        builder.appendLine(String.format(Locale.US, "  %-28s : %s", "Date Issued", issueDateStr))
        builder.appendLine(String.format(Locale.US, "  %-28s : %s", "Due Date", dueDateStr))
        builder.appendLine(String.format(Locale.US, "  %-28s : ₦%,.2f", "Total Repayment Obligation", loan.total_repayment))
        builder.appendLine()

        builder.appendLine("3. COLLATERAL PROVISION")
        builder.appendLine(subDivider)
        if (loan.collateral_description.isNotBlank()) {
            builder.appendLine("  Collateral Assigned: ${loan.collateral_description}")
        } else {
            builder.appendLine("  No collateral provided.")
        }
        builder.appendLine()

        builder.appendLine("4. TERMS AND CONDITIONS")
        builder.appendLine(subDivider)
        builder.appendLine("a. Repayment Obligation: The Borrower unconditionally agrees and promises to repay the Total Repayment Obligation to Deetrend Global Enterprise in accordance with the specified schedule.")
        builder.appendLine("b. Repayment Schedule: Repayments must be remitted on or before each designated periodic installment due date into the authorized company bank accounts.")
        builder.appendLine("c. Late Payment & Default: Failure to pay any installment when due shall constitute default. Deetrend Global Enterprise reserves the right to levy standard default administrative surcharges and accelerate collection.")
        builder.appendLine("d. Collateral Rights: If collateral has been assigned, the Lender retains equitable right to claim or liquidate said collateral in the un-cured event of persistent loan default.")
        builder.appendLine("e. Borrower Obligations: The Borrower warrants that all bank account credentials, business details, and identity documents submitted are valid, accurate, and current.")
        builder.appendLine("f. Lender Rights: Deetrend Global Enterprise may verify employment status, business operations, and credit standing throughout the loan lifecycle.")
        builder.appendLine("g. Electronic Consent: Submitting this agreement electronically with the borrower's digital acknowledgement constitutes full mutual legal consent.")
        builder.appendLine()

        if (installments.isNotEmpty()) {
            builder.appendLine("5. REPAYMENT SCHEDULE PREVIEW")
            builder.appendLine(subDivider)
            builder.appendLine(String.format(Locale.US, "  %-4s %-12s %-14s %-14s %-14s", "#", "Due Date", "Principal", "Interest", "Total Due"))
            val shortDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            for (inst in installments.take(12)) {
                builder.appendLine(
                    String.format(
                        Locale.US,
                        "  %-4d %-12s ₦%,-13.2f ₦%,-13.2f ₦%,-13.2f",
                        inst.installment_number,
                        shortDate.format(Date(inst.due_date_millis)),
                        inst.principal_component,
                        inst.interest_component,
                        inst.total_installment
                    )
                )
            }
            if (installments.size > 12) {
                builder.appendLine("  ... (${installments.size - 12} additional installments in schedule)")
            }
            builder.appendLine()
        }

        builder.appendLine(divider)
        builder.appendLine("          END OF DEETREND GLOBAL ENTERPRISE AGREEMENT")
        builder.appendLine(divider)

        return builder.toString()
    }
}
