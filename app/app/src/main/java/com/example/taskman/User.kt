package com.example.taskman

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class User(
    val login: String,
    val haslo: String
)