package com.example.myapplication

import android.widget.Toast
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearAuthSyncService : WearableListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/login_response") {
            val status = String(messageEvent.data)
            if (status == "success") {
                scope.launch {
                    // This service just receives the message. 
                    // Since both apps use the same Firebase project and package name,
                    // the Firebase login state *might* sync if Google Play Services handles it,
                    // but usually we need a token. 
                    // For now, we'll toast to confirm communication is working.
                    Toast.makeText(this@WearAuthSyncService, "Phone confirmed login! Syncing...", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
