package com.example.core.data.repository

import android.content.Context
import com.example.core.domain.model.Task
import com.example.core.domain.repository.TaskRepository
import com.example.core.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : TaskRepository {

    private fun getEffectiveUid(): String? {
        return auth.currentUser?.uid
    }

    private fun getUserDoc(): com.google.firebase.firestore.DocumentReference? {
        return getEffectiveUid()?.let {
            firestore.collection("users").document(it)
        }
    }

    override fun getTasksFlow(): Flow<Resource<List<Task>>> = callbackFlow {
        trySend(Resource.Loading())
        
        try {
            val userDoc = getUserDoc()
            if (userDoc == null) {
                trySend(Resource.Error("User not logged in"))
                close()
                return@callbackFlow
            }

            val listener = userDoc.collection("tasks")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Resource.Error(error.message ?: "Unknown error"))
                        return@addSnapshotListener
                    }
                    val tasks = snapshot?.documents?.map { doc ->
                        Task.fromMap(doc.id, doc.data ?: emptyMap())
                    } ?: emptyList()
                    trySend(Resource.Success(tasks))
                }

            awaitClose { listener.remove() }
        } catch (e: Throwable) {
            trySend(Resource.Error(e.message ?: "Sync error"))
            close()
        }
    }

    override suspend fun addTask(task: Task): Resource<String> {
        return try {
            val docRef = getUserDoc()?.collection("tasks")?.add(task.toMap())?.await()
            if (docRef != null) {
                Resource.Success(docRef.id)
            } else {
                Resource.Error("Could not create document")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error adding task")
        }
    }

    override suspend fun updateTask(task: Task): Resource<Unit> {
        if (task.remoteId.isBlank()) {
            android.util.Log.e("TaskRepository", "Attempted to update task without remoteId: ${task.title}")
            return Resource.Error("Missing remoteId for update")
        }
        return try {
            getUserDoc()?.collection("tasks")?.document(task.remoteId)?.set(task.toMap())?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error updating task")
        }
    }

    override suspend fun deleteTask(remoteId: String): Resource<Unit> {
        return try {
            getUserDoc()?.collection("tasks")?.document(remoteId)?.delete()?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error deleting task")
        }
    }

    override suspend fun toggleTaskDone(remoteId: String, done: Boolean): Resource<Unit> {
        return try {
            getUserDoc()?.collection("tasks")?.document(remoteId)?.update("done", done)?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error toggling task")
        }
    }
}
