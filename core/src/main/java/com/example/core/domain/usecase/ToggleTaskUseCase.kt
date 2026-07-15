package com.example.core.domain.usecase

import com.example.core.domain.repository.TaskRepository
import com.example.core.util.Resource
import javax.inject.Inject

class ToggleTaskUseCase @Inject constructor(
    private val repository: TaskRepository
) {
    suspend operator fun invoke(remoteId: String, done: Boolean): Resource<Unit> {
        return repository.toggleTaskDone(remoteId, done)
    }
}
