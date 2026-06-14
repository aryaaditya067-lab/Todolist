package com.example.todolist.wear

/**
 * Wear OS ke liye lightweight Task model.
 * Phone app ke Room Task se alag — sirf jo watch ko chahiye.
 */
data class WearTask(
    val id: Int = 0,
    val firestoreId: String = "",   // Firestore document ID — delete/update ke liye
    val title: String = "",
    val description: String = "",
    val done: Boolean = false,
    val date: Long = 0L,
    val category: String = "PERSONAL",
    val priority: String = "MEDIUM",
    val repeatMode: String = "ONE_TIME"
)