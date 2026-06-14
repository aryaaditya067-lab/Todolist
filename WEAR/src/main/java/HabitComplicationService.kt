package com.example.todolist.wear

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.SmallImageComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Watch face complication — today's progress text show karta hai.
 * e.g. "3/5 ✅" ya "All Done! 🎉"
 */
class HabitComplicationService : ComplicationDataSourceService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        return when (type) {
            ComplicationType.LONG_TEXT -> buildLongText("3/5 Tasks ✅")
            else -> null
        }
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener
    ) {
        scope.launch {
            val text = fetchTodayProgress()
            val data = buildLongText(text)
            listener.onComplicationData(data)
        }
    }

    private suspend fun fetchTodayProgress(): String {
        val uid = auth.currentUser?.uid ?: return "Login first"

        return try {
            val (start, end) = getTodayRange()
            val snapshot = db.collection("users")
                .document(uid)
                .collection("tasks")
                .whereGreaterThanOrEqualTo("date", start)
                .whereLessThanOrEqualTo("date", end)
                .get()
                .await()

            val tasks = snapshot.documents.filter {
                !(it.getBoolean("isRecurringTemplate") ?: false)
            }
            val total = tasks.size
            val done = tasks.count { it.getBoolean("done") == true }

            when {
                total == 0 -> "No tasks 📋"
                done == total -> "All Done! 🎉"
                else -> "$done/$total ✅"
            }
        } catch (e: Exception) {
            "Sync error"
        }
    }

    private fun buildLongText(text: String): ComplicationData {
        return LongTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder("Today's task progress").build()
        ).build()
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val end = start + 86400000L - 1
        return Pair(start, end)
    }
}