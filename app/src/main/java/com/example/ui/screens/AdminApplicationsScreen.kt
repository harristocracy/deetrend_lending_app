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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.example.data.model.ApplicationStatus
import com.example.data.model.LoanApplicationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminApplicationsScreen(
    applications: List<LoanApplicationEntity>,
    onApproveClick: (LoanApplicationEntity) -> Unit,
    onRejectClick: (applicationId: String, reason: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf<ApplicationStatus?>(null) }
    var viewingApplication by remember { mutableStateOf<LoanApplicationEntity?>(null) }
    var rejectingApplication by remember { mutableStateOf<LoanApplicationEntity?>(null) }
    var rejectionReasonInput by remember { mutableStateOf("") }

    val filteredApps = applications.filter { app ->
        val matchesSearch = searchQuery.isBlank() ||
                app.application_id.contains(searchQuery, ignoreCase = true) ||
                app.full_name.contains(searchQuery, ignoreCase = true) ||
                app.phone_number.contains(searchQuery, ignoreCase = true)
        val matchesStatus = selectedStatusFilter == null || app.application_status == selectedStatusFilter
        matchesSearch && matchesStatus
    }

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_applications_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_applications_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Loan Applications",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Review submitted borrower financing requests",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by Application ID, Name, Phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_applications_field")
                )
            }

            // Status Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedStatusFilter == null,
                        onClick = { selectedStatusFilter = null },
                        label = { Text("All (${applications.size})") }
                    )
                    ApplicationStatus.entries.forEach { status ->
                        val count = applications.count { it.application_status == status }
                        FilterChip(
                            selected = selectedStatusFilter == status,
                            onClick = { selectedStatusFilter = if (selectedStatusFilter == status) null else status },
                            label = { Text("$status ($count)") }
                        )
                    }
                }
            }

            // Empty state
            if (filteredApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No loan applications found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Application Items
            items(filteredApps, key = { it.id }) { app ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewingApplication = app }
                        .testTag("app_card_${app.application_id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = app.application_id,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            ApplicationStatusBadge(app.application_status)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = app.full_name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${app.phone_number} • ${app.occupation_type}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Amount Requested", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = String.format(Locale.US, "₦%,.2f", app.amount_requested),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Plan & Tenure", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "${app.repayment_plan} • ${app.loan_tenure_months} mos",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons: View, Approve, Reject
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewingApplication = app },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_view_app_${app.application_id}")
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View")
                            }

                            if (app.application_status == ApplicationStatus.Pending) {
                                Button(
                                    onClick = { onApproveClick(app) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_approve_app_${app.application_id}")
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve")
                                }

                                OutlinedButton(
                                    onClick = { rejectingApplication = app },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_reject_app_${app.application_id}")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject")
                                }
                            }
                        }
                    }
                }
            }
        }

        // View Complete Application Dialog
        viewingApplication?.let { app ->
            AlertDialog(
                onDismissRequest = { viewingApplication = null },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Application Details", style = MaterialTheme.typography.titleLarge)
                        ApplicationStatusBadge(app.application_status)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailRow("Application ID", app.application_id)
                        DetailRow("Applicant Name", app.full_name)
                        DetailRow("Phone Number", app.phone_number)
                        DetailRow("Occupation", app.occupation_type)
                        DetailRow("Home Address", app.home_address)
                        if (app.work_or_business_location.isNotBlank()) {
                            DetailRow("Work/Business Loc", app.work_or_business_location)
                        }
                        DetailRow("Amount Requested", String.format(Locale.US, "₦%,.2f", app.amount_requested))
                        DetailRow("Loan Purpose", if (app.loan_purpose.isNotBlank()) app.loan_purpose else "Not specified")
                        DetailRow("Repayment Plan", app.repayment_plan)
                        DetailRow("Loan Tenure", "${app.loan_tenure_months} Months")
                        DetailRow("Collateral Provided", if (app.collateral_provided) "Yes (${app.collateral_description})" else "No")
                        DetailRow("Bank Details", "${app.account_name} • ${app.bank_name} • ${app.account_number}")
                        DetailRow("Submitted Date", dateFormatter.format(Date(app.submitted_at)))

                        if (app.application_status == ApplicationStatus.Rejected && app.rejection_reason.isNotBlank()) {
                            DetailRow("Rejection Reason", app.rejection_reason)
                        }
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (app.application_status == ApplicationStatus.Pending) {
                            Button(onClick = {
                                val target = viewingApplication
                                viewingApplication = null
                                target?.let { onApproveClick(it) }
                            }) {
                                Text("Approve Loan")
                            }
                        }
                        TextButton(onClick = { viewingApplication = null }) {
                            Text("Close")
                        }
                    }
                }
            )
        }

        // Reject Dialog
        rejectingApplication?.let { app ->
            AlertDialog(
                onDismissRequest = { rejectingApplication = null },
                title = { Text("Reject Application (${app.application_id})") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Please specify the reason for rejecting ${app.full_name}'s application:")
                        OutlinedTextField(
                            value = rejectionReasonInput,
                            onValueChange = { rejectionReasonInput = it },
                            placeholder = { Text("e.g. Insufficient income verification, invalid collateral") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_rejection_reason")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = app.application_id
                            val reason = rejectionReasonInput
                            rejectingApplication = null
                            rejectionReasonInput = ""
                            onRejectClick(id, reason)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_reject_btn")
                    ) {
                        Text("Confirm Rejection")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rejectingApplication = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ApplicationStatusBadge(status: ApplicationStatus) {
    val (bgColor, txtColor) = when (status) {
        ApplicationStatus.Pending -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        ApplicationStatus.Approved -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        ApplicationStatus.Rejected -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
    }
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = status.name,
            color = txtColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f))
    }
}
