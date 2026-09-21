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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.ui.graphics.Color
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
import com.example.ui.PublicApplicationFormState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicApplicationScreen(
    formState: PublicApplicationFormState,
    onFullNameChange: (String) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onOccupationTypeChange: (String) -> Unit,
    onHomeAddressChange: (String) -> Unit,
    onWorkLocationChange: (String) -> Unit,
    onAmountRequestedChange: (String) -> Unit,
    onLoanPurposeChange: (String) -> Unit,
    onRepaymentPlanChange: (String) -> Unit,
    onCollateralProvidedChange: (Boolean) -> Unit,
    onCollateralDescriptionChange: (String) -> Unit,
    onLoanTenureChange: (Int) -> Unit,
    onAccountNameChange: (String) -> Unit,
    onBankNameChange: (String) -> Unit,
    onAccountNumberChange: (String) -> Unit,
    onSubmitApplication: () -> Unit,
    onResetForm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var occupationExpanded by remember { mutableStateOf(false) }
    val occupationOptions = listOf("Salary Earner", "Business Person")

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("public_apply_scrollable_column"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        com.example.ui.components.DeetrendBrandLogo(
                            emblemSize = 42.dp,
                            showSubtitle = true,
                            showWebsite = true
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Public Loan Application Portal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = com.example.ui.theme.DeetrendTeal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Complete your loan request details below. Approved terms and repayment schedules will be evaluated by Deetrend Global Enterprise administrators.",
                            style = MaterialTheme.typography.bodySmall,
                            color = com.example.ui.theme.DeetrendTextSecondary
                        )
                    }
                }
            }

            // Success Confirmation Screen
            if (formState.submittedApplicationId != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("application_success_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = "Application Submitted Successfully",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = formState.submittedApplicationId,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            Text(
                                text = "Your loan application has been submitted successfully. Your application reference is ${formState.submittedApplicationId}. Our loan officer will review your application shortly.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onResetForm,
                                modifier = Modifier.testTag("submit_another_application_btn")
                            ) {
                                Text("Submit Another Application")
                            }
                        }
                    }
                }
            } else {
                // Form Fields

                // Error alert if any
                if (formState.errorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = formState.errorMessage,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Section 1: Personal Information
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = formState.fullName,
                                onValueChange = onFullNameChange,
                                label = { Text("Full Name *") },
                                placeholder = { Text("e.g. Babatunde Adeleke") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_full_name")
                            )

                            OutlinedTextField(
                                value = formState.phoneNumber,
                                onValueChange = onPhoneNumberChange,
                                label = { Text("Phone Number *") },
                                placeholder = { Text("e.g. 08012345678") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_phone_number")
                            )

                            // Occupation Dropdown
                            ExposedDropdownMenuBox(
                                expanded = occupationExpanded,
                                onExpandedChange = { occupationExpanded = !occupationExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = formState.occupationType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Occupation *") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = occupationExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .testTag("dropdown_occupation")
                                )
                                ExposedDropdownMenu(
                                    expanded = occupationExpanded,
                                    onDismissRequest = { occupationExpanded = false }
                                ) {
                                    occupationOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = {
                                                onOccupationTypeChange(opt)
                                                occupationExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = formState.homeAddress,
                                onValueChange = onHomeAddressChange,
                                label = { Text("Personal Home Address *") },
                                placeholder = { Text("e.g. 14 Marina Street, Lagos Island, Lagos") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_home_address")
                            )

                            OutlinedTextField(
                                value = formState.workOrBusinessLocation,
                                onValueChange = onWorkLocationChange,
                                label = { Text("Business / Work Location") },
                                placeholder = { Text("e.g. Shop 42, Balogun Market, Lagos") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_work_location")
                            )
                        }
                    }
                }

                // Section 2: Loan Information
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loan Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = formState.amountRequested,
                                onValueChange = onAmountRequestedChange,
                                label = { Text("How Much Are You Applying For? (₦) *") },
                                placeholder = { Text("e.g. 250000") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                prefix = { Text("₦ ", fontWeight = FontWeight.Bold) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_amount_requested")
                            )

                            OutlinedTextField(
                                value = formState.loanPurpose,
                                onValueChange = onLoanPurposeChange,
                                label = { Text("Purpose of the Loan") },
                                placeholder = { Text("e.g. Business Inventory Expansion, Equipment purchase") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_loan_purpose")
                            )

                            // Repayment Plan (Weekly or Monthly)
                            Column {
                                Text("Repayment Plan *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    listOf("Monthly", "Weekly").forEach { plan ->
                                        FilterChip(
                                            selected = formState.repaymentPlan == plan,
                                            onClick = { onRepaymentPlanChange(plan) },
                                            label = { Text(plan) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            ),
                                            modifier = Modifier.testTag("chip_plan_$plan")
                                        )
                                    }
                                }
                            }

                            // Collateral Provided (Yes/No)
                            Column {
                                Text("Collateral Provided *", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilterChip(
                                        selected = !formState.collateralProvided,
                                        onClick = { onCollateralProvidedChange(false) },
                                        label = { Text("No") },
                                        modifier = Modifier.testTag("chip_collateral_no")
                                    )
                                    FilterChip(
                                        selected = formState.collateralProvided,
                                        onClick = { onCollateralProvidedChange(true) },
                                        label = { Text("Yes") },
                                        modifier = Modifier.testTag("chip_collateral_yes")
                                    )
                                }
                            }

                            if (formState.collateralProvided) {
                                OutlinedTextField(
                                    value = formState.collateralDescription,
                                    onValueChange = onCollateralDescriptionChange,
                                    label = { Text("Collateral Description *") },
                                    placeholder = { Text("Describe the asset, vehicle, or item provided as collateral...") },
                                    minLines = 3,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_collateral_desc")
                                )
                            }

                            // Loan Tenure (Months)
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Loan Tenure (How long you intend to repay the loan)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            "${formState.loanTenureMonths} Months",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Slider(
                                    value = formState.loanTenureMonths.toFloat(),
                                    onValueChange = { onLoanTenureChange(it.toInt()) },
                                    valueRange = 1f..24f,
                                    steps = 22,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("slider_loan_tenure")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("1 month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("12 months", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("24 months", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // Section 3: Bank Account Details
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bank Account Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Account to which approved funds may be disbursed:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = formState.accountName,
                                onValueChange = onAccountNameChange,
                                label = { Text("Account Name") },
                                placeholder = { Text("e.g. Babatunde Adeleke") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_account_name")
                            )

                            OutlinedTextField(
                                value = formState.bankName,
                                onValueChange = onBankNameChange,
                                label = { Text("Bank Name") },
                                placeholder = { Text("e.g. Access Bank / GTBank / First Bank") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_bank_name")
                            )

                            OutlinedTextField(
                                value = formState.accountNumber,
                                onValueChange = onAccountNumberChange,
                                label = { Text("Account Number") },
                                placeholder = { Text("e.g. 0123456789 (10 digits)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_account_number")
                            )
                        }
                    }
                }

                // Section 4: Submit Application Button
                item {
                    Button(
                        onClick = onSubmitApplication,
                        enabled = !formState.isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("submit_application_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (formState.isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Submitting Application...", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Submit Application", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
