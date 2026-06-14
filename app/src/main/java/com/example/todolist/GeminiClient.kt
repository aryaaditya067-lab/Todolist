package com.example.todolist

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GeminiClient {

    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: ApiService = retrofit.create(ApiService::class.java)
}