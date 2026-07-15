package com.example.myapplication

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WearAuthSyncService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_AUTH_SUCCESS = "com.example.myapplication.AUTH_SUCCESS"
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WearAuthSync", "Message received: ${messageEvent.path}")
        if (messageEvent.path == "/auth_success") {
            val data = String(messageEvent.data)
            if (data.startsWith("auth_success|")) {
                val phoneUid = data.substringAfter("|")
                
                // 1. Save the phone's UID immediately
                getSharedPreferences("wear_prefs", MODE_PRIVATE)
                    .edit()
                    .putString("phone_uid", phoneUid)
                    .apply()
                
                // 2. Broadcast success so ViewModel can update
                LocalBroadcastManager.getInstance(this)
                    .sendBroadcast(Intent(ACTION_AUTH_SUCCESS))

                scope.launch {
                    try {
                        // 3. Optional: Sign in anonymously on the watch to get Firebase permissions
                        if (FirebaseAuth.getInstance().currentUser == null) {
                            FirebaseAuth.getInstance().signInAnonymously().await()
                        }
                        Toast.makeText(this@WearAuthSyncService, "Sync successful!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Log.e("WearAuthSync", "Optional anon sign-in failed", e)
                    }
                }
            }
        }
    }
}
