package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AmortizationInstallment
import com.example.data.model.ApprovedLoan
import com.example.data.model.Borrower
import com.example.data.model.DeetrendInstallment
import com.example.data.model.LoanAgreement
import com.example.data.model.LoanApplication
import com.example.data.model.LoanApplicationEntity
import com.example.data.model.LoanReminder
import com.example.data.model.AuditLog
import com.example.data.model.LendingDocument
import com.example.data.model.PaymentRecord
import com.example.data.model.PaymentTransaction
import com.example.data.model.UserAccount

@Database(
    entities = [
        LoanApplication::class,
        AmortizationInstallment::class,
        PaymentTransaction::class,
        LoanReminder::class,
        // Deetrend Global Enterprise V1 Entities
        Borrower::class,
        LoanApplicationEntity::class,
        ApprovedLoan::class,
        LoanAgreement::class,
        DeetrendInstallment::class,
        PaymentRecord::class,
        UserAccount::class,
        AuditLog::class,
        LendingDocument::class
    ],
    version = 3,
    exportSchema = false
)
abstract class LoanDatabase : RoomDatabase() {

    abstract fun loanDao(): LoanDao
    abstract fun deetrendDao(): DeetrendDao

    companion object {
        @Volatile
        private var INSTANCE: LoanDatabase? = null

        fun getDatabase(context: Context): LoanDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LoanDatabase::class.java,
                    "loan_tracker_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

