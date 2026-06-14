package com.example.todolist

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val userDoc
        get() = auth.currentUser?.uid?.let { firestore.collection("users").document(it) }

    suspend fun syncTask(task: Task): String {
        val docRef = if (task.remoteId.isNotEmpty()) {
            userDoc?.collection("tasks")?.document(task.remoteId)
        } else {
            userDoc?.collection("tasks")?.document()
        }
        
        docRef?.set(task.toMap())?.await()
        return docRef?.id ?: ""
    }

    suspend fun deleteTask(remoteId: String) {
        if (remoteId.isNotEmpty()) {
            userDoc?.collection("tasks")?.document(remoteId)?.delete()?.await()
        }
    }

    suspend fun deleteTasksBatch(remoteIds: List<String>) {
        if (remoteIds.isEmpty()) return
        val batch = firestore.batch()
        remoteIds.forEach { rid ->
            val docRef = userDoc?.collection("tasks")?.document(rid)
            if (docRef != null) {
                batch.delete(docRef)
            }
        }
        batch.commit().await()
    }

    suspend fun fetchTasks(): List<Task> {
        val snapshot = userDoc?.collection("tasks")?.get()?.await()
        return snapshot?.documents?.map { doc ->
            Task.fromMap(doc.id, doc.data ?: emptyMap())
        } ?: emptyList()
    }
    
    suspend fun syncSettings(settings: Map<String, Any>) {
        userDoc?.collection("settings")?.document("preferences")?.set(settings)?.await()
    }

    suspend fun fetchSettings(): Map<String, Any>? {
        val doc = userDoc?.collection("settings")?.document("preferences")?.get()?.await()
        return doc?.data
    }
}
