package com.example.core.domain.usecase

import com.example.core.domain.model.Task
import com.example.core.domain.repository.TaskRepository
import com.example.core.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTasksUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    operator fun invoke(): Flow<Resource<List<Task>>> {
        return repository.getTasksFlow()
    }
}
