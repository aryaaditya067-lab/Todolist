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
public final class ToggleTaskUseCase_Factory implements Factory<ToggleTaskUseCase> {
  private final Provider<TaskRepository> repositoryProvider;

  public ToggleTaskUseCase_Factory(Provider<TaskRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public ToggleTaskUseCase get() {
    return newInstance(repositoryProvider.get());
  }

  public static ToggleTaskUseCase_Factory create(Provider<TaskRepository> repositoryProvider) {
    return new ToggleTaskUseCase_Factory(repositoryProvider);
  }

  public static ToggleTaskUseCase newInstance(TaskRepository repository) {
    return new ToggleTaskUseCase(repository);
  }
}
