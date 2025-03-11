package com.example.newhomepage.api.models

data class EventRequest(
    val title: String,
    val description: String,
    val location: String,
    val event_date: String,
    val category: String,
    val registration_deadline: String? = null,
    val capacity: Int? = null,
    val image_url: String? = null
)