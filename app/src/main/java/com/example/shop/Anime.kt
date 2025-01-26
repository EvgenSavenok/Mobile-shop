package com.example.shop

data class Anime(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val images: List<String> = emptyList(),
)
