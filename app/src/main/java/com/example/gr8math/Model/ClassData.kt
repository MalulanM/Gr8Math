package com.example.gr8math.Model

import com.google.gson.annotations.SerializedName

data class ClassData(
    @SerializedName("adviser_id")
    val adviserId: Int,
    @SerializedName("class_name")
    val className: String,
    @SerializedName("arrival_time")
    val arrivalTime: String,
    @SerializedName("dismissal_time")
    val dismissalTime: String,
    @SerializedName("class_size")
    val classSize: Int
)
