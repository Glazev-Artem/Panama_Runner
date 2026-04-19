package com.glazev.panama_runner.data.repository

import com.glazev.panama_runner.domain.repository.ProgressRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Реализация репозитория прогресса с использованием Firebase Firestore.
 */
class FirebaseProgressRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ProgressRepository {

    private val userDoc get() = auth.currentUser?.uid?.let { 
        firestore.collection("users").document(it) 
    }

    override suspend fun saveHighScore(score: Int) {
        val doc = userDoc ?: return
        val data = mapOf("highScore" to score)
        try {
            doc.set(data, SetOptions.merge()).await()
        } catch (e: Exception) {
            // В продакшене здесь должна быть обработка ошибок или логирование
        }
    }

    override fun getHighScore(): Flow<Int> = callbackFlow {
        val doc = userDoc
        if (doc == null) {
            trySend(0)
            close()
            return@callbackFlow
        }

        val subscription = doc.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            val score = snapshot?.getLong("highScore")?.toInt() ?: 0
            trySend(score)
        }

        awaitClose { subscription.remove() }
    }

    override suspend fun syncWithCloud() {
        // Firestore синхронизируется автоматически, 
        // но здесь можно добавить явную логику разрешения конфликтов, если нужно.
    }
}
