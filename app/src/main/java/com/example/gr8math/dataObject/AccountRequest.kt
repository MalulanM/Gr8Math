package com.example.gr8math

data class AccountRequest(
    val first_name: String,
    val last_name: String,
    val roles: String,
    val id:Int
)

data class AccountRequestResponse(
    val success: Boolean,
    val data: List<AccountRequest>
)
