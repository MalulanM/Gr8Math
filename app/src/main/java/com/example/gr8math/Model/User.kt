package com.example.gr8math.dataObject

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("first_name")
    var firstName: String,

    @SerializedName("last_name")
    var lastName: String,

    @SerializedName("email_add")
    var emailAdd: String,

    @SerializedName("password_hash")
    var passwordHash: String? = null,

    @SerializedName("password_hash_confirmation")
    var passwordHashConfirmation: String? = null,

    @SerializedName("gender")
    var gender: String,

    @SerializedName("birthdate")
    var birthdate: String,

    @SerializedName("learners_ref_number")
    var LRN: String? = null,

    @SerializedName("teaching_position")
    var teacherPosition: String? = null
)

