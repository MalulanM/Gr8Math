package com.example.gr8math.Adapter

data class Badge(
    val id: Int,
    val listTitle: String,   // Short title for the list (e.g. "First Ace!")
    val dialogTitle: String, // Long title for the dialog (e.g. "First Ace! Badge!")
    val description: String,
    val dateAcquired: String,
    val iconResId: Int,
    val isAcquired: Boolean
)