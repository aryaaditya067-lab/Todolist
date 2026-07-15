package com.example.core.data.repository

import com.example.core.domain.model.Task
import com.example.core.domain.repository.TaskRepository
import com.example.core.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : TaskRepository {

    private fun getUserDoc() = auth.currentUser?.uid?.let {
        firestore.collection("users").document(it)
    }

    override fun getTasksFlow(): Flow<Resource<List<Task>>> = callbackFlow {
        trySend(Resource.Loading())
        
        val userDoc = getUserDoc()
        if (userDoc == null) {
            trySend(Resource.Error("User not logged in"))
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
    }

    override suspend fun addTask(task: Task): Resource<Unit> {
        return try {
            getUserDoc()?.collection("tasks")?.add(task.toMap())?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Error adding task")
        }
    }

    override suspend fun updateTask(task: Task): Resource<Unit> {
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
