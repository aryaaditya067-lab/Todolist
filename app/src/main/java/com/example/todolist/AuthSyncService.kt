package com.example.todolist

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthSyncService : WearableListenerService() {

    @Inject
    lateinit var wearManager: WearCommunicationManager

    override fun onMessageReceived(messageEvent: MessageEvent) {
        // Watch is requesting current auth state
        if (messageEvent.path == "/request_login") {
            wearManager.sendAuthStatus()
        }
    }
}
