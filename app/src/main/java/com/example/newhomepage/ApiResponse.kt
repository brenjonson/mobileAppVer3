package com.example.newhomepage.api

data class ApiResponse<T>(
    val message: String,
    val data: T? = null
)