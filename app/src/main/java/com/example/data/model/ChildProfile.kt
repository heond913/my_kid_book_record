package com.example.data.model

data class ChildProfile(
    val name: String,
    val gender: String, // "BOY", "GIRL"
    val photoUri: String,
    val colorHex: String = "#8B5CF6", // Profile accent color
    val birthDate: String = "" // Birthday or Age/Grade
)
