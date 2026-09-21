package com.example.data.remote

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Service to handle Firebase Authentication operations for Deetrend Global Enterprise.
 * Provides:
 * - Email & Password Sign In
 * - Administrator Registration / Account Creation
 * - Current User State Checking
 * - Sign Out
 * - Password Reset
 */
class DeetrendFirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "DeetrendFirebaseAuth"
    }

    val currentUser: FirebaseUser?
        get() = try {
            auth.currentUser
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining current Firebase user", e)
            null
        }

    val isUserSignedIn: Boolean
        get() = currentUser != null

    /**
     * Authenticate staff or administrator with email & password.
     */
    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "Firebase Auth sign in successful for: ${user.email}")
                Result.success(user)
            } else {
                Result.failure(Exception("Firebase authentication returned empty user profile."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth sign in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new administrator or staff account.
     */
    suspend fun createAccount(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), password.trim()).await()
            val user = authResult.user
            if (user != null) {
                Log.d(TAG, "Firebase Auth user created: ${user.email}")
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to create Firebase user account."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase Auth user creation failed", e)
            Result.failure(e)
        }
    }

    /**
     * Send password reset email.
     */
    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Log.d(TAG, "Password reset email sent to $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send password reset email", e)
            Result.failure(e)
        }
    }

    /**
     * Sign out active Firebase user.
     */
    fun signOut() {
        try {
            auth.signOut()
            Log.d(TAG, "Signed out from Firebase Auth")
        } catch (e: Exception) {
            Log.e(TAG, "Error during Firebase sign out", e)
        }
    }
}
