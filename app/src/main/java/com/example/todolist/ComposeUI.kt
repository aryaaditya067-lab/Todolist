package com.example.todolist

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

// --- Premium Color Palette ---
val VibrantOrange = Color(0xFFFF6A00)
val DeepBlack = Color(0xFF0A0A0A)
val GlassWhite = Color(0x1AFFFFFF)
val GlassBorder = Color(0x33FFFFFF)
val TextPrimary = Color(0xFFF5F5F5)
val TextSecondary = Color(0x99FFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernTaskScreen(
    viewModel: TaskViewModel,
    onSettingsClick: () -> Unit,
    onUserClick: () -> Unit
) {
    val tasks by viewModel.allTasks.observeAsState(emptyList())
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val todayTasks = tasks.filter { it.date == today }

    Box(modifier = Modifier.fillMaxSize().background(DeepBlack)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            HeaderSection(onSettingsClick, onUserClick)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SummaryCardSection(tasks) }
                
                item {
                    Text(
                        text = "TODAY'S FOCUS",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = TextSecondary.copy(alpha = 0.5f)
                    )
                }

                if (todayTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "All clear for today. Add your next task.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                } else {
                    items(todayTasks) { task ->
                        TaskItem(task, 
                            onToggle = { viewModel.toggleTaskDone(task) },
                            onLongClick = { /* Handle delete? */ }
                        )
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 32.dp, end = 24.dp)
        ) {
            PremiumFAB(onClick = { showSheet = true })
        }

        // Bottom Sheet
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                scrimColor = Color.Black.copy(alpha = 0.7f),
                dragHandle = { PremiumDragHandle() }
            ) {
                GlassmorphicAddTaskSheet(
                    onDismiss = { showSheet = false },
                    onAdd = { title, desc -> 
                        val newTask = Task(
                            title = title,
                            description = desc,
                            date = today
                        )
                        viewModel.insert(newTask)
                    }
                )
            }
        }
    }
}

@Composable
fun HeaderSection(onSettingsClick: () -> Unit, onUserClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = GlassWhite,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            AsyncImage(
                model = "https://ui-avatars.com/api/?name=User&background=random",
                contentDescription = "Profile",
                modifier = Modifier.fillMaxSize().clickable { onUserClick() },
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        ) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greeting = when (hour) {
                in 5..11 -> "Good Morning ☀️"
                in 12..16 -> "Good Afternoon 🌤️"
                in 17..20 -> "Good Evening 🌆"
                else -> "Good Night 🌙"
            }
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            Text(
                text = sdf.format(Date()).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(GlassWhite)
        ) {
            Icon(Icons.Default.Settings, "Settings", tint = TextPrimary, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun SummaryCardSection(tasks: List<Task>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "LIFE PROGRESS",
                color = VibrantOrange,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "12", // Placeholder for actual streak
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = " days consistent",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Minimalist Habit Tracker Placeholder
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(7) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (it < 4) VibrantOrange else GlassWhite)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Finish strong. Consistency is key 🔥",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskItem(task: Task, onToggle: () -> Unit, onLongClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onToggle,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (task.done) GlassWhite.copy(alpha = 0.05f) else GlassWhite,
        border = BorderStroke(1.dp, if (task.done) Color.Transparent else GlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (task.done) VibrantOrange else TextSecondary, CircleShape)
                    .background(if (task.done) VibrantOrange else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (task.done) {
                    // Simple checkmark could be added here
                }
            }
            
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (task.done) TextSecondary else TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.done) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumFAB(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .graphicsLayer {
                shadowElevation = 20.dp.toPx()
                shape = RoundedCornerShape(20.dp)
            }
            .clip(RoundedCornerShape(20.dp))
            .background(VibrantOrange)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, "Add", tint = DeepBlack, modifier = Modifier.size(30.dp))
    }
}

@Composable
fun PremiumDragHandle() {
    Box(
        modifier = Modifier
            .padding(vertical = 12.dp)
            .width(48.dp)
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(TextSecondary.copy(alpha = 0.2f))
    )
}

@Composable
fun GlassmorphicAddTaskSheet(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(DeepBlack.copy(alpha = 0.9f))
            .border(1.dp, GlassBorder, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
    ) {
        Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text(
                "CREATE NEW TASK",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 2.sp),
                color = VibrantOrange
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            PremiumTextField(value = title, onValueChange = { title = it }, label = "Title", placeholder = "Task name")
            Spacer(modifier = Modifier.height(16.dp))
            PremiumTextField(value = desc, onValueChange = { desc = it }, label = "Notes", placeholder = "Details...", modifier = Modifier.height(100.dp))

            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(56.dp)) {
                    Text("CANCEL", color = TextSecondary)
                }
                Button(
                    onClick = { if (title.isNotBlank()) { onAdd(title, desc); onDismiss() } },
                    modifier = Modifier.weight(1.5f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange, contentColor = DeepBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("SAVE TASK", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
fun PremiumTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassWhite),
            placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.5f)) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = VibrantOrange,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
    }
}
