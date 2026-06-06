package com.example.taskman

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("sukces") val sukces: Boolean,
    @SerializedName("wiadomosc") val wiadomosc: String,
    @SerializedName("uzytkownik_id") val uzytkownikId: Int,
    @SerializedName("token_csrf") val tokenCsrf: String,
    @SerializedName("wygasa_za_godzin") val wygasaZaGodzin: Int
)