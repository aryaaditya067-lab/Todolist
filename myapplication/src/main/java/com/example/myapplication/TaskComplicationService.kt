package com.example.myapplication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class TaskComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("5").build(),
            contentDescription = PlainComplicationText.Builder("5 tasks remaining").build()
        ).setTitle(PlainComplicationText.Builder("Tasks").build()).build()
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null

        val count = fetchRemainingTaskCount()
        
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("$count").build(),
            contentDescription = PlainComplicationText.Builder("$count tasks remaining").build()
        ).setTitle(PlainComplicationText.Builder("Tasks").build()).build()
    }

    private suspend fun fetchRemainingTaskCount(): Int {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return 0
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val tomorrow = today + 24 * 60 * 60 * 1000

        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("tasks")
                .whereGreaterThanOrEqualTo("date", today)
                .whereLessThan("date", tomorrow)
                .get().await()

            snapshot.documents.count { doc ->
                val done = doc.getBoolean("done") ?: false
                val isTemplate = doc.getBoolean("isRecurringTemplate") ?: false
                !done && !isTemplate
            }
        } catch (e: Exception) {
            0
        }
    }
}
