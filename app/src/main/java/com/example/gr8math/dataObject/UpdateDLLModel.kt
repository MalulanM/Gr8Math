package com.example.gr8math.dataObject

data class UpdateDllMainRequest(
    val course_id: Int,
    val quarter_number: Int,
    val week_number: Int,
    val available_from: String,
    val available_until: String,
    val content_standard: String,
    val performance_standard: String,
    val learning_comp: String
)

data class UpdateProcedureRequest(
    val id: Int, // procedure id
    val date: String,
    val review: String,
    val purpose: String,
    val example: String,
    val discussion_proper: String,
    val developing_mastery: String,
    val application: String,
    val generalization: String,
    val evaluation: String,
    val additional_act: String
)

data class UpdateReferenceRequest(
    val id: Int, // reference id
    val date: String,
    val reference_title: String,
    val reference_text: String
)

data class UpdateReflectionRequest(
    val id: Int, // reflection id
    val date: String,
    val remark: String,
    val reflection: String
)
