package com.example.shop.data

data class Anime(
    var id: String = "",
    val name: String = "",
    val description: String = "",
    val genres: List<String> = emptyList(),
    val images: List<String> = emptyList()
)
