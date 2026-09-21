package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApplicationStatus
import com.example.data.model.ApprovedLoan
import com.example.data.model.Borrower
import com.example.data.model.ConsentStatus
import com.example.data.model.LoanAgreement
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.UserAccount
import com.example.domain.calculator.DeetrendLoanCalculator
import com.example.ui.DeetrendScreen
import com.example.ui.components.DeetrendBrandLogo
import com.example.ui.theme.DeetrendBackground
import com.example.ui.theme.DeetrendOrange
import com.example.ui.theme.DeetrendTeal
import com.example.ui.theme.DeetrendTealContainer
import com.example.ui.theme.DeetrendTealDark
import com.example.ui.theme.DeetrendTextPrimary
import com.example.ui.theme.DeetrendTextSecondary
import java.util.Locale

@Composable
fun AdminDashboardOverviewScreen(
    currentUser: UserAccount?,
    applications: List<LoanApplicationEntity>,
    loans: List<ApprovedLoan>,
    agreements: List<LoanAgreement>,
    borrowers: List<Borrower>,
    isSyncingCloud: Boolean = false,
    onSyncCloud: () -> Unit = {},
    isPullingCloud: Boolean = false,
    onPullCloud: () -> Unit = {},
    onNavigate: (DeetrendScreen) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dynamic database aggregations (strictly calculated, no static mock data)
    val activeLoans = loans.filter {
        val days = DeetrendLoanCalculator.getDaysDifference(it.due_date)
        it.outstanding_balance > 0.0 && days >= 0
    }
    val overdueLoans = loans.filter {
        val days = DeetrendLoanCalculator.getDaysDifference(it.due_date)
        it.outstanding_balance > 0.0 && days < 0
    }
    val settledLoans = loans.filter { it.outstanding_balance <= 0.0 }
    val pendingApplications = applications.filter { it.application_status == ApplicationStatus.Pending }
    val totalPrincipalOutstanding = loans.sumOf { it.outstanding_balance }
    val totalDisbursed = loans.sumOf { it.principal_amount }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeetrendBackground)
            .testTag("admin_dashboard_overview_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Official Deetrend Brand Header & Admin Identity
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DeetrendBrandLogo(
                            emblemSize = 44.dp,
                            showSubtitle = true,
                            showWebsite = true
                        )

                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("btn_dashboard_logout")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                tint = DeetrendTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Staff / Administrator Status Strip
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F7F6),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(DeetrendOrange, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Signed in as: ${currentUser?.name ?: "Harrison Daniel"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeetrendTextPrimary
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DeetrendTeal
                            ) {
                                Text(
                                    text = currentUser?.role?.name ?: "ADMINISTRATOR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Live Firebase Cloud Sync Action
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Firebase Firestore Connected",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Text(
                                        text = "Project: deetrend-global-enterprise",
                                        fontSize = 10.sp,
                                        color = Color(0xFF388E3C)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onPullCloud,
                                    enabled = !isPullingCloud && !isSyncingCloud,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF1B5E20)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_pull_cloud_now")
                                ) {
                                    if (isPullingCloud) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF2E7D32),
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restoring...", fontSize = 10.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Restore", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = onSyncCloud,
                                    enabled = !isSyncingCloud && !isPullingCloud,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E7D32),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_sync_cloud_now")
                                ) {
                                    if (isSyncingCloud) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pushing...", fontSize = 10.sp)
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Push", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Key Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portfolio Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeetrendTextPrimary
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DeetrendTealContainer,
                    modifier = Modifier.clickable { onNavigate(DeetrendScreen.ADMIN_AUDIT_TRAIL) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = DeetrendTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Audit & Vault",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeetrendTealDark
                        )
                    }
                }
            }
        }

        // Metric Card 1: Total Principal Outstanding (Brand Highlight in Teal)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeetrendTeal),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL PRINCIPAL OUTSTANDING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DeetrendOrange
                        ) {
                            Text(
                                text = "V1 Foundation",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format(Locale.US, "₦%,.2f", totalPrincipalOutstanding),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = String.format(Locale.US, "Total Capital Financed: ₦%,.2f across %d approved loans", totalDisbursed, loans.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // 4 KPI Grid Cards: Active, Overdue, Settled, Pending Applications
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    title = "Active Loans",
                    count = activeLoans.size.toString(),
                    subtitle = "Performing",
                    icon = Icons.Default.TrendingUp,
                    containerColor = Color(0xFFE6F4EA),
                    contentColor = Color(0xFF137333),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(DeetrendScreen.ADMIN_PORTFOLIO) }
                        .testTag("kpi_active_loans")
                )
                MetricSummaryCard(
                    title = "Overdue Loans",
                    count = overdueLoans.size.toString(),
                    subtitle = if (overdueLoans.isEmpty()) "Healthy" else "Needs Follow-up",
                    icon = Icons.Default.Warning,
                    containerColor = if (overdueLoans.isEmpty()) Color(0xFFF1F7F6) else Color(0xFFFEE2E2),
                    contentColor = if (overdueLoans.isEmpty()) DeetrendTealDark else Color(0xFFB91C1C),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(DeetrendScreen.ADMIN_PORTFOLIO) }
                        .testTag("kpi_overdue_loans")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSummaryCard(
                    title = "Settled Loans",
                    count = settledLoans.size.toString(),
                    subtitle = "Fully Repaid",
                    icon = Icons.Default.CheckCircle,
                    containerColor = Color(0xFFE8F0FE),
                    contentColor = Color(0xFF1A73E8),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(DeetrendScreen.ADMIN_PORTFOLIO) }
                        .testTag("kpi_settled_loans")
                )
                MetricSummaryCard(
                    title = "Pending Review",
                    count = pendingApplications.size.toString(),
                    subtitle = if (pendingApplications.isEmpty()) "All Reviewed" else "Action Required",
                    icon = Icons.Default.PendingActions,
                    containerColor = if (pendingApplications.isEmpty()) Color(0xFFF1F7F6) else Color(0xFFFFF3CD),
                    contentColor = if (pendingApplications.isEmpty()) DeetrendTealDark else Color(0xFF856404),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(DeetrendScreen.ADMIN_APPLICATIONS) }
                        .testTag("kpi_pending_applications")
                )
            }
        }

        // Section: Core Workflow Navigation
        item {
            Text(
                text = "Lending Operations & Records",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DeetrendTextPrimary
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminNavShortcutCard(
                    title = "Loan Applications",
                    description = "${applications.size} total applications (${pendingApplications.size} awaiting review)",
                    icon = Icons.Default.Assignment,
                    badgeCount = pendingApplications.size,
                    onClick = { onNavigate(DeetrendScreen.ADMIN_APPLICATIONS) },
                    testTag = "nav_shortcut_applications"
                )
                AdminNavShortcutCard(
                    title = "Loan Portfolio",
                    description = "${loans.size} loans issued (${activeLoans.size} active, ${overdueLoans.size} overdue)",
                    icon = Icons.Default.Folder,
                    badgeCount = null,
                    onClick = { onNavigate(DeetrendScreen.ADMIN_PORTFOLIO) },
                    testTag = "nav_shortcut_portfolio"
                )
                AdminNavShortcutCard(
                    title = "Loan Agreements",
                    description = "${agreements.size} agreements generated (${agreements.count { it.consent_status == ConsentStatus.Consented }} signed & sealed)",
                    icon = Icons.Default.Description,
                    badgeCount = null,
                    onClick = { onNavigate(DeetrendScreen.ADMIN_AGREEMENTS) },
                    testTag = "nav_shortcut_agreements"
                )
                AdminNavShortcutCard(
                    title = "Borrowers Directory",
                    description = "${borrowers.size} verified borrower profiles and disbursement accounts",
                    icon = Icons.Default.People,
                    badgeCount = null,
                    onClick = { onNavigate(DeetrendScreen.ADMIN_BORROWERS) },
                    testTag = "nav_shortcut_borrowers"
                )
                AdminNavShortcutCard(
                    title = "Audit Trail & Document Vault",
                    description = "Tamper-evident logs of approvals, consents, disbursements, and repayments",
                    icon = Icons.Default.Security,
                    badgeCount = null,
                    onClick = { onNavigate(DeetrendScreen.ADMIN_AUDIT_TRAIL) },
                    testTag = "nav_shortcut_audit_trail"
                )
            }
        }
    }
}

@Composable
fun MetricSummaryCard(
    title: String,
    count: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
                Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = count, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = contentColor)
            Text(text = subtitle, fontSize = 11.sp, color = contentColor.copy(alpha = 0.85f))
        }
    }
}

@Composable
fun AdminNavShortcutCard(
    title: String,
    description: String,
    icon: ImageVector,
    badgeCount: Int?,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = DeetrendTealContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = DeetrendTeal)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DeetrendTextPrimary)
                    if (badgeCount != null && badgeCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = DeetrendOrange
                        ) {
                            Text(
                                text = "$badgeCount",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = DeetrendTextSecondary)
            }
        }
    }
}
