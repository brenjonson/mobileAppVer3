package com.example.newhomepage.api.models

import java.util.*

// ข้อมูลกิจกรรมที่ได้รับจาก API
data class EventResponse(
    val id: Int,
    val title: String,
    val description: String,
    val image_url: String?,
    val location: String,
    val event_date: String,
    val registration_deadline: String?,
    val capacity: Int?,
    val category: String,
    val organizer_id: Int,
    val created_at: String,
    val updated_at: String
)

// สำหรับรายการกิจกรรมทั้งหมด
data class EventListResponse(
    val events: List<EventResponse>
)