package com.example.todolist

import android.content.Context
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearCommunicationManager @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    fun sendAuthStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            scope.launch {
                try {
                    val nodes = nodeClient.connectedNodes.await()
                    nodes.forEach { node ->
                        messageClient.sendMessage(node.id, "/login_response", "success".toByteArray()).await()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
