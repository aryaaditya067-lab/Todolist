package com.example.core.domain.usecase

import com.example.core.domain.repository.TaskRepository
import com.example.core.util.Resource
import javax.inject.Inject

class DeleteTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(remoteId: String): Resource<Unit> {
        return repository.deleteTask(remoteId)
    }
}
