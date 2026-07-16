package com.example.core.data.repository;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class TaskRepositoryImpl_Factory implements Factory<TaskRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<FirebaseAuth> authProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  public TaskRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<FirebaseAuth> authProvider, Provider<FirebaseFirestore> firestoreProvider) {
    this.contextProvider = contextProvider;
    this.authProvider = authProvider;
    this.firestoreProvider = firestoreProvider;
  }

  @Override
  public TaskRepositoryImpl get() {
    return newInstance(contextProvider.get(), authProvider.get(), firestoreProvider.get());
  }

  public static TaskRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<FirebaseAuth> authProvider, Provider<FirebaseFirestore> firestoreProvider) {
    return new TaskRepositoryImpl_Factory(contextProvider, authProvider, firestoreProvider);
  }

  public static TaskRepositoryImpl newInstance(Context context, FirebaseAuth auth,
      FirebaseFirestore firestore) {
    return new TaskRepositoryImpl(context, auth, firestore);
  }
}
