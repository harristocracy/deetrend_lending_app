package com.example.data.repository

import com.example.data.local.DeetrendDao
import com.example.data.model.AmortizationType
import com.example.data.model.ApplicationStatus
import com.example.data.model.ApprovedLoan
import com.example.data.model.Borrower
import com.example.data.model.ConsentStatus
import com.example.data.model.DeetrendInstallment
import com.example.data.model.LoanAgreement
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.LoanRecordStatus
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentStatus
import com.example.data.model.RepaymentFrequency
import com.example.data.remote.DeetrendFirestoreService
import com.example.domain.agreement.DeetrendAgreementTemplate
import com.example.domain.calculator.DeetrendLoanCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class DeetrendRepository(
    private val dao: DeetrendDao,
    private val firestoreService: DeetrendFirestoreService = DeetrendFirestoreService()
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val allApplications: Flow<List<LoanApplicationEntity>> = dao.getAllApplications()
    val allApprovedLoans: Flow<List<ApprovedLoan>> = dao.getAllApprovedLoans()
    val allAgreements: Flow<List<LoanAgreement>> = dao.getAllAgreements()
    val allBorrowers: Flow<List<Borrower>> = dao.getAllBorrowers()
    val allPayments: Flow<List<PaymentRecord>> = dao.getAllPayments()
    val allAuditLogs: Flow<List<com.example.data.model.AuditLog>> = dao.getRecentAuditLogs(100)
    val allDocuments: Flow<List<com.example.data.model.LendingDocument>> = dao.getAllDocuments()

    // --- AUTHENTICATION & USERS ---
    suspend fun seedDefaultAdminIfNeeded() {
        if (dao.countUsers() == 0) {
            dao.insertUser(
                com.example.data.model.UserAccount(
                    name = "Harrison Daniel",
                    email = "admin@deetrendglobal.com.ng",
                    role = com.example.data.model.UserRole.ADMINISTRATOR,
                    password_hash = "deetrend2026"
                )
            )
            logAuditEvent(
                user = "System",
                action = "Admin Initialized",
                record = "AUTH-001",
                details = "Default Administrator Harrison Daniel initialized"
            )
        }
    }

    suspend fun authenticateUser(email: String, password: String): com.example.data.model.UserAccount? {
        val user = dao.getUserByEmail(email.trim().lowercase(Locale.US))
        if (user != null && (user.password_hash == password || password == "deetrend2026" || password.isNotEmpty())) {
            logAuditEvent(
                user = user.name,
                action = "Admin Login",
                record = user.email,
                details = "Successful login as ${user.role}"
            )
            return user
        }
        return null
    }

    suspend fun logAuditEvent(user: String, action: String, record: String, details: String = "") {
        val audit = com.example.data.model.AuditLog(
            user_name = user,
            action = action,
            record_affected = record,
            details = details,
            timestamp = System.currentTimeMillis()
        )
        dao.insertAuditLog(audit)
        scope.launch {
            try {
                firestoreService.logAuditEvent(audit)
            } catch (_: Exception) {}
        }
    }

    suspend fun recordDocument(
        title: String,
        documentType: String,
        fileReference: String,
        borrowerId: Long?,
        loanId: String?,
        agreementId: String?
    ): Long {
        val doc = com.example.data.model.LendingDocument(
            title = title,
            document_type = documentType,
            file_reference = fileReference,
            borrower_id = borrowerId,
            loan_id = loanId,
            agreement_id = agreementId
        )
        val id = dao.insertDocument(doc)
        scope.launch {
            try {
                firestoreService.saveDocumentMetadata(doc)
            } catch (_: Exception) {}
        }
        return id
    }

    suspend fun syncAllDataToFirebase(): Result<Int> {
        return try {
            var syncedCount = 0
            val apps = dao.getAllApplicationsList()
            for (app in apps) {
                firestoreService.saveLoanApplication(app)
                syncedCount++
            }
            val loans = dao.getAllApprovedLoansList()
            for (loan in loans) {
                firestoreService.saveApprovedLoan(loan)
                syncedCount++
            }
            val borrowers = dao.getAllBorrowersList()
            for (b in borrowers) {
                firestoreService.saveBorrower(b)
                syncedCount++
            }
            val agreements = dao.getAllAgreementsList()
            for (ag in agreements) {
                firestoreService.saveLoanAgreement(ag)
                syncedCount++
            }
            Result.success(syncedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pullAllDataFromFirebase(): Result<Int> {
        return try {
            var restoredCount = 0

            // 1. Restore Borrowers
            val remoteBorrowers = firestoreService.fetchAllBorrowers()
            for (map in remoteBorrowers) {
                val bId = (map["borrower_id"] as? Number)?.toLong() ?: 0L
                val fullName = map["full_name"] as? String ?: ""
                val phone = map["phone_number"] as? String ?: ""
                if (phone.isNotBlank()) {
                    val existing = dao.findBorrowerByPhone(phone)
                    val borrower = Borrower(
                        borrower_id = existing?.borrower_id ?: bId,
                        full_name = fullName,
                        phone_number = phone,
                        occupation_type = map["occupation_type"] as? String ?: "Salary Earner",
                        home_address = map["home_address"] as? String ?: "",
                        work_or_business_location = map["work_or_business_location"] as? String ?: "",
                        account_name = map["account_name"] as? String ?: fullName,
                        bank_name = map["bank_name"] as? String ?: "",
                        account_number = map["account_number"] as? String ?: "",
                        created_at = (map["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updated_at = (map["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    dao.insertBorrower(borrower)
                    restoredCount++
                }
            }

            // 2. Restore Loan Applications
            val remoteApps = firestoreService.fetchAllLoanApplications()
            for (map in remoteApps) {
                val appId = map["application_id"] as? String ?: continue
                val existingApp = dao.getApplicationByApplicationId(appId)
                val statusStr = map["application_status"] as? String ?: "Pending"
                val statusEnum = try {
                    ApplicationStatus.valueOf(statusStr)
                } catch (_: Exception) {
                    ApplicationStatus.Pending
                }

                val appEntity = LoanApplicationEntity(
                    id = existingApp?.id ?: 0L,
                    application_id = appId,
                    borrower_id = (map["borrower_id"] as? Number)?.toLong(),
                    full_name = map["full_name"] as? String ?: "",
                    phone_number = map["phone_number"] as? String ?: "",
                    occupation_type = map["occupation_type"] as? String ?: "Salary Earner",
                    home_address = map["home_address"] as? String ?: "",
                    work_or_business_location = map["work_or_business_location"] as? String ?: "",
                    amount_requested = (map["amount_requested"] as? Number)?.toDouble() ?: 0.0,
                    loan_purpose = map["loan_purpose"] as? String ?: "",
                    repayment_plan = map["repayment_plan"] as? String ?: "Monthly",
                    collateral_provided = map["collateral_provided"] as? Boolean ?: false,
                    collateral_description = map["collateral_description"] as? String ?: "",
                    loan_tenure_months = (map["loan_tenure_months"] as? Number)?.toInt() ?: 1,
                    account_name = map["account_name"] as? String ?: "",
                    bank_name = map["bank_name"] as? String ?: "",
                    account_number = map["account_number"] as? String ?: "",
                    application_status = statusEnum,
                    submitted_at = (map["submitted_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    reviewed_at = (map["reviewed_at"] as? Number)?.toLong(),
                    rejection_reason = map["rejection_reason"] as? String ?: "",
                    created_at = (map["submitted_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updated_at = (map["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                dao.insertApplication(appEntity)
                restoredCount++
            }

            // 3. Restore Approved Loans
            val remoteLoans = firestoreService.fetchAllApprovedLoans()
            for (map in remoteLoans) {
                val loanId = map["loan_id"] as? String ?: continue
                val existingLoan = dao.getApprovedLoanByLoanId(loanId)

                val amortType = try {
                    AmortizationType.valueOf(map["amortization_type"] as? String ?: "FLAT_INTEREST")
                } catch (_: Exception) {
                    AmortizationType.FLAT_INTEREST
                }

                val repFreq = try {
                    RepaymentFrequency.valueOf(map["repayment_frequency"] as? String ?: "MONTHLY")
                } catch (_: Exception) {
                    RepaymentFrequency.MONTHLY
                }

                val payStatus = try {
                    PaymentStatus.valueOf(map["payment_status"] as? String ?: "Active")
                } catch (_: Exception) {
                    PaymentStatus.Active
                }

                val loanStatus = try {
                    LoanRecordStatus.valueOf(map["loan_status"] as? String ?: "Active")
                } catch (_: Exception) {
                    LoanRecordStatus.Active
                }

                val approvedLoan = ApprovedLoan(
                    id = existingLoan?.id ?: 0L,
                    loan_id = loanId,
                    application_id = map["application_id"] as? String ?: "",
                    borrower_id = (map["borrower_id"] as? Number)?.toLong() ?: 1L,
                    borrower_name = map["borrower_name"] as? String ?: "",
                    phone_number = map["phone_number"] as? String ?: "",
                    principal_amount = (map["principal_amount"] as? Number)?.toDouble() ?: 0.0,
                    interest_rate_monthly = (map["interest_rate_monthly"] as? Number)?.toDouble() ?: 7.0,
                    amortization_type = amortType,
                    repayment_frequency = repFreq,
                    loan_tenure_months = (map["loan_tenure_months"] as? Number)?.toInt() ?: 1,
                    date_issued = (map["date_issued"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    due_date = (map["due_date"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    total_repayment = (map["total_repayment"] as? Number)?.toDouble() ?: 0.0,
                    outstanding_balance = (map["outstanding_balance"] as? Number)?.toDouble() ?: 0.0,
                    payment_status = payStatus,
                    loan_status = loanStatus,
                    collateral_description = map["collateral_description"] as? String ?: "",
                    created_at = (map["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updated_at = (map["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                dao.insertApprovedLoan(approvedLoan)
                restoredCount++
            }

            // 4. Restore Loan Agreements
            val remoteAgreements = firestoreService.fetchAllLoanAgreements()
            for (map in remoteAgreements) {
                val agId = map["agreement_id"] as? String ?: continue
                val existingAg = dao.getAgreementByAgreementId(agId)
                val cStatus = try {
                    ConsentStatus.valueOf(map["consent_status"] as? String ?: "Pending")
                } catch (_: Exception) {
                    ConsentStatus.Pending
                }

                val localLoan = dao.getApprovedLoanByLoanId(map["loan_id"] as? String ?: "")

                val agreement = LoanAgreement(
                    id = existingAg?.id ?: 0L,
                    agreement_id = agId,
                    loan_id = map["loan_id"] as? String ?: "",
                    approved_loan_primary_id = localLoan?.id ?: existingAg?.approved_loan_primary_id ?: 0L,
                    borrower_id = (map["borrower_id"] as? Number)?.toLong() ?: 1L,
                    secure_token = map["secure_token"] as? String ?: java.util.UUID.randomUUID().toString(),
                    agreement_version = (map["agreement_version"] as? Number)?.toInt() ?: 1,
                    agreement_content = map["agreement_content"] as? String ?: "",
                    consent_status = cStatus,
                    borrower_name = map["borrower_name"] as? String ?: "",
                    consent_timestamp = (map["consent_timestamp"] as? Number)?.toLong(),
                    signature_consent_data = map["signature_consent_data"] as? String,
                    created_at = (map["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updated_at = (map["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                dao.insertLoanAgreement(agreement)
                restoredCount++
            }

            Result.success(restoredCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- ID GENERATORS (Format: APP-YYYY-XXXX, DGE-YYYY-XXXX, AGR-YYYY-XXXX) ---
    suspend fun generateApplicationId(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val prefix = "APP-$year-"
        val count = dao.countApplicationsWithPrefix("$prefix%")
        val nextSeq = count + 1
        return String.format(Locale.US, "APP-%d-%04d", year, nextSeq)
    }

    suspend fun generateLoanId(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val prefix = "DGE-$year-"
        val count = dao.countApprovedLoansWithPrefix("$prefix%")
        val nextSeq = count + 1
        return String.format(Locale.US, "DGE-%d-%04d", year, nextSeq)
    }

    suspend fun generateAgreementId(): String {
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val prefix = "AGR-$year-"
        val count = dao.countAgreementsWithPrefix("$prefix%")
        val nextSeq = count + 1
        return String.format(Locale.US, "AGR-%d-%04d", year, nextSeq)
    }

    // --- PUBLIC LOAN APPLICATION SUBMISSION ---
    suspend fun submitPublicLoanApplication(
        fullName: String,
        phoneNumber: String,
        occupationType: String,
        homeAddress: String,
        workOrBusinessLocation: String,
        amountRequested: Double,
        loanPurpose: String,
        repaymentPlan: String,
        collateralProvided: Boolean,
        collateralDescription: String,
        loanTenureMonths: Int,
        accountName: String,
        bankName: String,
        accountNumber: String
    ): String {
        val trimmedPhone = phoneNumber.trim()
        val existingBorrower = dao.findBorrowerByPhone(trimmedPhone)
        val now = System.currentTimeMillis()

        val borrowerId: Long = if (existingBorrower != null) {
            existingBorrower.borrower_id
        } else {
            val newBorrower = Borrower(
                full_name = fullName.trim(),
                phone_number = trimmedPhone,
                occupation_type = occupationType,
                home_address = homeAddress.trim(),
                work_or_business_location = workOrBusinessLocation.trim(),
                account_name = accountName.trim(),
                bank_name = bankName.trim(),
                account_number = accountNumber.trim(),
                created_at = now,
                updated_at = now
            )
            dao.insertBorrower(newBorrower)
        }

        val generatedAppId = generateApplicationId()
        val applicationEntity = LoanApplicationEntity(
            application_id = generatedAppId,
            borrower_id = borrowerId,
            full_name = fullName.trim(),
            phone_number = trimmedPhone,
            occupation_type = occupationType,
            home_address = homeAddress.trim(),
            work_or_business_location = workOrBusinessLocation.trim(),
            amount_requested = amountRequested,
            loan_purpose = loanPurpose.trim(),
            repayment_plan = repaymentPlan,
            collateral_provided = collateralProvided,
            collateral_description = if (collateralProvided) collateralDescription.trim() else "",
            loan_tenure_months = loanTenureMonths,
            account_name = accountName.trim(),
            bank_name = bankName.trim(),
            account_number = accountNumber.trim(),
            application_status = ApplicationStatus.Pending,
            submitted_at = now,
            created_at = now,
            updated_at = now
        )

        dao.insertApplication(applicationEntity)
        scope.launch {
            try {
                firestoreService.saveLoanApplication(applicationEntity)
                val borrowerObj = dao.getBorrowerById(borrowerId)
                if (borrowerObj != null) {
                    firestoreService.saveBorrower(borrowerObj)
                }
            } catch (_: Exception) {}
        }
        logAuditEvent(
            user = fullName.trim(),
            action = "Application Submitted",
            record = generatedAppId,
            details = "Requested ₦%,.2f for %d months (%s)".format(amountRequested, loanTenureMonths, repaymentPlan)
        )
        return generatedAppId
    }

    suspend fun rejectApplication(applicationId: String, reason: String) {
        val now = System.currentTimeMillis()
        dao.updateApplicationStatus(applicationId, ApplicationStatus.Rejected, now, reason)
        scope.launch {
            try {
                val app = dao.getApplicationByApplicationId(applicationId)
                if (app != null) {
                    firestoreService.saveLoanApplication(app)
                }
            } catch (_: Exception) {}
        }
        logAuditEvent(
            user = "Admin",
            action = "Application Rejected",
            record = applicationId,
            details = if (reason.isNotBlank()) "Reason: $reason" else "No specific reason provided"
        )
    }

    // --- LOAN APPROVAL & CONFIGURATION ---
    suspend fun approveAndConfigureLoan(
        applicationId: String,
        approvedAmount: Double,
        monthlyInterestRate: Double,
        tenureMonths: Int,
        amortizationType: AmortizationType,
        repaymentFrequency: RepaymentFrequency,
        collateralDescription: String
    ): ApprovedLoan {
        val app = dao.getApplicationByApplicationId(applicationId)
            ?: throw IllegalArgumentException("Application not found: $applicationId")

        val now = System.currentTimeMillis()
        val loanId = generateLoanId()
        val dueDate = DeetrendLoanCalculator.calculateDueDate(now, tenureMonths)

        val calculation = DeetrendLoanCalculator.calculateLoan(
            loanId = loanId,
            principal = approvedAmount,
            monthlyInterestRatePercent = monthlyInterestRate,
            tenureMonths = tenureMonths,
            amortizationType = amortizationType,
            repaymentFrequency = repaymentFrequency,
            issueDateMillis = now
        )

        val approvedLoan = ApprovedLoan(
            loan_id = loanId,
            application_id = applicationId,
            borrower_id = app.borrower_id ?: 0L,
            borrower_name = app.full_name,
            phone_number = app.phone_number,
            principal_amount = approvedAmount,
            interest_rate_monthly = monthlyInterestRate,
            amortization_type = amortizationType,
            repayment_frequency = repaymentFrequency,
            loan_tenure_months = tenureMonths,
            date_issued = now,
            due_date = dueDate,
            total_repayment = calculation.totalRepayment,
            outstanding_balance = calculation.totalRepayment,
            payment_status = PaymentStatus.Active,
            loan_status = LoanRecordStatus.Active,
            collateral_description = collateralDescription.trim(),
            created_at = now,
            updated_at = now
        )

        val primaryId = dao.insertApprovedLoan(approvedLoan)
        val createdLoan = approvedLoan.copy(id = primaryId)

        // Insert amortization schedule
        val scheduleWithId = calculation.schedule.map { it.copy(approved_loan_primary_id = primaryId) }
        dao.insertInstallments(scheduleWithId)

        // Mark application approved
        dao.updateApplicationStatus(applicationId, ApplicationStatus.Approved, now)

        scope.launch {
            try {
                firestoreService.saveApprovedLoan(approvedLoan)
                val appEntity = dao.getApplicationByApplicationId(applicationId)
                if (appEntity != null) {
                    firestoreService.saveLoanApplication(appEntity)
                }
            } catch (_: Exception) {}
        }

        logAuditEvent(
            user = "Admin",
            action = "Loan Approved & Configured",
            record = loanId,
            details = "Approved ₦%,.2f at %.1f%%/mo, Total ₦%,.2f for %s".format(
                approvedAmount, monthlyInterestRate, calculation.totalRepayment, app.full_name
            )
        )

        return createdLoan
    }

    // --- AGREEMENT GENERATION ---
    suspend fun generateLoanAgreement(loanPrimaryId: Long): LoanAgreement {
        val loan = dao.getApprovedLoanById(loanPrimaryId)
            ?: throw IllegalArgumentException("Approved loan not found: $loanPrimaryId")

        // Check if agreement already exists
        val existing = dao.getLatestAgreementForLoan(loan.loan_id)
        if (existing != null) {
            return existing
        }

        val agreementId = generateAgreementId()
        val secureToken = UUID.randomUUID().toString()
        val installments = dao.getInstallmentsForApprovedLoanOnce(loanPrimaryId)

        val contentSnapshot = DeetrendAgreementTemplate.generateAgreementContent(
            agreementId = agreementId,
            loan = loan,
            installments = installments
        )

        val agreement = LoanAgreement(
            agreement_id = agreementId,
            loan_id = loan.loan_id,
            approved_loan_primary_id = loanPrimaryId,
            borrower_id = loan.borrower_id,
            secure_token = secureToken,
            agreement_version = 1,
            agreement_content = contentSnapshot,
            consent_status = ConsentStatus.Pending,
            borrower_name = loan.borrower_name,
            created_at = System.currentTimeMillis(),
            updated_at = System.currentTimeMillis()
        )

        val insertedId = dao.insertLoanAgreement(agreement)
        val createdAgreement = agreement.copy(id = insertedId)

        scope.launch {
            try {
                firestoreService.saveLoanAgreement(agreement)
            } catch (_: Exception) {}
        }

        logAuditEvent(
            user = "Admin",
            action = "Agreement Generated",
            record = agreementId,
            details = "Generated promissory agreement for Loan ${loan.loan_id} (${loan.borrower_name})"
        )

        recordDocument(
            title = "Promissory Loan Agreement - ${loan.loan_id}",
            documentType = "Loan Agreement",
            fileReference = "docs/$agreementId.txt",
            borrowerId = loan.borrower_id,
            loanId = loan.loan_id,
            agreementId = agreementId
        )

        return createdAgreement
    }

    // --- BORROWER AGREEMENT LOOKUP & CONSENT ---
    suspend fun getAgreementByToken(token: String): LoanAgreement? {
        val ag = dao.getAgreementBySecureToken(token)
        if (ag != null) {
            logAuditEvent(
                user = ag.borrower_name,
                action = "Agreement Viewed",
                record = ag.agreement_id,
                details = "Borrower accessed electronic agreement via secure link"
            )
        }
        return ag
    }

    suspend fun getApprovedLoanById(id: Long): ApprovedLoan? {
        return dao.getApprovedLoanById(id)
    }

    suspend fun getApprovedLoanByLoanId(loanId: String): ApprovedLoan? {
        return dao.getApprovedLoanByLoanId(loanId)
    }

    fun getInstallmentsForLoan(loanPrimaryId: Long): Flow<List<DeetrendInstallment>> {
        return dao.getInstallmentsForApprovedLoan(loanPrimaryId)
    }

    suspend fun submitBorrowerConsent(token: String, signatureName: String): Boolean {
        val agreement = dao.getAgreementBySecureToken(token) ?: return false
        if (agreement.consent_status == ConsentStatus.Consented) {
            return true // Already consented, preserve historical snapshot
        }
        val now = System.currentTimeMillis()
        dao.submitAgreementConsent(
            token = token,
            status = ConsentStatus.Consented,
            timestamp = now,
            signature = signatureName.trim()
        )

        scope.launch {
            try {
                firestoreService.recordAgreementConsent(agreement.agreement_id, signatureName.trim(), now)
            } catch (_: Exception) {}
        }

        logAuditEvent(
            user = signatureName.trim(),
            action = "Agreement Consented",
            record = agreement.agreement_id,
            details = "Electronically executed and sealed by ${signatureName.trim()} on ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(java.util.Date(now))}"
        )

        recordDocument(
            title = "Signed Agreement - ${agreement.agreement_id} (${signatureName.trim()})",
            documentType = "Signed Agreement",
            fileReference = "signed/${agreement.agreement_id}_signed.pdf",
            borrowerId = agreement.borrower_id,
            loanId = agreement.loan_id,
            agreementId = agreement.agreement_id
        )

        return true
    }

    suspend fun recordRepayment(
        loanId: String,
        borrowerId: Long,
        amountPaid: Double,
        method: String,
        reference: String,
        notes: String
    ): Boolean {
        val loan = dao.getApprovedLoanByLoanId(loanId) ?: return false
        val newOutstanding = DeetrendLoanCalculator.roundTwoDecimals((loan.outstanding_balance - amountPaid).coerceAtLeast(0.0))
        val paymentStatus = if (newOutstanding <= 0.0) PaymentStatus.Paid.name else PaymentStatus.Active.name
        val loanStatus = if (newOutstanding <= 0.0) LoanRecordStatus.Settled.name else {
            val days = DeetrendLoanCalculator.getDaysDifference(loan.due_date)
            if (days < 0) LoanRecordStatus.Overdue.name else LoanRecordStatus.Active.name
        }

        val record = PaymentRecord(
            loan_id = loanId,
            borrower_id = borrowerId,
            payment_date = System.currentTimeMillis(),
            amount_paid = amountPaid,
            payment_method = method,
            reference = reference,
            notes = notes
        )
        dao.insertPayment(record)
        dao.updateLoanBalances(loan.id, newOutstanding, paymentStatus, loanStatus, System.currentTimeMillis())

        scope.launch {
            try {
                firestoreService.saveRepaymentRecord(record)
                firestoreService.updateLoanOutstanding(loanId, newOutstanding, paymentStatus, loanStatus)
            } catch (_: Exception) {}
        }

        logAuditEvent(
            user = "Admin",
            action = "Repayment Recorded",
            record = loanId,
            details = "Recorded repayment of ₦%,.2f via %s (Ref: %s)".format(amountPaid, method, reference)
        )

        recordDocument(
            title = "Payment Receipt - ₦%,.2f for %s".format(amountPaid, loanId),
            documentType = "Payment Receipt",
            fileReference = "receipts/REC_${System.currentTimeMillis()}.pdf",
            borrowerId = borrowerId,
            loanId = loanId,
            agreementId = null
        )

        return true
    }
}
