package com.example.newhomepage.api.models

// ข้อมูลที่ส่งไปยัง API สำหรับการลงทะเบียน
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val role: String = "student"
)

// ข้อมูลที่ส่งไปยัง API สำหรับการเข้าสู่ระบบ
data class LoginRequest(
    val email: String,
    val password: String
)

// ข้อมูลที่ได้รับจาก API หลังจากเข้าสู่ระบบสำเร็จ
data class LoginResponse(
    val token: String,
    val user: User
)

// ข้อมูลผู้ใช้
data class User(
    val id: Int,
    val username: String,
    val email: String,
    val role: String
)