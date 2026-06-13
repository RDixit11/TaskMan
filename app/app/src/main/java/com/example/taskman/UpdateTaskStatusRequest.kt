package com.example.taskman

data class UpdateTaskStatusRequest (
    val token_csrf: String,
    val stan: String
)