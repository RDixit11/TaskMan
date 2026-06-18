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

// LISTY WSPÓŁZDIELONE

    @GET("/api/multi-listy") // Funkcja do pobierania tablic współdzielonych
    suspend fun getSharedBoard(@Header("X-CSRF-Token") token: String): Response<SharedBoardResponse>

    @POST("/api/multi-listy") // Funkcja do tworzenia nowej tablicy współdzielonej
    suspend fun createSharedBoard(@Body createBoardRequest: CreateBoardRequest): Response<Unit>

    @GET("/api/multi-listy/{lista_id}/members") // Funkcja do pobierania członków tablicy współdzielonej
    suspend fun getMembers(@Header("X-CSRF-Token") token: String,@Path("lista_id") boardId: Int): Response<MembersResponse>

    @POST("/api/multi-listy/{lista_id}/members") // Funkcja do dodawania członka do tablicy współdzielonej
    suspend fun addMember(@Path("lista_id") boardId: Int, @Body addMemberRequest: AddMemberRequest): Response<Unit>

    @DELETE("/api/multi-listy/{lista_id}/members/{member_user_id}")
    suspend fun deleteMember(@Header("X-CSRF-Token") token: String,@Path("lista_id") boardId: Int, @Path("member_user_id") memberId: Int): Response<Unit>

    @POST("/api/multi-listy/{lista_id}/zadania") // Funkcja do tworzenia nowego zadania w tablicy współdzielonej
    suspend fun addSharedTask(@Body addTaskRequest: AddTaskRequest, @Path("lista_id") boardId: Int): Response<Unit>

    @GET("/api/multi-listy/{lista_id}/zadania") // Funkcja do pobierania zadań z tablicy współdzielonej
    suspend fun getSharedTasks(@Header("X-CSRF-Token") token: String, @Path("lista_id") boardId: Int): Response<TasksResponse>

    @PUT("/api/multi-zadania/{zadanie_id}") // Funkcja do zmiany nazwy i opisu zadania w tablicy współdzielonej
    suspend fun  updateSharedTask(@Body AddTaskRequest: AddTaskRequest, @Path("zadanie_id") taskId: Int): Response<Unit>

    @DELETE("/api/multi-zadania/{zadanie_id}") // Funkcja do usuwania zadania w tablicy współdzielonej
    suspend fun deleteSharedTask(@Header("X-CSRF-Token") token: String, @Path("zadanie_id") taskId: Int): Response<Unit>

    @PATCH("/api/multi-zadania/{zadanie_id}/stan") // Funkcja do zmiany stanu zadania w tablicy współdzielonej
    suspend fun updateSharedTaskStatus(@Body updateTaskStatusRequest: UpdateTaskStatusRequest, @Path("zadanie_id") taskId: Int): Response<Unit>
}