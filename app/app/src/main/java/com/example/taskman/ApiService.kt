package com.example.taskman

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
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
}