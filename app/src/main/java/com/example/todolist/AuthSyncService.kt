package com.example.todolist

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthSyncService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/request_login") {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                // In a real app, you might send a custom token or rely on the fact that
                // both apps share the same Firestore/Firebase instance.
                // For this implementation, we'll send a "success" message so the watch knows to proceed.
                scope.launch {
                    try {
                        Wearable.getMessageClient(this@AuthSyncService)
                            .sendMessage(messageEvent.sourceNodeId, "/login_response", "success".toByteArray())
                            .await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
