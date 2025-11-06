package com.example.gr8math.dataObject

import com.google.gson.annotations.SerializedName

data class LoginUser(
    @SerializedName("email_add")
    val email: String,
    @SerializedName("password_hash")
    val password: String
)