package com.example.taskman

data class TasksResponse (
    val zadania: List<Tasks>
)

data class Tasks (
    val id: Int,
    val tytul: String,
    val opis: String?,
    val stan: String,
)