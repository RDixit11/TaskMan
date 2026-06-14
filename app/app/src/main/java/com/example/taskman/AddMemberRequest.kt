package com.example.taskman

data class AddMemberRequest (
    val token_csrf: String,
    val login: String
)