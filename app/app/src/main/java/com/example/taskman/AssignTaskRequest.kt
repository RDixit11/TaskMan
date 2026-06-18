package com.example.taskman

data class AssignTaskRequest (
    val token_csrf: String,
    val przypisany_uzytkownik_id: Int?
)