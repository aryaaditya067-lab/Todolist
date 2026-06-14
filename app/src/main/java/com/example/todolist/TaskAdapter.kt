package com.example.todolist

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.RowTaskBinding

class TaskAdapter(
    private val onTaskToggled: (Task) -> Unit,
    private val onStartTimer: (Task) -> Unit,
    private val onPauseTimer: (Task) -> Unit,
    private val onStopTimer: (Task) -> Unit,
    private val onTaskLongClicked: (Task) -> Unit,
    private val onSelectionChanged: (Int) -> Unit,
    private val onDeleteTask: (Task) -> Unit,
    private val onTaskClicked: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    private val selectedTaskIds = mutableSetOf<Int>()
    var isSelectionMode = false
        private set

    var shakingTaskId: Int? = null
        private set

    val isEditMode: Boolean get() = isSelectionMode || shakingTaskId != null

    private var isTransitioning = false

    fun toggleSelection(taskId: Int) {
        if (selectedTaskIds.contains(taskId)) {
            selectedTaskIds.remove(taskId)
            if (shakingTaskId == taskId) shakingTaskId = null
        } else {
            selectedTaskIds.add(taskId)
            shakingTaskId = taskId
        }
        
        if (selectedTaskIds.isEmpty()) {
            isSelectionMode = false
            shakingTaskId = null
        }
        notifyDataSetChanged()
        onSelectionChanged(selectedTaskIds.size)
    }

    fun selectAll() {
        isSelectionMode = true
        selectedTaskIds.clear()
        currentList.forEach { selectedTaskIds.add(it.id) }
        shakingTaskId = null
        notifyDataSetChanged()
        onSelectionChanged(selectedTaskIds.size)
    }

    fun enterSelectionMode(taskId: Int) {
        isSelectionMode = true
        selectedTaskIds.add(taskId)
        shakingTaskId = taskId
        notifyDataSetChanged()
        onSelectionChanged(selectedTaskIds.size)
    }

    fun enterSelectionModeWithoutTask() {
        isSelectionMode = true
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun startShaking(taskId: Int) {
        shakingTaskId = taskId
        notifyDataSetChanged()
    }

    fun stopShaking() {
        shakingTaskId = null
        notifyDataSetChanged()
    }

    fun exitEditMode() {
        stopShaking()
        clearSelection()
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedTaskIds.clear()
        shakingTaskId = null
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    fun getSelectedTasks(): List<Task> {
        return currentList.filter { selectedTaskIds.contains(it.id) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = RowTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: RowTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: Task) {
            val isSelected = selectedTaskIds.contains(task.id)
            val settingsManager = SettingsManager(binding.root.context)
            val accentColor = settingsManager.accentColor
            val accentColorStateList = ColorStateList.valueOf(accentColor)

            binding.txtTaskTitle.text = task.title
            binding.txtTaskDesc.text = task.description
            binding.txtTaskDesc.visibility = if (task.description.isEmpty()) View.GONE else View.VISIBLE

            if (shakingTaskId == task.id || (isSelectionMode && isSelected)) {
                val shake = AnimationUtils.loadAnimation(binding.root.context, R.anim.shake)
                binding.cardRoot.startAnimation(shake)
                binding.btnDeleteTask.visibility = if (!isSelectionMode) View.VISIBLE else View.GONE
            } else {
                binding.cardRoot.clearAnimation()
                binding.btnDeleteTask.visibility = View.GONE
            }

            if (task.done) {
                binding.imgCheck.visibility = View.GONE
                binding.lottieCheck.visibility = View.VISIBLE
                binding.lottieCheck.progress = 1.0f 
                binding.txtTaskTitle.paintFlags = binding.txtTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.txtTaskTitle.alpha = 0.5f
            } else {
                binding.imgCheck.visibility = View.VISIBLE
                binding.imgCheck.setImageResource(R.drawable.ic_unchecked)
                binding.imgCheck.alpha = 1.0f
                binding.lottieCheck.visibility = View.GONE
                binding.txtTaskTitle.paintFlags = binding.txtTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.txtTaskTitle.alpha = 1.0f
            }

            if (isSelectionMode) {
                binding.root.strokeWidth = if (isSelected) 3 else 1
                binding.root.strokeColor = if (isSelected) accentColor else Color.parseColor("#33FFFFFF")
                binding.root.cardElevation = if (isSelected) 8f else 0f
            } else {
                binding.root.strokeWidth = 1
                binding.root.strokeColor = Color.parseColor("#33FFFFFF")
                binding.root.cardElevation = 0f
            }

            binding.txtCategory.visibility = View.GONE
            binding.viewPriority.backgroundTintList = when (task.priority) {
                TaskPriority.HIGH -> ColorStateList.valueOf(Color.RED)
                TaskPriority.MEDIUM -> accentColorStateList
                TaskPriority.LOW -> ColorStateList.valueOf(Color.GRAY)
            }

            if (task.timerType != TimerType.NONE && !task.done && !isSelectionMode) {
                binding.layoutTimer.visibility = View.VISIBLE
                binding.btnStartTimer.imageTintList = accentColorStateList
                
                when (task.timerStatus) {
                    TimerStatus.NOT_STARTED, TimerStatus.FINISHED -> {
                        binding.btnStartTimer.setImageResource(android.R.drawable.ic_media_play)
                        binding.btnStopTimer.visibility = View.GONE
                    }
                    TimerStatus.RUNNING -> {
                        binding.btnStartTimer.setImageResource(android.R.drawable.ic_media_pause)
                        binding.btnStopTimer.visibility = View.VISIBLE
                    }
                    TimerStatus.PAUSED -> {
                        binding.btnStartTimer.setImageResource(android.R.drawable.ic_media_play)
                        binding.btnStopTimer.visibility = View.VISIBLE
                    }
                }
            } else {
                binding.layoutTimer.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(task.id)
                } else if (shakingTaskId != null) {
                    stopShaking()
                } else {
                    onTaskClicked(task)
                }
            }

            binding.btnDeleteTask.setOnClickListener {
                binding.root.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                binding.cardRoot.clearAnimation()
                binding.root.animate()
                    .alpha(0f)
                    .scaleX(0.7f)
                    .scaleY(0.7f)
                    .setDuration(300)
                    .withEndAction {
                        if (shakingTaskId == task.id) shakingTaskId = null
                        onDeleteTask(task)
                        binding.root.alpha = 1f
                        binding.root.scaleX = 1f
                        binding.root.scaleY = 1f
                    }
                    .start()
            }

            binding.layoutCheck.setOnClickListener { 
                if (isTransitioning) return@setOnClickListener

                if (isSelectionMode) {
                    toggleSelection(task.id)
                } else if (!task.done) {
                    isTransitioning = true
                    binding.imgCheck.visibility = View.INVISIBLE
                    binding.lottieCheck.visibility = View.VISIBLE
                    binding.lottieCheck.playAnimation()
                    binding.root.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    
                    ObjectAnimator.ofFloat(binding.txtTaskTitle, "alpha", 1.0f, 0.5f).setDuration(400).start()
                    binding.txtTaskTitle.paintFlags = binding.txtTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    
                    Handler(Looper.getMainLooper()).postDelayed({
                        onTaskToggled(task)
                        isTransitioning = false
                    }, 700)
                } else {
                    onTaskToggled(task)
                }
            }

            binding.btnStartTimer.setOnClickListener {
                if (task.timerStatus == TimerStatus.RUNNING) onPauseTimer(task) else onStartTimer(task)
            }
            binding.btnStopTimer.setOnClickListener { onStopTimer(task) }
            
            binding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    enterSelectionMode(task.id)
                }
                true
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.title == newItem.title &&
                   oldItem.description == newItem.description &&
                   oldItem.done == newItem.done &&
                   oldItem.timerStatus == newItem.timerStatus &&
                   oldItem.category == newItem.category &&
                   oldItem.priority == newItem.priority &&
                   oldItem.date == newItem.date
        }
    }
}
