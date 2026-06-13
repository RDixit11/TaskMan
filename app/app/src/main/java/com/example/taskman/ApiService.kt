package com.example.taskman

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("/api/rejestracja") // Funkcja do tworzenia użytkownika
    suspend fun createUser(@Body user: User): Response<Unit>

    @POST("/api/logowanie") // Funkcja do logowania
    suspend fun login(@Body user: User): Response<LoginResponse>

    @POST("/api/listy-zadan") // Funkcja do wysyłania nowo utworzonej tablicy
    suspend fun createBoard(@Body createBoardRequest: CreateBoardRequest): Response<Unit>

    @GET("/api/listy-zadan") // Funkcja do pobierania tablic
    suspend fun getBoards(@Header("X-CSRF-Token") token: String): Response<BoardsResponse>

    @DELETE("/api/listy-zadan/{id}") // Funkcja do usuwania tablicy)
    suspend fun deleteBoard(@Header("X-CSRF-Token") token: String, @Path("id") boardId: Int): Response<Unit>

    @PUT("/api/listy-zadan/{id}") // Funkcja do zmiany nazwy tablicy)
    suspend fun renameBoard(@Body createBoardRequest: CreateBoardRequest, @Path("id") boardId: Int): Response<Unit>

    @POST("/api/listy-zadan/{id}/zadania") // Funkcja do tworzenia nowego zadania)
    suspend fun addTask(@Body addTaskRequest: AddTaskRequest, @Path("id") boardId: Int): Response<Unit>

    @GET("/api/listy-zadan/{id}/zadania") // Funkcja do pobierania zadań
    suspend fun getTasks(@Path("id") boardId: Int, @Header("X-CSRF-Token") token: String): Response<TasksResponse>

    @PATCH("/api/zadania/{id_zadania}/stan") // Funkcja do zmiany stanu zadania
    suspend fun updateTaskStatus(@Body updateTaskStatusRequest: UpdateTaskStatusRequest, @Path("id_zadania") taskId: Int): Response<Unit>

    @DELETE("/api/zadania/{id_zadania}") // Funkcja do usuwania zadania
    suspend fun deleteTask(@Header("X-CSRF-Token") token: String, @Path("id_zadania") taskId: Int): Response<Unit>

    @PUT("/api/zadania/{id_zadania}") // Funkcja do zmiany nazwy i opisu zadania
    suspend fun updateTask(@Body AddTaskRequest: AddTaskRequest, @Path("id_zadania") taskId: Int): Response<Unit>
}