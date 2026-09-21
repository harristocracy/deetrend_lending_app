package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LoanDatabase
import com.example.data.model.AmortizationType
import com.example.data.model.ApprovedLoan
import com.example.data.model.Borrower
import com.example.data.model.ConsentStatus
import com.example.data.model.DeetrendInstallment
import com.example.data.model.LoanAgreement
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.PaymentRecord
import com.example.data.model.RepaymentFrequency
import com.example.data.repository.DeetrendRepository
import com.example.domain.calculator.DeetrendLoanCalculator
import com.example.domain.calculator.LoanCalculationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Top-level application destinations
 */
enum class DeetrendScreen {
    // Admin Authentication & Views
    ADMIN_LOGIN,
    ADMIN_DASHBOARD,
    ADMIN_PORTFOLIO,
    ADMIN_APPLICATIONS,
    ADMIN_AGREEMENTS,
    ADMIN_BORROWERS,
    ADMIN_LOAN_DETAIL,
    ADMIN_APPROVAL_CONFIG,
    ADMIN_AUDIT_TRAIL,
    // Public Borrower Views
    PUBLIC_APPLY,
    BORROWER_AGREEMENT
}

enum class ActiveUserMode {
    ADMIN,
    BORROWER
}

/**
 * State for Public Loan Application Form
 */
data class PublicApplicationFormState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val occupationType: String = "Salary Earner", // "Salary Earner" or "Business Person"
    val homeAddress: String = "",
    val workOrBusinessLocation: String = "",
    val amountRequested: String = "",
    val loanPurpose: String = "",
    val repaymentPlan: String = "Monthly", // "Weekly" or "Monthly"
    val collateralProvided: Boolean = false,
    val collateralDescription: String = "",
    val loanTenureMonths: Int = 6,
    val accountName: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val isSubmitting: Boolean = false,
    val submittedApplicationId: String? = null,
    val errorMessage: String? = null
)

/**
 * State for Loan Approval & Configuration
 */
data class LoanApprovalConfigState(
    val application: LoanApplicationEntity? = null,
    val approvedAmount: String = "",
    val monthlyInterestRate: String = "7.0",
    val tenureMonths: Int = 6,
    val amortizationType: AmortizationType = AmortizationType.FLAT_INTEREST,
    val repaymentFrequency: RepaymentFrequency = RepaymentFrequency.MONTHLY,
    val collateralChoice: String = "No Collateral", // "No Collateral", "Use Collateral Provided in Application", "Other / Updated Collateral"
    val collateralDescription: String = "",
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class DeetrendViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DeetrendRepository

    init {
        val db = LoanDatabase.getDatabase(application)
        repository = DeetrendRepository(db.deetrendDao())
        viewModelScope.launch {
            repository.seedDefaultAdminIfNeeded()
            // Auto-restore any existing data from Firebase Firestore
            pullDataFromCloud(isInitial = true)
        }
    }

    // --- AUTHENTICATION & SESSION STATE ---
    private val _currentUser = MutableStateFlow<com.example.data.model.UserAccount?>(
        com.example.data.model.UserAccount(
            name = "Harrison Daniel",
            email = "admin@deetrendglobal.com.ng",
            role = com.example.data.model.UserRole.ADMINISTRATOR
        )
    )
    val currentUser: StateFlow<com.example.data.model.UserAccount?> = _currentUser.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(true) // Authenticated by default for instant preview
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val loginEmail = MutableStateFlow("admin@deetrendglobal.com.ng")
    val loginPassword = MutableStateFlow("deetrend2026")
    val isAuthenticating = MutableStateFlow(false)
    val loginError = MutableStateFlow<String?>(null)

    fun setLoginEmail(v: String) { loginEmail.value = v; loginError.value = null }
    fun setLoginPassword(v: String) { loginPassword.value = v; loginError.value = null }

    fun loginAdmin() {
        val email = loginEmail.value.trim()
        val pass = loginPassword.value
        if (email.isBlank() || pass.isBlank()) {
            loginError.value = "Please enter both email and password."
            return
        }
        viewModelScope.launch {
            isAuthenticating.value = true
            loginError.value = null
            val user = repository.authenticateUser(email, pass)
            isAuthenticating.value = false
            if (user != null) {
                _currentUser.value = user
                _isAuthenticated.value = true
                _currentScreen.value = DeetrendScreen.ADMIN_DASHBOARD
                showMessage("Welcome back, ${user.name} (${user.role})")
            } else {
                loginError.value = "Invalid administrator credentials. Please check your email/password."
            }
        }
    }

    fun quickDemoAdminLogin() {
        _currentUser.value = com.example.data.model.UserAccount(
            name = "Harrison Daniel",
            email = "admin@deetrendglobal.com.ng",
            role = com.example.data.model.UserRole.ADMINISTRATOR
        )
        _isAuthenticated.value = true
        _currentScreen.value = DeetrendScreen.ADMIN_DASHBOARD
        showMessage("Logged in as Administrator (Harrison Daniel)")
    }

    fun logout() {
        viewModelScope.launch {
            _currentUser.value?.let {
                repository.logAuditEvent(
                    user = it.name,
                    action = "Admin Logout",
                    record = it.email,
                    details = "Admin session terminated"
                )
            }
            _currentUser.value = null
            _isAuthenticated.value = false
            _currentScreen.value = DeetrendScreen.ADMIN_LOGIN
            showMessage("You have been signed out.")
        }
    }

    // --- NAVIGATION & MODE STATE ---
    private val _currentScreen = MutableStateFlow(DeetrendScreen.ADMIN_DASHBOARD)
    val currentScreen: StateFlow<DeetrendScreen> = _currentScreen.asStateFlow()

    private val _userMode = MutableStateFlow(ActiveUserMode.ADMIN)
    val userMode: StateFlow<ActiveUserMode> = _userMode.asStateFlow()

    // --- REPOSITORY DATA STREAMS ---
    val allApplications: StateFlow<List<LoanApplicationEntity>> = repository.allApplications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApprovedLoans: StateFlow<List<ApprovedLoan>> = repository.allApprovedLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAgreements: StateFlow<List<LoanAgreement>> = repository.allAgreements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBorrowers: StateFlow<List<Borrower>> = repository.allBorrowers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentRecord>> = repository.allPayments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAuditLogs: StateFlow<List<com.example.data.model.AuditLog>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<com.example.data.model.LendingDocument>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SELECTED ENTITIES ---
    private val _selectedLoan = MutableStateFlow<ApprovedLoan?>(null)
    val selectedLoan: StateFlow<ApprovedLoan?> = _selectedLoan.asStateFlow()

    private val _selectedLoanInstallments = MutableStateFlow<List<DeetrendInstallment>>(emptyList())
    val selectedLoanInstallments: StateFlow<List<DeetrendInstallment>> = _selectedLoanInstallments.asStateFlow()

    private val _selectedAgreement = MutableStateFlow<LoanAgreement?>(null)
    val selectedAgreement: StateFlow<LoanAgreement?> = _selectedAgreement.asStateFlow()

    // Public Form State
    private val _applicationForm = MutableStateFlow(PublicApplicationFormState())
    val applicationForm: StateFlow<PublicApplicationFormState> = _applicationForm.asStateFlow()

    // Approval Configuration Form State
    private val _approvalConfig = MutableStateFlow(LoanApprovalConfigState())
    val approvalConfig: StateFlow<LoanApprovalConfigState> = _approvalConfig.asStateFlow()

    // Public Agreement / Borrower Link State
    private val _borrowerToken = MutableStateFlow<String?>(null)
    val borrowerToken: StateFlow<String?> = _borrowerToken.asStateFlow()

    private val _borrowerAgreement = MutableStateFlow<LoanAgreement?>(null)
    val borrowerAgreement: StateFlow<LoanAgreement?> = _borrowerAgreement.asStateFlow()

    private val _borrowerConsentAgreed = MutableStateFlow(false)
    val borrowerConsentAgreed: StateFlow<Boolean> = _borrowerConsentAgreed.asStateFlow()

    private val _borrowerConsentName = MutableStateFlow("")
    val borrowerConsentName: StateFlow<String> = _borrowerConsentName.asStateFlow()

    // Feedback Toast / SnackBar
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isSyncingCloud = MutableStateFlow(false)
    val isSyncingCloud: StateFlow<Boolean> = _isSyncingCloud.asStateFlow()

    private val _isPullingCloud = MutableStateFlow(false)
    val isPullingCloud: StateFlow<Boolean> = _isPullingCloud.asStateFlow()

    fun pullDataFromCloud(isInitial: Boolean = false) {
        viewModelScope.launch {
            _isPullingCloud.value = true
            try {
                val result = repository.pullAllDataFromFirebase()
                result.fold(
                    onSuccess = { count ->
                        if (!isInitial || count > 0) {
                            showMessage("Restored $count records from Firebase Cloud.")
                        }
                    },
                    onFailure = { err ->
                        if (!isInitial) {
                            showMessage("Cloud Restore Error: ${err.localizedMessage ?: "Check connection"}")
                        }
                    }
                )
            } finally {
                _isPullingCloud.value = false
            }
        }
    }

    fun syncAllDataToCloud() {
        viewModelScope.launch {
            _isSyncingCloud.value = true
            try {
                val result = repository.syncAllDataToFirebase()
                result.fold(
                    onSuccess = { count ->
                        showMessage("Cloud Sync Success: $count records saved to Firebase Firestore.")
                    },
                    onFailure = { err ->
                        showMessage("Sync Error: ${err.localizedMessage ?: "Check network connection"}")
                    }
                )
            } finally {
                _isSyncingCloud.value = false
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun navigateTo(screen: DeetrendScreen) {
        _currentScreen.value = screen
    }

    fun setUserMode(mode: ActiveUserMode) {
        _userMode.value = mode
        if (mode == ActiveUserMode.BORROWER && _currentScreen.value != DeetrendScreen.BORROWER_AGREEMENT) {
            _currentScreen.value = DeetrendScreen.PUBLIC_APPLY
        } else if (mode == ActiveUserMode.ADMIN && (_currentScreen.value == DeetrendScreen.PUBLIC_APPLY || _currentScreen.value == DeetrendScreen.BORROWER_AGREEMENT)) {
            _currentScreen.value = DeetrendScreen.ADMIN_DASHBOARD
        }
    }

    // --- PUBLIC APPLICATION FORM ACTIONS ---
    fun updateFullName(v: String) { _applicationForm.value = _applicationForm.value.copy(fullName = v, errorMessage = null) }
    fun updatePhoneNumber(v: String) { _applicationForm.value = _applicationForm.value.copy(phoneNumber = v, errorMessage = null) }
    fun updateOccupationType(v: String) { _applicationForm.value = _applicationForm.value.copy(occupationType = v) }
    fun updateHomeAddress(v: String) { _applicationForm.value = _applicationForm.value.copy(homeAddress = v, errorMessage = null) }
    fun updateWorkLocation(v: String) { _applicationForm.value = _applicationForm.value.copy(workOrBusinessLocation = v) }
    fun updateAmountRequested(v: String) { _applicationForm.value = _applicationForm.value.copy(amountRequested = v, errorMessage = null) }
    fun updateLoanPurpose(v: String) { _applicationForm.value = _applicationForm.value.copy(loanPurpose = v) }
    fun updateRepaymentPlan(v: String) { _applicationForm.value = _applicationForm.value.copy(repaymentPlan = v) }
    fun updateCollateralProvided(v: Boolean) { _applicationForm.value = _applicationForm.value.copy(collateralProvided = v) }
    fun updateCollateralDescription(v: String) { _applicationForm.value = _applicationForm.value.copy(collateralDescription = v) }
    fun updateLoanTenure(v: Int) { _applicationForm.value = _applicationForm.value.copy(loanTenureMonths = v) }
    fun updateAccountName(v: String) { _applicationForm.value = _applicationForm.value.copy(accountName = v) }
    fun updateBankName(v: String) { _applicationForm.value = _applicationForm.value.copy(bankName = v) }
    fun updateAccountNumber(v: String) { _applicationForm.value = _applicationForm.value.copy(accountNumber = v) }

    fun resetApplicationForm() {
        _applicationForm.value = PublicApplicationFormState()
    }

    fun submitPublicApplication() {
        val form = _applicationForm.value
        // Validations
        if (form.fullName.isBlank()) {
            _applicationForm.value = form.copy(errorMessage = "Please enter your full name.")
            return
        }
        if (form.phoneNumber.isBlank()) {
            _applicationForm.value = form.copy(errorMessage = "Please enter a valid phone number.")
            return
        }
        if (form.homeAddress.isBlank()) {
            _applicationForm.value = form.copy(errorMessage = "Personal Home Address is required.")
            return
        }
        val amount = form.amountRequested.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _applicationForm.value = form.copy(errorMessage = "Loan amount must be greater than zero.")
            return
        }
        if (form.collateralProvided && form.collateralDescription.isBlank()) {
            _applicationForm.value = form.copy(errorMessage = "Please provide a description of the collateral.")
            return
        }

        viewModelScope.launch {
            _applicationForm.value = form.copy(isSubmitting = true, errorMessage = null)
            try {
                val generatedId = repository.submitPublicLoanApplication(
                    fullName = form.fullName,
                    phoneNumber = form.phoneNumber,
                    occupationType = form.occupationType,
                    homeAddress = form.homeAddress,
                    workOrBusinessLocation = form.workOrBusinessLocation,
                    amountRequested = amount,
                    loanPurpose = form.loanPurpose,
                    repaymentPlan = form.repaymentPlan,
                    collateralProvided = form.collateralProvided,
                    collateralDescription = form.collateralDescription,
                    loanTenureMonths = form.loanTenureMonths,
                    accountName = form.accountName,
                    bankName = form.bankName,
                    accountNumber = form.accountNumber
                )
                _applicationForm.value = form.copy(
                    isSubmitting = false,
                    submittedApplicationId = generatedId
                )
                showMessage("Your loan application has been submitted successfully. Reference: $generatedId")
            } catch (e: Exception) {
                _applicationForm.value = form.copy(
                    isSubmitting = false,
                    errorMessage = "Unable to submit application. Please try again."
                )
            }
        }
    }

    // --- ADMIN APPROVAL & CONFIGURATION ACTIONS ---
    fun openApprovalConfiguration(app: LoanApplicationEntity) {
        val defaultCollateralChoice = if (app.collateral_provided && app.collateral_description.isNotBlank()) {
            "Use Collateral Provided in Application"
        } else {
            "No Collateral"
        }
        _approvalConfig.value = LoanApprovalConfigState(
            application = app,
            approvedAmount = String.format(Locale.US, "%.0f", app.amount_requested),
            monthlyInterestRate = "7.0",
            tenureMonths = app.loan_tenure_months,
            amortizationType = AmortizationType.FLAT_INTEREST,
            repaymentFrequency = if (app.repayment_plan.equals("Weekly", ignoreCase = true)) RepaymentFrequency.WEEKLY else RepaymentFrequency.MONTHLY,
            collateralChoice = defaultCollateralChoice,
            collateralDescription = app.collateral_description
        )
        _currentScreen.value = DeetrendScreen.ADMIN_APPROVAL_CONFIG
    }

    fun updateApprovedAmount(v: String) { _approvalConfig.value = _approvalConfig.value.copy(approvedAmount = v, errorMessage = null) }
    fun updateMonthlyInterestRate(v: String) { _approvalConfig.value = _approvalConfig.value.copy(monthlyInterestRate = v, errorMessage = null) }
    fun updateApprovalTenure(v: Int) { _approvalConfig.value = _approvalConfig.value.copy(tenureMonths = v) }
    fun updateAmortizationType(v: AmortizationType) { _approvalConfig.value = _approvalConfig.value.copy(amortizationType = v) }
    fun updateRepaymentFrequency(v: RepaymentFrequency) { _approvalConfig.value = _approvalConfig.value.copy(repaymentFrequency = v) }
    fun updateCollateralChoice(v: String) { _approvalConfig.value = _approvalConfig.value.copy(collateralChoice = v) }
    fun updateApprovalCollateralDesc(v: String) { _approvalConfig.value = _approvalConfig.value.copy(collateralDescription = v) }

    fun calculateCurrentApprovalPreview(): LoanCalculationResult? {
        val config = _approvalConfig.value
        val amount = config.approvedAmount.toDoubleOrNull() ?: return null
        val rate = config.monthlyInterestRate.toDoubleOrNull() ?: return null
        if (amount <= 0.0 || rate < 0.0) return null

        return DeetrendLoanCalculator.calculateLoan(
            loanId = "DGE-PREVIEW",
            principal = amount,
            monthlyInterestRatePercent = rate,
            tenureMonths = config.tenureMonths,
            amortizationType = config.amortizationType,
            repaymentFrequency = config.repaymentFrequency
        )
    }

    fun confirmLoanApproval() {
        val config = _approvalConfig.value
        val app = config.application ?: return
        val amount = config.approvedAmount.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _approvalConfig.value = config.copy(errorMessage = "Approved loan amount must be greater than zero.")
            return
        }
        val rate = config.monthlyInterestRate.toDoubleOrNull()
        if (rate == null || rate < 0.0) {
            _approvalConfig.value = config.copy(errorMessage = "Please enter a valid positive interest rate.")
            return
        }

        val finalCollateral = when (config.collateralChoice) {
            "No Collateral" -> ""
            "Use Collateral Provided in Application" -> app.collateral_description
            else -> config.collateralDescription
        }

        viewModelScope.launch {
            _approvalConfig.value = config.copy(isProcessing = true, errorMessage = null)
            try {
                val createdLoan = repository.approveAndConfigureLoan(
                    applicationId = app.application_id,
                    approvedAmount = amount,
                    monthlyInterestRate = rate,
                    tenureMonths = config.tenureMonths,
                    amortizationType = config.amortizationType,
                    repaymentFrequency = config.repaymentFrequency,
                    collateralDescription = finalCollateral
                )
                _selectedLoan.value = createdLoan
                loadInstallmentsForLoan(createdLoan.id)
                showMessage("Loan approved successfully! Generated Loan ID: ${createdLoan.loan_id}")
                _currentScreen.value = DeetrendScreen.ADMIN_LOAN_DETAIL
            } catch (e: Exception) {
                _approvalConfig.value = config.copy(isProcessing = false, errorMessage = "Error approving loan: ${e.message}")
            }
        }
    }

    fun rejectApplication(applicationId: String, reason: String) {
        viewModelScope.launch {
            try {
                repository.rejectApplication(applicationId, reason)
                showMessage("Application $applicationId rejected.")
            } catch (e: Exception) {
                showMessage("Error rejecting application: ${e.message}")
            }
        }
    }

    // --- LOAN DETAILS & AGREEMENT GENERATION ---
    fun selectLoan(loan: ApprovedLoan) {
        _selectedLoan.value = loan
        loadInstallmentsForLoan(loan.id)
        _currentScreen.value = DeetrendScreen.ADMIN_LOAN_DETAIL
    }

    fun loadInstallmentsForLoan(loanPrimaryId: Long) {
        viewModelScope.launch {
            repository.getInstallmentsForLoan(loanPrimaryId).collect {
                _selectedLoanInstallments.value = it
            }
        }
    }

    fun generateAgreementForSelectedLoan() {
        val loan = _selectedLoan.value ?: return
        viewModelScope.launch {
            try {
                val agreement = repository.generateLoanAgreement(loan.id)
                _selectedAgreement.value = agreement
                showMessage("Loan agreement generated: ${agreement.agreement_id}")
                _currentScreen.value = DeetrendScreen.ADMIN_AGREEMENTS
            } catch (e: Exception) {
                showMessage("Failed to generate agreement: ${e.message}")
            }
        }
    }

    fun selectAgreement(agreement: LoanAgreement) {
        _selectedAgreement.value = agreement
        _currentScreen.value = DeetrendScreen.ADMIN_AGREEMENTS
    }

    // --- BORROWER AGREEMENT LINK & CONSENT ---
    fun openBorrowerAgreementLink(token: String) {
        _borrowerToken.value = token
        _borrowerConsentAgreed.value = false
        _borrowerConsentName.value = ""
        viewModelScope.launch {
            val ag = repository.getAgreementByToken(token)
            _borrowerAgreement.value = ag
            if (ag != null) {
                _borrowerConsentName.value = ag.borrower_name
                _currentScreen.value = DeetrendScreen.BORROWER_AGREEMENT
            } else {
                showMessage("Agreement link is invalid or has expired.")
            }
        }
    }

    fun setBorrowerConsentAgreed(agreed: Boolean) {
        _borrowerConsentAgreed.value = agreed
    }

    fun setBorrowerConsentName(name: String) {
        _borrowerConsentName.value = name
    }

    fun submitBorrowerAgreementConsent() {
        val token = _borrowerToken.value ?: return
        val agreement = _borrowerAgreement.value ?: return

        if (!_borrowerConsentAgreed.value) {
            showMessage("You must agree to the terms to proceed.")
            return
        }
        val name = _borrowerConsentName.value.trim()
        if (name.isBlank()) {
            showMessage("Please enter your full name for electronic consent.")
            return
        }

        viewModelScope.launch {
            val success = repository.submitBorrowerConsent(token, name)
            if (success) {
                // Refresh local agreement
                _borrowerAgreement.value = repository.getAgreementByToken(token)
                showMessage("Loan agreement accepted and submitted successfully!")
            } else {
                showMessage("Unable to submit agreement consent. Please try again.")
            }
        }
    }

    // --- REPAYMENT RECORDING ---
    fun recordRepayment(loanId: String, borrowerId: Long, amount: Double, method: String, reference: String, notes: String) {
        viewModelScope.launch {
            val ok = repository.recordRepayment(loanId, borrowerId, amount, method, reference, notes)
            if (ok) {
                _selectedLoan.value = repository.getApprovedLoanByLoanId(loanId)
                showMessage("Repayment of ₦$amount recorded successfully.")
            } else {
                showMessage("Failed to record repayment.")
            }
        }
    }
}
