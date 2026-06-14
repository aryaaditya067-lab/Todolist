package com.example.todolist.wear

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Firestore se aaj ke tasks real-time fetch karta hai.
 * Phone app ke TaskRepository ka wear-side mirror.
 */
class WearTaskRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun getTodayRange(): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        return Pair(start, end)
    }

    private fun getUserCollection() = auth.currentUser?.uid?.let { uid ->
        db.collection("users").document(uid).collection("tasks")
    }

    /**
     * Aaj ke tasks ka real-time Flow — Firestore snapshot listener use karta hai.
     */
    fun getTodayTasksFlow(): Flow<List<WearTask>> = callbackFlow {
        val collection = getUserCollection()
        if (collection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val (start, end) = getTodayRange()

        val listener: ListenerRegistration = collection
            .whereGreaterThanOrEqualTo("date", start)
            .whereLessThanOrEqualTo("date", end)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val tasks = snapshot.documents.mapNotNull { doc ->
                    try {
                        WearTask(
                            id = (doc.getLong("id") ?: 0L).toInt(),
                            firestoreId = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            done = doc.getBoolean("done") ?: false,
                            date = doc.getLong("date") ?: 0L,
                            category = doc.getString("category") ?: "PERSONAL",
                            priority = doc.getString("priority") ?: "MEDIUM",
                            repeatMode = doc.getString("repeatMode") ?: "ONE_TIME"
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.filter { !it.title.isNullOrBlank() }

                trySend(tasks)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Task done toggle — Firestore update
     */
    suspend fun toggleTaskDone(task: WearTask) {
        val collection = getUserCollection() ?: return
        collection.document(task.firestoreId)
            .update("done", !task.done)
            .await()
    }

    /**
     * Task delete — Firestore se remove
     */
    suspend fun deleteTask(task: WearTask) {
        val collection = getUserCollection() ?: return
        collection.document(task.firestoreId)
            .delete()
            .await()
    }

    /**
     * Habit tracker ke liye — last 30 din ke completion data
     * Returns: Map<dayTimestamp, completionRatio 0.0-1.0>
     */
    fun getLast30DaysFlow(): Flow<Map<Long, Float>> = callbackFlow {
        val collection = getUserCollection()
        if (collection == null) {
            trySend(emptyMap())
            close()
            return@callbackFlow
        }

        val thirtyDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val listener: ListenerRegistration = collection
            .whereGreaterThanOrEqualTo("date", thirtyDaysAgo)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    trySend(emptyMap())
                    return@addSnapshotListener
                }

                // Group by day, calculate completion ratio
                val dayMap = mutableMapOf<Long, Pair<Int, Int>>() // total, done

                snapshot.documents.forEach { doc ->
                    val date = doc.getLong("date") ?: return@forEach
                    val isRecurringTemplate = doc.getBoolean("isRecurringTemplate") ?: false
                    if (isRecurringTemplate) return@forEach

                    // Normalize to day start
                    val dayStart = Calendar.getInstance().apply {
                        timeInMillis = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val done = doc.getBoolean("done") ?: false
                    val current = dayMap[dayStart] ?: Pair(0, 0)
                    dayMap[dayStart] = Pair(current.first + 1, current.second + if (done) 1 else 0)
                }

                val ratioMap = dayMap.mapValues { (_, pair) ->
                    if (pair.first == 0) 0f else pair.second.toFloat() / pair.first.toFloat()
                }

                trySend(ratioMap)
            }

        awaitClose { listener.remove() }
    }
}