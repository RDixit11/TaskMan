package com.example.taskman

import com.google.gson.annotations.SerializedName

data class Board(
    @SerializedName("nazwa") val name: String
)