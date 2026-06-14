package com.example.taskman

import com.google.gson.annotations.SerializedName

data class SharedBoardResponse (
    val listy: List<SharedBoard>?
)

data class SharedBoard(
    @SerializedName("id") override val id: Int,
    @SerializedName("nazwa") override val name: String
) : RenderableBoard
