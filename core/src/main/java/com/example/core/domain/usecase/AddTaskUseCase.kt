package com.example.core.domain.usecase

import com.example.core.domain.model.Task
import com.example.core.domain.repository.TaskRepository
import com.example.core.util.Resource
import javax.inject.Inject

class AddTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(task: Task): Resource<Unit> {
        return repository.addTask(task)
    }
}
