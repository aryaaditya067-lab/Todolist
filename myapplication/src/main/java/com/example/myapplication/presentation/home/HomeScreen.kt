package com.example.myapplication.presentation.home

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.*
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.*
import com.example.myapplication.HabitProgress
import com.example.myapplication.Task
import com.example.myapplication.presentation.components.HabitCard
import com.example.myapplication.presentation.components.ProgressCard
import com.example.myapplication.presentation.components.TaskItem
import com.example.myapplication.presentation.theme.OrangeAccent
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    tasks: List<Task>,
    habits: List<HabitProgress>,
    userName: String,
    greeting: String,
    completedCount: Int,
    totalCount: Int,
    isLoading: Boolean,
    onToggleTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onVoiceTask: (String) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    val progress by remember(completedCount, totalCount) {
        derivedStateOf {
            if (totalCount > 0) completedCount.toFloat() / totalCount
            else 0f
        }
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

    Box(modifier = Modifier.fillMaxSize()) {
        FluidBackground(progress = progress)

        TransformingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .rotaryScrollable(
                    behavior = RotaryScrollableDefaults.behavior(listState),
                    focusRequester = focusRequester
                ),
            state = listState,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 40.dp)
        ) {
            // Header
            item {
                Header(userName = userName, greeting = greeting)
            }

            // Voice Add
            item {
                VoiceAddButton {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "What needs to be done?")
                    }
                    voiceLauncher.launch(intent)
                }
            }

            // Progress Stats
            item {
                ProgressCard(completedCount = completedCount, totalCount = totalCount)
            }

            // Tasks
            if (tasks.isEmpty() && !isLoading) {
                item { EmptyTasksView() }
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
                        TaskItem(task = task, onToggle = { onToggleTask(task) }, onDelete = { onDeleteTask(task) })
                    }
                }
            }

            // Habits
            if (habits.isNotEmpty()) {
                item { SectionHeader("HABIT TRACKER") }
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
        
        ScrollIndicator(state = listState, modifier = Modifier.align(Alignment.CenterEnd))
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun Header(userName: String, greeting: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Text(text = greeting, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = userName, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VoiceAddButton(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(OrangeAccent.copy(alpha = 0.1f)).border(1.dp, OrangeAccent.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Default.Mic, contentDescription = "Voice Add", tint = OrangeAccent, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "Add Task", style = MaterialTheme.typography.labelSmall, color = OrangeAccent, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun EmptyTasksView() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    ) {
        Icon(imageVector = Icons.Default.TaskAlt, contentDescription = "Success", tint = OrangeAccent, modifier = Modifier.size(40.dp))
        Text(text = "All Done! 🎉", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FluidBackground(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "fluid")
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart), label = "wave1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart), label = "wave2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val h = size.height
        val w = size.width
        val fillLevel = h * (1f - progress.coerceIn(0f, 1f))

        val path1 = Path().apply {
            moveTo(0f, h); lineTo(0f, fillLevel)
            for (x in 0..w.toInt() step 5) {
                lineTo(x.toFloat(), fillLevel + sin(x / 60f + wave1) * 12f)
            }
            lineTo(w, h); close()
        }
        val path2 = Path().apply {
            moveTo(0f, h); lineTo(0f, fillLevel)
            for (x in 0..w.toInt() step 5) {
                lineTo(x.toFloat(), fillLevel + cos(x / 80f + wave2) * 10f)
            }
            lineTo(w, h); close()
        }
        drawPath(path1, color = OrangeAccent.copy(alpha = 0.04f))
        drawPath(path2, color = OrangeAccent.copy(alpha = 0.06f))
    }
}

private fun calculateItemScale(progress: TransformingLazyColumnItemScrollProgress): Float {
    if (progress == TransformingLazyColumnItemScrollProgress.Unspecified) return 1f
    return lerp(1f, 0.5f, calculateItemDistance(progress).coerceIn(0f, 1f))
}

private fun calculateItemDistance(progress: TransformingLazyColumnItemScrollProgress): Float {
    if (progress == TransformingLazyColumnItemScrollProgress.Unspecified) return 0f
    val itemCenter = (progress.topOffsetFraction + progress.bottomOffsetFraction) / 2f
    return (kotlin.math.abs(itemCenter - 0.5f) * 2.2f).coerceAtMost(1f)
}
