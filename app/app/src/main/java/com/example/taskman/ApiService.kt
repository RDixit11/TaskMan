package com.example.taskman

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/api/rejestracja") // Funkcja do tworzenia użytkownika
    suspend fun createUser(@Body user: User): Response<Unit>

    @POST("/api/logowanie") // Funkcja do logowania
    suspend fun login(@Body user: User): Response<LoginResponse>
}