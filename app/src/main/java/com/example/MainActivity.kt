package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ApplicationStatus
import com.example.ui.ActiveUserMode
import com.example.ui.DeetrendScreen
import com.example.ui.DeetrendViewModel
import com.example.ui.screens.AdminAgreementsScreen
import com.example.ui.screens.AdminApplicationsScreen
import com.example.ui.screens.AdminAuditTrailScreen
import com.example.ui.screens.AdminBorrowersScreen
import com.example.ui.screens.AdminDashboardOverviewScreen
import com.example.ui.screens.AdminLoanDetailScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.AdminPortfolioScreen
import com.example.ui.screens.BorrowerAgreementConsentScreen
import com.example.ui.screens.LoanApprovalConfigurationScreen
import com.example.ui.screens.PublicApplicationScreen
import com.example.ui.theme.DeetrendOrange
import com.example.ui.theme.DeetrendTeal
import com.example.ui.theme.DeetrendTealContainer
import com.example.ui.theme.DeetrendTealDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                DeetrendLendingAppRoot()
            }
        }
    }
}

@Composable
fun DeetrendLendingAppRoot(
    viewModel: DeetrendViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val userMode by viewModel.userMode.collectAsStateWithLifecycle()

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val loginEmail by viewModel.loginEmail.collectAsStateWithLifecycle()
    val loginPassword by viewModel.loginPassword.collectAsStateWithLifecycle()
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()

    val applications by viewModel.allApplications.collectAsStateWithLifecycle()
    val approvedLoans by viewModel.allApprovedLoans.collectAsStateWithLifecycle()
    val agreements by viewModel.allAgreements.collectAsStateWithLifecycle()
    val borrowers by viewModel.allBorrowers.collectAsStateWithLifecycle()
    val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()

    val selectedLoan by viewModel.selectedLoan.collectAsStateWithLifecycle()
    val selectedInstallments by viewModel.selectedLoanInstallments.collectAsStateWithLifecycle()
    val applicationForm by viewModel.applicationForm.collectAsStateWithLifecycle()
    val approvalConfig by viewModel.approvalConfig.collectAsStateWithLifecycle()

    val borrowerAgreement by viewModel.borrowerAgreement.collectAsStateWithLifecycle()
    val borrowerConsentAgreed by viewModel.borrowerConsentAgreed.collectAsStateWithLifecycle()
    val borrowerConsentName by viewModel.borrowerConsentName.collectAsStateWithLifecycle()

    val isSyncingCloud by viewModel.isSyncingCloud.collectAsStateWithLifecycle()
    val isPullingCloud by viewModel.isPullingCloud.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    // Back button behavior
    BackHandler(enabled = currentScreen != DeetrendScreen.ADMIN_DASHBOARD && currentScreen != DeetrendScreen.PUBLIC_APPLY && currentScreen != DeetrendScreen.ADMIN_LOGIN) {
        when (currentScreen) {
            DeetrendScreen.ADMIN_APPROVAL_CONFIG -> viewModel.navigateTo(DeetrendScreen.ADMIN_APPLICATIONS)
            DeetrendScreen.ADMIN_LOAN_DETAIL -> viewModel.navigateTo(DeetrendScreen.ADMIN_PORTFOLIO)
            DeetrendScreen.ADMIN_AUDIT_TRAIL -> viewModel.navigateTo(DeetrendScreen.ADMIN_DASHBOARD)
            DeetrendScreen.BORROWER_AGREEMENT -> viewModel.navigateTo(DeetrendScreen.ADMIN_AGREEMENTS)
            else -> viewModel.navigateTo(DeetrendScreen.ADMIN_DASHBOARD)
        }
    }

    val pendingApplicationsCount = applications.count { it.application_status == ApplicationStatus.Pending }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Mode Switcher Header: Easily switch between "Admin Management Portal" and "Borrower Portal (/apply)"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                TabRow(
                    selectedTabIndex = if (userMode == ActiveUserMode.ADMIN) 0 else 1,
                    containerColor = Color.White,
                    contentColor = DeetrendTeal,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(
                                tabPositions[if (userMode == ActiveUserMode.ADMIN) 0 else 1]
                            ),
                            color = DeetrendTeal
                        )
                    }
                ) {
                    Tab(
                        selected = userMode == ActiveUserMode.ADMIN,
                        onClick = { viewModel.setUserMode(ActiveUserMode.ADMIN) },
                        text = {
                            Text(
                                text = "Admin Portal",
                                fontWeight = if (userMode == ActiveUserMode.ADMIN) FontWeight.Bold else FontWeight.Normal,
                                color = if (userMode == ActiveUserMode.ADMIN) DeetrendTeal else Color(0xFF6B7280)
                            )
                        },
                        modifier = Modifier.testTag("tab_mode_admin")
                    )
                    Tab(
                        selected = userMode == ActiveUserMode.BORROWER,
                        onClick = { viewModel.setUserMode(ActiveUserMode.BORROWER) },
                        text = {
                            Text(
                                text = "Borrower Portal (/apply)",
                                fontWeight = if (userMode == ActiveUserMode.BORROWER) FontWeight.Bold else FontWeight.Normal,
                                color = if (userMode == ActiveUserMode.BORROWER) DeetrendTeal else Color(0xFF6B7280)
                            )
                        },
                        modifier = Modifier.testTag("tab_mode_borrower")
                    )
                }
            }
        },
        bottomBar = {
            if (userMode == ActiveUserMode.ADMIN && isAuthenticated && currentScreen != DeetrendScreen.ADMIN_LOGIN) {
                NavigationBar(
                    modifier = Modifier.testTag("admin_bottom_navigation_bar"),
                    containerColor = Color.White
                ) {
                    NavigationBarItem(
                        selected = currentScreen == DeetrendScreen.ADMIN_DASHBOARD,
                        onClick = { viewModel.navigateTo(DeetrendScreen.ADMIN_DASHBOARD) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == DeetrendScreen.ADMIN_DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        label = { Text("Overview") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeetrendTeal,
                            selectedTextColor = DeetrendTeal,
                            indicatorColor = DeetrendTealContainer
                        ),
                        modifier = Modifier.testTag("nav_item_admin_dashboard")
                    )

                    NavigationBarItem(
                        selected = currentScreen == DeetrendScreen.ADMIN_APPLICATIONS || currentScreen == DeetrendScreen.ADMIN_APPROVAL_CONFIG,
                        onClick = { viewModel.navigateTo(DeetrendScreen.ADMIN_APPLICATIONS) },
                        icon = {
                            BadgedBox(badge = {
                                if (pendingApplicationsCount > 0) {
                                    Badge(containerColor = DeetrendOrange) { Text("$pendingApplicationsCount", color = Color.White) }
                                }
                            }) {
                                Icon(
                                    imageVector = if (currentScreen == DeetrendScreen.ADMIN_APPLICATIONS) Icons.Filled.Assignment else Icons.Outlined.Assignment,
                                    contentDescription = "Applications"
                                )
                            }
                        },
                        label = { Text("Applications") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeetrendTeal,
                            selectedTextColor = DeetrendTeal,
                            indicatorColor = DeetrendTealContainer
                        ),
                        modifier = Modifier.testTag("nav_item_admin_applications")
                    )

                    NavigationBarItem(
                        selected = currentScreen == DeetrendScreen.ADMIN_PORTFOLIO || currentScreen == DeetrendScreen.ADMIN_LOAN_DETAIL,
                        onClick = { viewModel.navigateTo(DeetrendScreen.ADMIN_PORTFOLIO) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == DeetrendScreen.ADMIN_PORTFOLIO) Icons.Filled.Folder else Icons.Outlined.Folder,
                                contentDescription = "Portfolio"
                            )
                        },
                        label = { Text("Portfolio") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeetrendTeal,
                            selectedTextColor = DeetrendTeal,
                            indicatorColor = DeetrendTealContainer
                        ),
                        modifier = Modifier.testTag("nav_item_admin_portfolio")
                    )

                    NavigationBarItem(
                        selected = currentScreen == DeetrendScreen.ADMIN_AGREEMENTS,
                        onClick = { viewModel.navigateTo(DeetrendScreen.ADMIN_AGREEMENTS) },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen == DeetrendScreen.ADMIN_AGREEMENTS) Icons.Filled.Description else Icons.Outlined.Description,
                                contentDescription = "Agreements"
                            )
                        },
                        label = { Text("Agreements") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeetrendTeal,
                            selectedTextColor = DeetrendTeal,
                            indicatorColor = DeetrendTealContainer
                        ),
                        modifier = Modifier.testTag("nav_item_admin_agreements")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (userMode == ActiveUserMode.ADMIN && !isAuthenticated) {
                AdminLoginScreen(
                    emailValue = loginEmail,
                    passwordValue = loginPassword,
                    isLoading = isAuthenticating,
                    errorMessage = loginError,
                    onEmailChange = { viewModel.setLoginEmail(it) },
                    onPasswordChange = { viewModel.setLoginPassword(it) },
                    onLoginClick = { viewModel.loginAdmin() },
                    onQuickDemoClick = { viewModel.quickDemoAdminLogin() },
                    onNavigateToPublicApply = { viewModel.setUserMode(ActiveUserMode.BORROWER) }
                )
            } else {
                when (currentScreen) {
                    DeetrendScreen.ADMIN_LOGIN -> {
                        AdminLoginScreen(
                            emailValue = loginEmail,
                            passwordValue = loginPassword,
                            isLoading = isAuthenticating,
                            errorMessage = loginError,
                            onEmailChange = { viewModel.setLoginEmail(it) },
                            onPasswordChange = { viewModel.setLoginPassword(it) },
                            onLoginClick = { viewModel.loginAdmin() },
                            onQuickDemoClick = { viewModel.quickDemoAdminLogin() },
                            onNavigateToPublicApply = { viewModel.setUserMode(ActiveUserMode.BORROWER) }
                        )
                    }

                    DeetrendScreen.ADMIN_DASHBOARD -> {
                        AdminDashboardOverviewScreen(
                            currentUser = currentUser,
                            applications = applications,
                            loans = approvedLoans,
                            agreements = agreements,
                            borrowers = borrowers,
                            isSyncingCloud = isSyncingCloud,
                            onSyncCloud = { viewModel.syncAllDataToCloud() },
                            isPullingCloud = isPullingCloud,
                            onPullCloud = { viewModel.pullDataFromCloud() },
                            onNavigate = { viewModel.navigateTo(it) },
                            onLogout = { viewModel.logout() }
                        )
                    }

                    DeetrendScreen.ADMIN_APPLICATIONS -> {
                        AdminApplicationsScreen(
                            applications = applications,
                            onApproveClick = { app ->
                                viewModel.openApprovalConfiguration(app)
                            },
                            onRejectClick = { appId, reason ->
                                viewModel.rejectApplication(appId, reason)
                            },
                            onBack = { viewModel.navigateTo(DeetrendScreen.ADMIN_DASHBOARD) }
                        )
                    }

                    DeetrendScreen.ADMIN_APPROVAL_CONFIG -> {
                        LoanApprovalConfigurationScreen(
                            configState = approvalConfig,
                            onApprovedAmountChange = { viewModel.updateApprovedAmount(it) },
                            onMonthlyInterestRateChange = { viewModel.updateMonthlyInterestRate(it) },
                            onTenureChange = { viewModel.updateApprovalTenure(it) },
                            onAmortizationTypeChange = { viewModel.updateAmortizationType(it) },
                            onRepaymentFrequencyChange = { viewModel.updateRepaymentFrequency(it) },
                            onCollateralChoiceChange = { viewModel.updateCollateralChoice(it) },
                            onCollateralDescriptionChange = { viewModel.updateApprovalCollateralDesc(it) },
                            onConfirmApproval = { viewModel.confirmLoanApproval() },
                            onBack = { viewModel.navigateTo(DeetrendScreen.ADMIN_APPLICATIONS) }
                        )
                    }

                    DeetrendScreen.ADMIN_PORTFOLIO -> {
                        AdminPortfolioScreen(
                            loans = approvedLoans,
                            onSelectLoan = { loan ->
                                viewModel.selectLoan(loan)
                            },
                            onBack = { viewModel.navigateTo(DeetrendScreen.ADMIN_DASHBOARD) }
                        )
                    }

                    DeetrendScreen.ADMIN_LOAN_DETAIL -> {
                        selectedLoan?.let { loan ->
                            val linkedBorrower = borrowers.find { it.borrower_id == loan.borrower_id }
                            AdminLoanDetailScreen(
                                loan = loan,
                                borrower = linkedBorrower,
                                installments = selectedInstallments,
                                onGenerateAgreement = { viewModel.generateAgreementForSelectedLoan() },
                                onRecordRepayment = { amt, method, ref, notes ->
                                    viewModel.recordRepayment(loan.loan_id, loan.borrower_id, amt, method, ref, notes)
                                },
                                onBack = { viewModel.navigateTo(DeetrendScreen.ADMIN_PORTFOLIO) }
                            )
                        } ?: run {
                            viewModel.navigateTo(DeetrendScreen.ADMIN_PORTFOLIO)
                        }
                    }

                    DeetrendScreen.ADMIN_AGREEMENTS -> {
                        AdminAgreementsScreen(
                            agreements = agreements,
                            onOpenBorrowerLink = { token ->
                                viewModel.openBorrowerAgreementLink(token)
                            },
                            onBack = { viewModel.navigateTo(DeetrendScreen.ADMIN_DASHBOARD) }
                        )
                    }

                    DeetrendScreen.ADMIN_BORROWERS -> {
                        AdminBorrowersScreen(
                            borrowers = borrowers,
                            onBack = { viewModel.navigateTo(DeetrendScreen.ADMIN_DASHBOARD) }
                        )
                    }

                    DeetrendScreen.ADMIN_AUDIT_TRAIL -> {
                        AdminAuditTrailScreen(
                            auditLogs = auditLogs,
                            documents = documents,
                            onBack = { viewModel.navigateTo(DeetrendScreen.ADMIN_DASHBOARD) }
                        )
                    }

                    DeetrendScreen.PUBLIC_APPLY -> {
                        PublicApplicationScreen(
                            formState = applicationForm,
                            onFullNameChange = { viewModel.updateFullName(it) },
                            onPhoneNumberChange = { viewModel.updatePhoneNumber(it) },
                            onOccupationTypeChange = { viewModel.updateOccupationType(it) },
                            onHomeAddressChange = { viewModel.updateHomeAddress(it) },
                            onWorkLocationChange = { viewModel.updateWorkLocation(it) },
                            onAmountRequestedChange = { viewModel.updateAmountRequested(it) },
                            onLoanPurposeChange = { viewModel.updateLoanPurpose(it) },
                            onRepaymentPlanChange = { viewModel.updateRepaymentPlan(it) },
                            onCollateralProvidedChange = { viewModel.updateCollateralProvided(it) },
                            onCollateralDescriptionChange = { viewModel.updateCollateralDescription(it) },
                            onLoanTenureChange = { viewModel.updateLoanTenure(it) },
                            onAccountNameChange = { viewModel.updateAccountName(it) },
                            onBankNameChange = { viewModel.updateBankName(it) },
                            onAccountNumberChange = { viewModel.updateAccountNumber(it) },
                            onSubmitApplication = { viewModel.submitPublicApplication() },
                            onResetForm = { viewModel.resetApplicationForm() }
                        )
                    }

                    DeetrendScreen.BORROWER_AGREEMENT -> {
                        BorrowerAgreementConsentScreen(
                            agreement = borrowerAgreement,
                            consentAgreed = borrowerConsentAgreed,
                            signatureName = borrowerConsentName,
                            onConsentAgreedChange = { viewModel.setBorrowerConsentAgreed(it) },
                            onSignatureNameChange = { viewModel.setBorrowerConsentName(it) },
                            onSubmitConsent = { viewModel.submitBorrowerAgreementConsent() },
                            onBackToAdmin = { viewModel.navigateTo(DeetrendScreen.ADMIN_DASHBOARD) }
                        )
                    }
                }
            }
        }
    }
}

// Retained for screenshot & preview compatibility
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Deetrend Lending: $name", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Deetrend Global Enterprise")
    }
}
