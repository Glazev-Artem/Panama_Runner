package com.glazev.panama_runner.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUserEmail: String?
    val isUserSignedIn: Flow<Boolean>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
}
