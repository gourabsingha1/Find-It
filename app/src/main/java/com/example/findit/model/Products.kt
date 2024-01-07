package com.example.findit.model

class Products(
    val name: String? = null,
    val price: Double? = null,
    val location: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val productId: String? = null,
    val timestamp: Long? = null,
    val uid: String? = null,
    val searchTags: ArrayList<String>? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)