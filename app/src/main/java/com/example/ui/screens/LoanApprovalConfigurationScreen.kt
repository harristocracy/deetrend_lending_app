package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AmortizationType
import com.example.data.model.RepaymentFrequency
import com.example.domain.calculator.DeetrendLoanCalculator
import com.example.ui.LoanApprovalConfigState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanApprovalConfigurationScreen(
    configState: LoanApprovalConfigState,
    onApprovedAmountChange: (String) -> Unit,
    onMonthlyInterestRateChange: (String) -> Unit,
    onTenureChange: (Int) -> Unit,
    onAmortizationTypeChange: (AmortizationType) -> Unit,
    onRepaymentFrequencyChange: (RepaymentFrequency) -> Unit,
    onCollateralChoiceChange: (String) -> Unit,
    onCollateralDescriptionChange: (String) -> Unit,
    onConfirmApproval: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val app = configState.application ?: return

    var collateralDropdownExpanded by remember { mutableStateOf(false) }
    val collateralOptions = listOf(
        "No Collateral",
        "Use Collateral Provided in Application",
        "Other / Updated Collateral"
    )

    // Dynamic Live Calculation Preview
    val previewAmount = configState.approvedAmount.toDoubleOrNull() ?: 0.0
    val previewRate = configState.monthlyInterestRate.toDoubleOrNull() ?: 0.0
    val calculationPreview = if (previewAmount > 0.0 && previewRate >= 0.0) {
        DeetrendLoanCalculator.calculateLoan(
            loanId = "DGE-PREVIEW",
            principal = previewAmount,
            monthlyInterestRatePercent = previewRate,
            tenureMonths = configState.tenureMonths,
            amortizationType = configState.amortizationType,
            repaymentFrequency = configState.repaymentFrequency
        )
    } else null

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("loan_approval_config_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_approval_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Loan Approval Configuration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Determine & finalize terms for ${app.full_name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Original Application Request Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Requested by Applicant (${app.application_id}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount Requested:", style = MaterialTheme.typography.bodySmall)
                            Text(String.format(Locale.US, "₦%,.2f", app.amount_requested), fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Requested Plan & Tenure:", style = MaterialTheme.typography.bodySmall)
                            Text("${app.repayment_plan} • ${app.loan_tenure_months} Months", fontWeight = FontWeight.Medium)
                        }
                        if (app.collateral_provided && app.collateral_description.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Provided Collateral:", style = MaterialTheme.typography.bodySmall)
                                Text(app.collateral_description, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Error Display
            if (configState.errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(configState.errorMessage, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // Form Inputs Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Approved Terms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        // 1. Approved Loan Amount
                        OutlinedTextField(
                            value = configState.approvedAmount,
                            onValueChange = onApprovedAmountChange,
                            label = { Text("Approved Loan Amount (₦) *") },
                            placeholder = { Text("Enter final approved principal") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            prefix = { Text("₦ ", fontWeight = FontWeight.Bold) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_approved_amount")
                        )

                        // 2. Amortization Type (Flat Interest / Reducing Balance)
                        Column {
                            Text("Amortization Type *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = configState.amortizationType == AmortizationType.FLAT_INTEREST,
                                    onClick = { onAmortizationTypeChange(AmortizationType.FLAT_INTEREST) },
                                    label = { Text("Flat Interest") },
                                    modifier = Modifier.testTag("chip_flat_interest")
                                )
                                FilterChip(
                                    selected = configState.amortizationType == AmortizationType.REDUCING_BALANCE,
                                    onClick = { onAmortizationTypeChange(AmortizationType.REDUCING_BALANCE) },
                                    label = { Text("Reducing Balance") },
                                    modifier = Modifier.testTag("chip_reducing_balance")
                                )
                            }
                        }

                        // 3. Interest Rate Per Month (%)
                        OutlinedTextField(
                            value = configState.monthlyInterestRate,
                            onValueChange = onMonthlyInterestRateChange,
                            label = { Text("Interest Rate Per Month (%) *") },
                            placeholder = { Text("e.g. 7.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            suffix = { Text("% / mo", fontWeight = FontWeight.Bold) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_monthly_interest_rate")
                        )

                        // 4. Loan Tenure (Months)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Loan Tenure", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "${configState.tenureMonths} Months",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Slider(
                                value = configState.tenureMonths.toFloat(),
                                onValueChange = { onTenureChange(it.toInt()) },
                                valueRange = 1f..24f,
                                steps = 22,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("slider_approval_tenure")
                            )
                        }

                        // 5. Repayment Frequency (Weekly / Monthly)
                        Column {
                            Text("Repayment Frequency *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilterChip(
                                    selected = configState.repaymentFrequency == RepaymentFrequency.MONTHLY,
                                    onClick = { onRepaymentFrequencyChange(RepaymentFrequency.MONTHLY) },
                                    label = { Text("Monthly") },
                                    modifier = Modifier.testTag("chip_frequency_monthly")
                                )
                                FilterChip(
                                    selected = configState.repaymentFrequency == RepaymentFrequency.WEEKLY,
                                    onClick = { onRepaymentFrequencyChange(RepaymentFrequency.WEEKLY) },
                                    label = { Text("Weekly") },
                                    modifier = Modifier.testTag("chip_frequency_weekly")
                                )
                            }
                        }

                        // 6. Collateral Options
                        ExposedDropdownMenuBox(
                            expanded = collateralDropdownExpanded,
                            onExpandedChange = { collateralDropdownExpanded = !collateralDropdownExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = configState.collateralChoice,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Collateral Selection *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = collateralDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .testTag("dropdown_collateral_choice")
                            )
                            ExposedDropdownMenu(
                                expanded = collateralDropdownExpanded,
                                onDismissRequest = { collateralDropdownExpanded = false }
                            ) {
                                collateralOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            onCollateralChoiceChange(opt)
                                            collateralDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (configState.collateralChoice != "No Collateral") {
                            OutlinedTextField(
                                value = configState.collateralDescription,
                                onValueChange = onCollateralDescriptionChange,
                                label = { Text("Collateral Description") },
                                placeholder = { Text("Asset / vehicle / property description...") },
                                minLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_approval_collateral_desc")
                            )
                        }
                    }
                }
            }

            // Calculation Preview Card
            calculationPreview?.let { calc ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Calculated Loan Repayment Terms",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Periodic Installment:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    String.format(Locale.US, "₦%,.2f (%s)", calc.periodicPayment, if (calc.repaymentFrequency == RepaymentFrequency.WEEKLY) "Weekly" else "Monthly"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Interest:", style = MaterialTheme.typography.bodyMedium)
                                Text(String.format(Locale.US, "₦%,.2f", calc.totalInterest), fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Repayment:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    String.format(Locale.US, "₦%,.2f", calc.totalRepayment),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Confirm & Issue Button
            item {
                Button(
                    onClick = onConfirmApproval,
                    enabled = !configState.isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("confirm_loan_approval_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (configState.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configuring & Creating Loan...")
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Confirm Approval & Create Loan", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
