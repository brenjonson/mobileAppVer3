// สร้างไฟล์ app/src/main/java/com/example/newhomepage/api/models/RegistrationResponse.kt
package com.example.newhomepage.api.models

data class RegistrationResponse(
    val id: Int,
    val user_id: Int,
    val event_id: Int,
    val status: String,
    val created_at: String,
    // เพิ่มฟิลด์อื่นๆ ตามที่ API ส่งมา
    val title: String?, // อาจมีข้อมูลกิจกรรมแนบมาด้วย
    val event_date: String?
)