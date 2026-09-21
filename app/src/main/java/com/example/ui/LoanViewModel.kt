package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LoanDatabase
import com.example.data.model.AmortizationInstallment
import com.example.data.model.LoanApplication
import com.example.data.model.LoanReminder
import com.example.data.model.PaymentTransaction
import com.example.data.repository.LoanRepository
import com.example.domain.calculator.LoanCalculator
import com.example.notification.LoanNotificationHelper
import com.example.domain.export.LoanAgreementDocument
import com.example.domain.export.LoanAgreementExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    DASHBOARD,
    NEW_APPLICATION,
    AGREEMENT_VIEW,
    LOAN_DETAIL,
    REMINDERS
}

data class LoanApplicationFormState(
    val borrowerName: String = "",
    val borrowerEmail: String = "",
    val borrowerPhone: String = "",
    val borrowerIdNumber: String = "",
    val employmentStatus: String = "Employed",
    val monthlyIncomeText: String = "5500",
    val loanPurpose: String = "Personal Expenses",
    val principalText: String = "10000",
    val termMonths: Int = 12,
    val nameError: String? = null,
    val emailError: String? = null,
    val principalError: String? = null
) {
    val principalAmount: Double
        get() = principalText.toDoubleOrNull() ?: 0.0

    val monthlyIncome: Double
        get() = monthlyIncomeText.toDoubleOrNull() ?: 0.0

    val calculatedInterestRate: Double
        get() = LoanCalculator.calculateAnnualInterestRate(principalAmount, termMonths)

    val monthlyPayment: Double
        get() = LoanCalculator.calculateMonthlyPayment(principalAmount, calculatedInterestRate, termMonths)

    val totalRepayment: Double
        get() = LoanCalculator.roundTwoDecimals(monthlyPayment * termMonths)

    val totalInterest: Double
        get() = LoanCalculator.roundTwoDecimals(totalRepayment - principalAmount)
}

class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LoanRepository
    private val notificationHelper: LoanNotificationHelper

    init {
        val db = LoanDatabase.getDatabase(application)
        repository = LoanRepository(db.loanDao())
        notificationHelper = LoanNotificationHelper(application)

        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val allLoans: StateFlow<List<LoanApplication>> = repository.allLoans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeReminders: StateFlow<List<LoanReminder>> = repository.activeReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allPayments: StateFlow<List<PaymentTransaction>> = repository.allPayments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedLoanId = MutableStateFlow<Long?>(null)
    val selectedLoanId: StateFlow<Long?> = _selectedLoanId.asStateFlow()

    private val _selectedLoanInstallments = MutableStateFlow<List<AmortizationInstallment>>(emptyList())
    val selectedLoanInstallments: StateFlow<List<AmortizationInstallment>> = _selectedLoanInstallments.asStateFlow()

    private val _selectedLoanPayments = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    val selectedLoanPayments: StateFlow<List<PaymentTransaction>> = _selectedLoanPayments.asStateFlow()

    private val _applicationForm = MutableStateFlow(LoanApplicationFormState())
    val applicationForm: StateFlow<LoanApplicationFormState> = _applicationForm.asStateFlow()

    private val _notificationFeedbackMessage = MutableStateFlow<String?>(null)
    val notificationFeedbackMessage: StateFlow<String?> = _notificationFeedbackMessage.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectLoan(loanId: Long, openAgreement: Boolean = false) {
        _selectedLoanId.value = loanId
        viewModelScope.launch {
            repository.getInstallments(loanId).collect {
                _selectedLoanInstallments.value = it
            }
        }
        viewModelScope.launch {
            repository.getPayments(loanId).collect {
                _selectedLoanPayments.value = it
            }
        }
        _currentScreen.value = if (openAgreement) AppScreen.AGREEMENT_VIEW else AppScreen.LOAN_DETAIL
    }

    fun updateFormName(value: String) {
        _applicationForm.value = _applicationForm.value.copy(borrowerName = value, nameError = null)
    }

    fun updateFormEmail(value: String) {
        _applicationForm.value = _applicationForm.value.copy(borrowerEmail = value, emailError = null)
    }

    fun updateFormPhone(value: String) {
        _applicationForm.value = _applicationForm.value.copy(borrowerPhone = value)
    }

    fun updateFormIdNumber(value: String) {
        _applicationForm.value = _applicationForm.value.copy(borrowerIdNumber = value)
    }

    fun updateFormEmployment(value: String) {
        _applicationForm.value = _applicationForm.value.copy(employmentStatus = value)
    }

    fun updateFormIncome(value: String) {
        _applicationForm.value = _applicationForm.value.copy(monthlyIncomeText = value)
    }

    fun updateFormPurpose(value: String) {
        _applicationForm.value = _applicationForm.value.copy(loanPurpose = value)
    }

    fun updateFormPrincipal(value: String) {
        _applicationForm.value = _applicationForm.value.copy(principalText = value, principalError = null)
    }

    fun updateFormTerm(months: Int) {
        _applicationForm.value = _applicationForm.value.copy(termMonths = months)
    }

    fun resetApplicationForm() {
        _applicationForm.value = LoanApplicationFormState()
    }

    fun submitLoanApplication(): Boolean {
        val form = _applicationForm.value
        var hasError = false

        if (form.borrowerName.isBlank()) {
            _applicationForm.value = _applicationForm.value.copy(nameError = "Borrower name is required")
            hasError = true
        }

        if (form.borrowerEmail.isBlank() || !form.borrowerEmail.contains("@")) {
            _applicationForm.value = _applicationForm.value.copy(emailError = "Valid email is required")
            hasError = true
        }

        if (form.principalAmount <= 0) {
            _applicationForm.value = _applicationForm.value.copy(principalError = "Please enter a valid loan amount")
            hasError = true
        }

        if (hasError) return false

        viewModelScope.launch {
            val loanId = repository.createLoanApplication(
                borrowerName = form.borrowerName.trim(),
                borrowerEmail = form.borrowerEmail.trim(),
                borrowerPhone = form.borrowerPhone.trim(),
                borrowerIdNumber = form.borrowerIdNumber.trim().ifEmpty { "ID-" + (100000..999999).random() },
                employmentStatus = form.employmentStatus,
                monthlyIncome = form.monthlyIncome,
                loanPurpose = form.loanPurpose,
                principalAmount = form.principalAmount,
                termMonths = form.termMonths
            )
            selectLoan(loanId, openAgreement = true)
        }
        return true
    }

    fun signAgreementAndActivate(loanId: Long, signature: String) {
        viewModelScope.launch {
            val success = repository.signAgreementAndActivateLoan(loanId, signature)
            if (success) {
                selectLoan(loanId, openAgreement = false)
            }
        }
    }

    fun recordPayment(
        loanId: Long,
        installmentId: Long?,
        installmentNumber: Int?,
        amount: Double,
        paymentMethod: String,
        referenceNote: String
    ) {
        viewModelScope.launch {
            repository.recordPayment(
                loanId = loanId,
                installmentId = installmentId,
                installmentNumber = installmentNumber,
                amountPaid = amount,
                paymentMethod = paymentMethod,
                referenceNote = referenceNote
            )
        }
    }

    fun dismissReminder(reminderId: Long) {
        viewModelScope.launch {
            repository.dismissReminder(reminderId)
        }
    }

    fun triggerAutomatedRemindersNow() {
        viewModelScope.launch {
            val reminders = activeReminders.value
            if (reminders.isNotEmpty()) {
                reminders.forEach { reminder ->
                    notificationHelper.showReminderNotification(reminder)
                }
                _notificationFeedbackMessage.value = "Dispatched ${reminders.size} automated payment reminder(s) to notifications!"
            } else {
                _notificationFeedbackMessage.value = "All active loan installments are currently up to date."
            }
        }
    }

    fun clearFeedbackMessage() {
        _notificationFeedbackMessage.value = null
    }

    fun exportLoanAgreementDocument(loan: LoanApplication): LoanAgreementDocument {
        val installments = if (loan.id == _selectedLoanId.value) _selectedLoanInstallments.value else emptyList()
        return LoanAgreementExporter.toDocument(loan, installments)
    }

    fun exportLoanAgreementFormattedText(loan: LoanApplication): String {
        val document = exportLoanAgreementDocument(loan)
        return LoanAgreementExporter.exportToFormattedText(document)
    }

    fun exportLoanAgreementHtml(loan: LoanApplication): String {
        val document = exportLoanAgreementDocument(loan)
        return LoanAgreementExporter.exportToHtml(document)
    }

    fun notifyExportSuccess(label: String) {
        _notificationFeedbackMessage.value = label
    }
}
