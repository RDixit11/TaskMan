package com.example.taskman
import com.google.gson.annotations.SerializedName
data class BoardsResponse(
    val listy: List<Board>
)

data class Board(
    @SerializedName("id") val id: Int,
    @SerializedName("nazwa") val name: String
)