package com.example.taskman

data class AddTaskRequest(
    val token_csrf: String,
    val tytul: String,
    val opis: String
)
