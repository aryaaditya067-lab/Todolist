package com.example.todolist

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearCommunicationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    fun sendAuthStatus() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) return  // login nahi hai toh kuch mat bhejo
        
        scope.launch {
            // Retry karo 3 baar — watch connect hone mein time lagta hai
            repeat(3) { attempt ->
                try {
                    val nodes = nodeClient.connectedNodes.await()
                    if (nodes.isNotEmpty()) {
                        val payload = "auth_success|${user.uid}"
                        nodes.forEach { node ->
                            messageClient.sendMessage(
                                node.id, 
                                "/auth_success", 
                                payload.toByteArray()
                            ).await()  // await lagao ensure karne ke liye
                        }
                        Log.d("WearComm", "Auth sent successfully on attempt ${attempt + 1}")
                        return@launch  // success, ab retry mat karo
                    }
                } catch (e: Exception) {
                    Log.e("WearComm", "Attempt ${attempt + 1} failed", e)
                }
                if (attempt < 2) delay(2000)  // 2 sec wait karke dobara try karo
            }
        }
    }
}
