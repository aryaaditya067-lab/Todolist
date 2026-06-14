package com.example.todolist.wear

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.todolist.wear.databinding.ActivityWearMainBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WearMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWearMainBinding
    private val viewModel: WearViewModel by viewModels()
    private lateinit var adapter: WearTaskAdapter
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWearMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Auth check
        if (auth.currentUser == null) {
            binding.txtWearStatus.text = "Please login\non phone first"
            binding.progressWear.visibility = View.GONE
            return
        }

        setupRecyclerView()
        setupHabitView()
        observeViewModel()
        updateDateHeader()
        schedulePeriodicRoast()
    }

    private fun schedulePeriodicRoast() {
        val roastWorkRequest = PeriodicWorkRequestBuilder<WearRoastWorker>(4, java.util.concurrent.TimeUnit.HOURS)
            .addTag("periodic_roast")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "periodic_roast",
            ExistingPeriodicWorkPolicy.KEEP,
            roastWorkRequest
        )
    }

    private fun setupRecyclerView() {
        adapter = WearTaskAdapter(
            onToggle = { task ->
                viewModel.toggleTask(task)
                // Haptic feedback
                binding.wearRecyclerView.performHapticFeedback(
                    android.view.HapticFeedbackConstants.CONFIRM
                )
            },
            onDelete = { task ->
                viewModel.deleteTask(task)
                binding.wearRecyclerView.performHapticFeedback(
                    android.view.HapticFeedbackConstants.REJECT
                )
            }
        )

        // WearableRecyclerView — round screen ke liye curved scroll
        binding.wearRecyclerView.apply {
            layoutManager = WearableLinearLayoutManager(this@WearMainActivity)
            adapter = this@WearMainActivity.adapter
            isEdgeItemsCenteringEnabled = true  // Top/bottom items center hote hain round screen pe
        }
    }

    private fun setupHabitView() {
        // Accent color — default orange, phone app se match
        binding.wearHabitDots.setAccentColor(0xFFFF6B00.toInt())
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Tasks observe
                launch {
                    viewModel.tasks.collect { tasks ->
                        adapter.submitList(tasks)
                        binding.progressWear.visibility = View.GONE

                        if (tasks.isEmpty()) {
                            binding.txtWearStatus.visibility = View.VISIBLE
                            binding.txtWearStatus.text = "All done! 🎉"
                            binding.wearRecyclerView.visibility = View.GONE
                        } else {
                            binding.txtWearStatus.visibility = View.GONE
                            binding.wearRecyclerView.visibility = View.VISIBLE
                        }

                        // Summary count update
                        val doneCount = tasks.count { it.done }
                        binding.txtWearSummary.text = "$doneCount/${tasks.size}"
                    }
                }

                // Habit dots observe
                launch {
                    viewModel.habitData.collect { data ->
                        binding.wearHabitDots.setHabitData(data)
                    }
                }

                // Loading state
                launch {
                    viewModel.isLoading.collect { loading ->
                        binding.progressWear.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                // Error messages
                launch {
                    viewModel.errorMessage.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(this@WearMainActivity, msg, Toast.LENGTH_SHORT).show()
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun updateDateHeader() {
        val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        binding.txtWearDate.text = sdf.format(Date())
    }
}