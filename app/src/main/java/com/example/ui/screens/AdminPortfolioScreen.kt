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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.data.model.ApprovedLoan
import com.example.data.model.LoanRecordStatus
import com.example.data.model.PaymentStatus
import com.example.domain.calculator.DeetrendLoanCalculator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdminPortfolioScreen(
    loans: List<ApprovedLoan>,
    onSelectLoan: (ApprovedLoan) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var loanStatusFilter by remember { mutableStateOf<String?>("All") } // "All", "Active", "Overdue", "Settled"
    var paymentStatusFilter by remember { mutableStateOf<String?>("All") } // "All", "Active", "Paid"
    var sortBy by remember { mutableStateOf("Date Issued (Newest)") } // "Date Issued (Newest)", "Outstanding Balance"

    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    // Dynamic Filter & Sort Engine
    val processedLoans = loans.filter { loan ->
        val computedDays = DeetrendLoanCalculator.getDaysDifference(loan.due_date)
        val dynamicLoanStatus = when {
            loan.outstanding_balance <= 0.0 -> "Settled"
            computedDays < 0 -> "Overdue"
            else -> "Active"
        }
        val matchesLoanStatus = loanStatusFilter == "All" || dynamicLoanStatus.equals(loanStatusFilter, ignoreCase = true)
        val matchesPaymentStatus = paymentStatusFilter == "All" || loan.payment_status.name.equals(paymentStatusFilter, ignoreCase = true)
        val matchesSearch = searchQuery.isBlank() ||
                loan.loan_id.contains(searchQuery, ignoreCase = true) ||
                loan.borrower_name.contains(searchQuery, ignoreCase = true) ||
                loan.phone_number.contains(searchQuery, ignoreCase = true)

        matchesLoanStatus && matchesPaymentStatus && matchesSearch
    }.let { list ->
        if (sortBy == "Outstanding Balance") {
            list.sortedByDescending { it.outstanding_balance }
        } else {
            list.sortedByDescending { it.date_issued }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("admin_portfolio_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_portfolio_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Loan Portfolio",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${loans.size} total approved lending facilities",
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
                    placeholder = { Text("Search by Loan ID, Borrower, Phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_portfolio_input")
                )
            }

            // Filter Chips: Loan Status (Active, Overdue, Settled)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Filter by Loan Status:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("All", "Active", "Overdue", "Settled").forEach { st ->
                            FilterChip(
                                selected = loanStatusFilter == st,
                                onClick = { loanStatusFilter = st },
                                label = { Text(st) },
                                modifier = Modifier.testTag("chip_status_$st")
                            )
                        }
                    }
                }
            }

            // Sort Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sortBy == "Date Issued (Newest)",
                        onClick = { sortBy = "Date Issued (Newest)" },
                        label = { Text("Sort: Date Issued") },
                        modifier = Modifier.testTag("sort_date_issued")
                    )
                    FilterChip(
                        selected = sortBy == "Outstanding Balance",
                        onClick = { sortBy = "Outstanding Balance" },
                        label = { Text("Sort: Balance (High-Low)") },
                        modifier = Modifier.testTag("sort_balance")
                    )
                }
            }

            // Empty State
            if (processedLoans.isEmpty()) {
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
                                text = "No loans match current filters.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Portfolio Records
            items(processedLoans, key = { it.id }) { loan ->
                val daysRemainingStr = DeetrendLoanCalculator.formatDaysRemaining(loan.due_date)
                val daysDiff = DeetrendLoanCalculator.getDaysDifference(loan.due_date)
                val dynamicStatus = when {
                    loan.outstanding_balance <= 0.0 -> LoanRecordStatus.Settled
                    daysDiff < 0 -> LoanRecordStatus.Overdue
                    else -> LoanRecordStatus.Active
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLoan(loan) }
                        .testTag("loan_card_${loan.loan_id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
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
                                    text = loan.loan_id,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            LoanStatusBadge(dynamicStatus)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = loan.borrower_name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${loan.phone_number} • ${loan.loan_tenure_months} mos • ${loan.interest_rate_monthly}%/mo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Principal Financed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    String.format(Locale.US, "₦%,.2f", loan.principal_amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Outstanding Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    String.format(Locale.US, "₦%,.2f", loan.outstanding_balance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (loan.outstanding_balance > 0.0) MaterialTheme.colorScheme.primary else Color(0xFF16A34A)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Calendar & Days Remaining Info
                        Surface(
                            color = if (daysDiff < 0 && loan.outstanding_balance > 0.0) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (daysDiff < 0 && loan.outstanding_balance > 0.0) Icons.Default.Warning else Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (daysDiff < 0 && loan.outstanding_balance > 0.0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Due: ${dateFormatter.format(Date(loan.due_date))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = if (loan.outstanding_balance <= 0.0) "Settled" else daysRemainingStr,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (loan.outstanding_balance <= 0.0) Color(0xFF16A34A) else if (daysDiff < 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoanStatusBadge(status: LoanRecordStatus) {
    val (bgColor, txtColor) = when (status) {
        LoanRecordStatus.Active -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        LoanRecordStatus.Overdue -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        LoanRecordStatus.Settled -> Color(0xFFE0F2FE) to Color(0xFF0369A1)
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
