package com.example.taskman

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/api/rejestracja")
    suspend fun createUser(@Body user: User): Response<Unit>
}