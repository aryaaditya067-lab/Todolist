package com.example.myapplication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userDoc
        get() = auth.currentUser?.uid?.let { firestore.collection("users").document(it) }

    suspend fun fetchTasks(): List<Task> {
        val snapshot = userDoc?.collection("tasks")?.get()?.await()
        return snapshot?.documents?.map { doc ->
            Task.fromMap(doc.id, doc.data ?: emptyMap())
        } ?: emptyList()
    }

    suspend fun toggleTaskDone(remoteId: String, done: Boolean) {
        userDoc?.collection("tasks")?.document(remoteId)?.update("done", done)?.await()
    }

    suspend fun deleteTask(remoteId: String) {
        userDoc?.collection("tasks")?.document(remoteId)?.delete()?.await()
    }
}
