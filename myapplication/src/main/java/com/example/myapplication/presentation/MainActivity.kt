package com.example.myapplication.presentation

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScrollProgress
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material3.*
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.example.myapplication.Task
import com.example.myapplication.TaskPriority
import com.example.myapplication.WearViewModel
import com.example.myapplication.WearUiState
import com.example.myapplication.presentation.theme.TodolistTheme
import com.example.myapplication.presentation.theme.OrangeAccent
import com.example.myapplication.presentation.theme.AMOLEDBlack
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.rotaryinput.rotaryWithScroll
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val viewModel: WearViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            WearApp(viewModel)
        }
    }

    private fun createNotificationChannel() {
        val name = "Task Notifications"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel("task_channel", name, importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    fun startOngoingActivity(context: Context, remainingTasks: Int) {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (remainingTasks == 0) {
            notificationManager.cancel(1001)
            return
        }
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notificationBuilder = NotificationCompat.Builder(context, "task_channel")
            .setSmallIcon(com.example.myapplication.R.drawable.ic_launcher_foreground)
            .setContentTitle("Focus")
            .setContentText("$remainingTasks tasks left today")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        val status = Status.Builder()
            .addTemplate("#count# tasks left")
            .addPart("count", Status.TextPart("$remainingTasks"))
            .build()
        val ongoingActivity = OngoingActivity.Builder(context, 1001, notificationBuilder)
            .setAnimatedIcon(com.example.myapplication.R.drawable.ic_launcher_foreground)
            .setStaticIcon(com.example.myapplication.R.drawable.ic_launcher_foreground)
            .setTouchIntent(pendingIntent)
            .setStatus(status)
            .build()
        ongoingActivity.apply(context)
        notificationManager.notify(1001, notificationBuilder.build())
    }
}

@Composable
fun WearApp(viewModel: WearViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var currentUser by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser) }

    val loginLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val signInTask = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = signInTask.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        currentUser = FirebaseAuth.getInstance().currentUser
                    }
                }
        } catch (e: ApiException) {
            Toast.makeText(context, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.completedCount, uiState.totalCount) {
        val remaining = uiState.totalCount - uiState.completedCount
        (context as? MainActivity)?.startOngoingActivity(context, remaining)
    }

    TodolistTheme {
        if (currentUser == null) {
            LoginScreen(onSignInClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(com.example.myapplication.R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                loginLauncher.launch(googleSignInClient.signInIntent)
            })
        } else {
            val listState = rememberTransformingLazyColumnState()

            // ─── OPTIMIZATION: derivedStateOf avoids extra recompositions on progress ────
            val progress by remember(uiState.completedCount, uiState.totalCount) {
                derivedStateOf {
                    if (uiState.totalCount > 0)
                        uiState.completedCount.toFloat() / uiState.totalCount
                    else 0f
                }
            }

            Scaffold(
                positionIndicator = { 
                    // Use Material 3 ScrollIndicator for better performance
                    ScrollIndicator(state = listState)
                },
                timeText = { TimeText() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AMOLEDBlack)
                ) {
                    FluidBackground(progress = progress)

                    MainContent(
                        tasks = uiState.tasks,
                        habits = uiState.habits,
                        userName = uiState.userName,
                        greeting = uiState.greeting,
                        completedCount = uiState.completedCount,
                        totalCount = uiState.totalCount,
                        isLoading = uiState.isLoading,
                        aiRoast = uiState.aiRoast,
                        isRoasting = uiState.isRoasting,
                        listState = listState,
                        onGenerateRoast = remember { { viewModel.generateRoast() } },
                        onToggleTask = remember {
                            { task ->
                                val activity = context as? Activity
                                if (!task.done && activity != null) {
                                    val view = activity.window.decorView
                                    if (task.priority == TaskPriority.HIGH) {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        view.postDelayed({
                                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        }, 150)
                                    } else {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    }
                                }
                                viewModel.toggleTask(task)
                            }
                        },
                        onDeleteTask = remember { { viewModel.deleteTask(it) } },
                        onVoiceTask = remember { { viewModel.addVoiceTask(it) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    tasks: List<Task>,
    habits: List<com.example.myapplication.HabitProgress>,
    userName: String,
    greeting: String,
    completedCount: Int,
    totalCount: Int,
    isLoading: Boolean,
    aiRoast: String?,
    isRoasting: Boolean,
    listState: TransformingLazyColumnState,
    onGenerateRoast: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onVoiceTask: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val dateHeader = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date())
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            if (spokenText != null) {
                onVoiceTask(spokenText)
                Toast.makeText(context, "Task added!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    TransformingLazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .rotaryScrollable(
                behavior = RotaryScrollableDefaults.behavior(listState),
                focusRequester = focusRequester
            ),
        state = listState,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 40.dp)
    ) {
        // ─── Header ────────────────────────────────────────────────────────
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ─── AI ROAST ──────────────────────────────────────────────────────
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(OrangeAccent.copy(alpha = 0.05f))
                    .border(1.dp, OrangeAccent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "AI ROAST 💀",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (isRoasting) {
                    androidx.wear.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        colors = ProgressIndicatorDefaults.colors(indicatorColor = OrangeAccent),
                        strokeWidth = 2.dp
                    )
                } else if (aiRoast != null) {
                    Text(
                        text = aiRoast,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Refresh",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.clickable { onGenerateRoast() }
                    )
                } else {
                    androidx.wear.compose.material3.Button(
                        onClick = onGenerateRoast,
                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent.copy(alpha = 0.2f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Roast Me", color = OrangeAccent, fontSize = 10.sp)
                    }
                }
            }
        }

        // ─── Voice Add ─────────────────────────────────────────────────────
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // REMOVE RIPPLE FOR FLUIDITY
                    ) {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "What needs to be done?")
                        }
                        voiceLauncher.launch(intent)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(OrangeAccent.copy(alpha = 0.1f))
                        .border(1.dp, OrangeAccent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.wear.compose.material3.Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Add",
                        tint = OrangeAccent,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Add Task",
                    style = MaterialTheme.typography.labelSmall,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ─── Section header ────────────────────────────────────────────────
        item {
            val statsText = remember(completedCount, totalCount) {
                "Done $completedCount/$totalCount"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S FOCUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = statsText,
                    style = MaterialTheme.typography.labelSmall,
                    color = OrangeAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ─── Tasks ─────────────────────────────────────────────────────────
        if (tasks.isEmpty() && !isLoading) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    androidx.wear.compose.material3.Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = "Success",
                        tint = OrangeAccent,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "All Done! 🎉",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            items(tasks, key = { it.remoteId }) { task ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight { measuredHeight, scrollProgress ->
                            (measuredHeight * calculateItemScale(scrollProgress)).toInt()
                        }
                        .graphicsLayer {
                            val scale = calculateItemScale(scrollProgress)
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(0.2f, 1f, (1f - calculateItemDistance(scrollProgress)).coerceIn(0f, 1f))
                        }
                ) {
                    TaskItem(
                        task = task,
                        onToggle = { onToggleTask(task) },
                        onDelete = { onDeleteTask(task) }
                    )
                }
            }
        }

        // ─── Habits ────────────────────────────────────────────────────────
        if (habits.isNotEmpty()) {
            item {
                Text(
                    text = "HABIT TRACKER",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 4.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
            items(habits, key = { it.name }) { habit ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight { measuredHeight, scrollProgress ->
                            (measuredHeight * calculateItemScale(scrollProgress)).toInt()
                        }
                        .graphicsLayer {
                            val scale = calculateItemScale(scrollProgress)
                            scaleX = scale
                            scaleY = scale
                            alpha = lerp(0.2f, 1f, (1f - calculateItemDistance(scrollProgress)).coerceIn(0f, 1f))
                        }
                ) {
                    HabitCard(habit = habit)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private fun calculateItemScale(progress: TransformingLazyColumnItemScrollProgress): Float {
    if (progress == TransformingLazyColumnItemScrollProgress.Unspecified) return 1f
    val distance = calculateItemDistance(progress)
    return lerp(1f, 0.5f, distance.coerceIn(0f, 1f))
}

private fun calculateItemDistance(progress: TransformingLazyColumnItemScrollProgress): Float {
    if (progress == TransformingLazyColumnItemScrollProgress.Unspecified) return 0f
    val itemCenter = (progress.topOffsetFraction + progress.bottomOffsetFraction) / 2f
    return Math.min(1f, Math.abs(itemCenter - 0.5f) * 2.2f)
}

@Composable
fun FluidBackground(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width
        val fillLevel = h * (1f - progress.coerceIn(0f, 1f))

        val path1 = Path().apply {
            moveTo(0f, h)
            lineTo(0f, fillLevel)
            for (x in 0..w.toInt() step 5) {
                lineTo(x.toFloat(), fillLevel + sin(x / 60f + wave1) * 12f)
            }
            lineTo(w, h)
            close()
        }
        val path2 = Path().apply {
            moveTo(0f, h)
            lineTo(0f, fillLevel)
            for (x in 0..w.toInt() step 5) {
                lineTo(x.toFloat(), fillLevel + cos(x / 80f + wave2) * 10f)
            }
            lineTo(w, h)
            close()
        }

        drawPath(path1, color = OrangeAccent.copy(alpha = 0.04f))
        drawPath(path2, color = OrangeAccent.copy(alpha = 0.06f))
    }
}

@Composable
fun LoginScreen(onSignInClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AMOLEDBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Todolist",
                style = MaterialTheme.typography.titleMedium,
                color = OrangeAccent,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.wear.compose.material3.Button(
                onClick = onSignInClick,
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign In", color = Color.Black)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Use same account as phone",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun HabitCard(habit: com.example.myapplication.HabitProgress) {
    // FLATTENED: Use Box instead of Card for max performance
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = habit.streak,
                    style = MaterialTheme.typography.labelSmall,
                    color = OrangeAccent,
                    fontSize = 10.sp
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(34.dp)
            ) {
                val progressText = remember(habit.progress) {
                    "${(habit.progress * 100).toInt()}%"
                }
                Text(
                    text = progressText,
                    fontSize = 8.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    val priorityColor = remember(task.priority) {
        when (task.priority) {
            TaskPriority.HIGH -> Color.Red
            TaskPriority.MEDIUM -> OrangeAccent
            TaskPriority.LOW -> Color.Gray
        }
    }

    // FLATTENED: Use Box instead of Card for max performance
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121212))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // REMOVE RIPPLE FOR FLUIDITY
                onClick = onToggle
            )
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (task.done) OrangeAccent else Color.Gray, CircleShape)
                    .background(if (task.done) OrangeAccent else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (task.done) {
                    androidx.wear.compose.material3.Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (task.done) Color.Gray else Color.White,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )
            }

            androidx.wear.compose.material3.IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                androidx.wear.compose.material3.Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
