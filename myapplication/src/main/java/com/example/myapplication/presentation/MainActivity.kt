package com.example.myapplication.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import androidx.core.app.NotificationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.TimeText
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.example.core.domain.model.TaskPriority
import com.example.myapplication.WearViewModel
import com.example.myapplication.presentation.home.HomeScreen
import com.example.myapplication.presentation.login.LoginScreen
import com.example.myapplication.presentation.theme.TodolistTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object WearPaths {
    const val COMPANION_CAPABILITY = "companion_app"
    const val DEEP_LINK_LOGIN = "todolist://login"
    const val NOTIFICATION_CHANNEL_ID = "task_channel"
    const val ONGOING_NOTIFICATION_ID = 1001
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WearViewModel by viewModels()
    private lateinit var capabilityClient: CapabilityClient
    private lateinit var remoteActivityHelper: RemoteActivityHelper
    private lateinit var googleSignInClient: GoogleSignInClient

    private val loginLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Google sign in failed", e)
            Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            installSplashScreen()
        } catch (e: Throwable) {
            android.util.Log.e("MainActivity", "Failed to install splash screen", e)
        }
        super.onCreate(savedInstanceState)

        capabilityClient = Wearable.getCapabilityClient(this)
        remoteActivityHelper = RemoteActivityHelper(this)
        
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(com.example.myapplication.R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        createNotificationChannel()

        setContent {
            WearApp(
                viewModel = viewModel,
                onOpenAppOnPhone = ::openAppOnPhone,
                onGoogleSignIn = ::signInWithGoogle,
                onUpdateOngoingActivity = ::updateOngoingActivity,
            )
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        loginLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val auth = FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Welcome!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun openAppOnPhone() {
        lifecycleScope.launch {
            try {
                val nodes = capabilityClient
                    .getCapability(WearPaths.COMPANION_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                    .await()
                    .nodes
                
                val node = nodes.firstOrNull()
                if (node != null) {
                    remoteActivityHelper.startRemoteActivity(
                        Intent(Intent.ACTION_VIEW)
                            .addCategory(Intent.CATEGORY_BROWSABLE)
                            .setData(Uri.parse(WearPaths.DEEP_LINK_LOGIN)),
                        node.id,
                    ).await()
                    Toast.makeText(this@MainActivity, "Check your phone!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Phone app not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Throwable) {
                Toast.makeText(this@MainActivity, "Could not reach phone", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateOngoingActivity(remainingTasks: Int) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (remainingTasks <= 0) {
            notificationManager.cancel(WearPaths.ONGOING_NOTIFICATION_ID)
            return
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notificationBuilder = NotificationCompat.Builder(this, WearPaths.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(com.example.myapplication.R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus")
            .setContentText("$remainingTasks tasks left today")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        val status = Status.Builder()
            .addTemplate("#count# tasks left")
            .addPart("count", Status.TextPart("$remainingTasks"))
            .build()

        OngoingActivity.Builder(this, WearPaths.ONGOING_NOTIFICATION_ID, notificationBuilder)
            .setTouchIntent(pendingIntent)
            .setStatus(status)
            .build()
            .apply(this)

        notificationManager.notify(WearPaths.ONGOING_NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            WearPaths.NOTIFICATION_CHANNEL_ID,
            "Task Notifications",
            NotificationManager.IMPORTANCE_LOW,
        )
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }
}

@Composable
fun WearApp(
    viewModel: WearViewModel,
    onOpenAppOnPhone: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onUpdateOngoingActivity: (Int) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoggedIn = uiState.isLoggedIn

    val remainingTasks = uiState.totalCount - uiState.completedCount
    LaunchedEffect(remainingTasks) {
        try {
            onUpdateOngoingActivity(remainingTasks)
        } catch (e: Throwable) {}
    }

    val view = LocalView.current

    TodolistTheme {
        if (!isLoggedIn) {
            LoginScreen(
                onContinueOnPhoneClick = onOpenAppOnPhone,
                onGoogleSignInClick = onGoogleSignIn
            )
        } else {
            AppScaffold {
                ScreenScaffold(timeText = { TimeText() }) {
                    HomeScreen(
                        tasks = uiState.tasks,
                        habits = uiState.habits,
                        userName = uiState.userName,
                        greeting = uiState.greeting,
                        completedCount = uiState.completedCount,
                        totalCount = uiState.totalCount,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onToggleTask = { task ->
                            if (!task.done) {
                                view?.let { v ->
                                    if (task.priority == TaskPriority.HIGH) {
                                        v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                        v.postDelayed({
                                            v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                        }, 150L)
                                    } else {
                                        v.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                    }
                                }
                            }
                            viewModel.toggleTask(task)
                        },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onVoiceTask = { viewModel.addVoiceTask(it) },
                    )
                }
            }
        }
    }
}
