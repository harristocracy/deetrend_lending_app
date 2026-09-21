package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AmortizationType
import com.example.data.model.ApprovedLoan
import com.example.data.model.Borrower
import com.example.data.model.DeetrendInstallment
import com.example.data.model.LoanRecordStatus
import com.example.data.model.RepaymentFrequency
import com.example.domain.calculator.DeetrendLoanCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminLoanDetailScreen(
    loan: ApprovedLoan,
    borrower: Borrower?,
    installments: List<DeetrendInstallment>,
    onGenerateAgreement: () -> Unit,
    onRecordRepayment: (amount: Double, method: String, reference: String, notes: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("dd MMMM yyyy", Locale.US) }
    var showRepaymentDialog by remember { mutableStateOf(false) }
    var repaymentAmountInput by remember { mutableStateOf("") }
    var repaymentMethodInput by remember { mutableStateOf("Bank Transfer") }
    var repaymentReferenceInput by remember { mutableStateOf("") }

    val daysDiff = DeetrendLoanCalculator.getDaysDifference(loan.due_date)
    val dynamicStatus = when {
        loan.outstanding_balance <= 0.0 -> LoanRecordStatus.Settled
        daysDiff < 0 -> LoanRecordStatus.Overdue
        else -> LoanRecordStatus.Active
    }
    val daysRemainingStr = DeetrendLoanCalculator.formatDaysRemaining(loan.due_date)

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_loan_detail_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_detail_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Loan Profile: ${loan.loan_id}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Linked Application: ${loan.application_id}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LoanStatusBadge(dynamicStatus)
                }
            }

            // Quick Actions: Generate Loan Agreement / Record Repayment
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onGenerateAgreement,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_agreement_btn")
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Agreement")
                    }

                    OutlinedButton(
                        onClick = { showRepaymentDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("record_repayment_btn")
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Record Repayment")
                    }
                }
            }

            // Borrower Information Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Borrower Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        DetailRow("Full Name", loan.borrower_name)
                        DetailRow("Phone Number", loan.phone_number)
                        borrower?.let { b ->
                            DetailRow("Occupation", b.occupation_type)
                            DetailRow("Home Address", b.home_address)
                            if (b.work_or_business_location.isNotBlank()) {
                                DetailRow("Work/Business Location", b.work_or_business_location)
                            }
                            DetailRow("Bank Account", "${b.account_name} (${b.bank_name}) - ${b.account_number}")
                        }
                    }
                }
            }

            // Loan Information Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Loan Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        DetailRow("Loan ID", loan.loan_id)
                        DetailRow("Principal Amount", String.format(Locale.US, "₦%,.2f", loan.principal_amount))
                        DetailRow("Interest Rate", String.format(Locale.US, "%.2f%% per month", loan.interest_rate_monthly))
                        DetailRow("Amortization Type", if (loan.amortization_type == AmortizationType.FLAT_INTEREST) "Flat Interest" else "Reducing Balance")
                        DetailRow("Repayment Frequency", if (loan.repayment_frequency == RepaymentFrequency.WEEKLY) "Weekly" else "Monthly")
                        DetailRow("Loan Tenure", "${loan.loan_tenure_months} Months")
                        DetailRow("Date Issued", dateFormatter.format(Date(loan.date_issued)))
                        DetailRow("Due Date", dateFormatter.format(Date(loan.due_date)))
                        DetailRow("Days Remaining", daysRemainingStr)
                        DetailRow("Total Repayment", String.format(Locale.US, "₦%,.2f", loan.total_repayment))
                        DetailRow("Outstanding Balance", String.format(Locale.US, "₦%,.2f", loan.outstanding_balance))
                        DetailRow("Payment Status", loan.payment_status.name)
                        DetailRow("Loan Status", dynamicStatus.name)
                        DetailRow("Collateral", if (loan.collateral_description.isNotBlank()) loan.collateral_description else "No collateral provided.")
                    }
                }
            }

            // Repayment Schedule Table / Preview
            if (installments.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Amortization Schedule (${installments.size} Installments)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            installments.forEach { inst ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Inst #${inst.installment_number} • ${dateFormatter.format(Date(inst.due_date_millis))}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(String.format(Locale.US, "₦%,.2f", inst.total_installment), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Principal: ₦${String.format(Locale.US, "%,.2f", inst.principal_component)} | Interest: ₦${String.format(Locale.US, "%,.2f", inst.interest_component)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Close: ₦${String.format(Locale.US, "%,.2f", inst.closing_balance)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Repayment Dialog
        if (showRepaymentDialog) {
            AlertDialog(
                onDismissRequest = { showRepaymentDialog = false },
                title = { Text("Record Repayment") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Record a payment made against ${loan.loan_id}:")
                        OutlinedTextField(
                            value = repaymentAmountInput,
                            onValueChange = { repaymentAmountInput = it },
                            label = { Text("Amount Paid (₦)") },
                            placeholder = { Text("e.g. 50000") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = repaymentMethodInput,
                            onValueChange = { repaymentMethodInput = it },
                            label = { Text("Payment Method") },
                            placeholder = { Text("Bank Transfer / Cash / Cheque") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = repaymentReferenceInput,
                            onValueChange = { repaymentReferenceInput = it },
                            label = { Text("Reference / Bank Receipt") },
                            placeholder = { Text("e.g. NIP-TXN-882201") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val amt = repaymentAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            onRecordRepayment(amt, repaymentMethodInput, repaymentReferenceInput, "Admin manual entry")
                            showRepaymentDialog = false
                            repaymentAmountInput = ""
                        }
                    }) {
                        Text("Save Repayment")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRepaymentDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
