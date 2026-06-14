package com.example.taskman

import com.google.gson.annotations.SerializedName

data class MembersResponse (
    @SerializedName("members")
    val members: List<Member>
)

data class Member (
    @SerializedName("login")
    val login: String,
    @SerializedName("uzytkownik_id")
    val id: Int
)