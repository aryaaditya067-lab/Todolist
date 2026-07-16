package com.example.core.domain.usecase;

import com.example.core.domain.repository.TaskRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class UpdateTaskUseCase_Factory implements Factory<UpdateTaskUseCase> {
  private final Provider<TaskRepository> repositoryProvider;

  public UpdateTaskUseCase_Factory(Provider<TaskRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public UpdateTaskUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static UpdateTaskUseCase_Factory create(Provider<TaskRepository> repositoryProvider) {
    return new UpdateTaskUseCase_Factory(repositoryProvider);
  }

  public static UpdateTaskUseCase newInstance(TaskRepository repository) {
    return new UpdateTaskUseCase(repository);
  }
}
