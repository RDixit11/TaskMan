package com.example.taskman

data class CreateBoardRequest(
    val token_csrf: String,
    val nazwa: String
)