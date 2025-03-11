// สร้างไฟล์ใหม่ app/src/main/java/com/example/newhomepage/repositories/RegistrationRepository.kt
package com.example.newhomepage.repositories

import android.util.Log
import com.example.newhomepage.api.RetrofitClient
import com.example.newhomepage.api.ApiResponse
import com.example.newhomepage.api.models.RegistrationResponse

class RegistrationRepository {
    private val apiService = RetrofitClient.apiService

    // ฟังก์ชันสำหรับลงทะเบียนเข้าร่วมกิจกรรม
    suspend fun registerForEvent(eventId: Int, token: String): Result<Int> {
        return try {
            Log.d("RegistrationRepo", "Registering for event ID: $eventId")
            Log.d("RegistrationRepo", "Token: $token")

            val authToken = "Bearer $token"
            val response = apiService.registerForEvent(eventId, authToken)

            // Log response code
            Log.d("RegistrationRepo", "Response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                Log.d("RegistrationRepo", "Registration successful: ${response.body()}")
                Result.success(response.body()!!.data ?: -1)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Registration failed"
                Log.e("RegistrationRepo", "Registration failed: $errorBody")
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e("RegistrationRepo", "Registration exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ฟังก์ชันสำหรับยกเลิกการลงทะเบียน
    suspend fun cancelRegistration(eventId: Int, token: String): Result<Boolean> {
        return try {
            val authToken = "Bearer $token"
            val response = apiService.cancelRegistration(eventId, authToken)

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Cancellation failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ฟังก์ชันสำหรับดึงข้อมูลการลงทะเบียนของผู้ใช้
    suspend fun getUserRegistrations(token: String): Result<List<RegistrationResponse>> {
        return try {
            val authToken = "Bearer $token"
            val response = apiService.getUserRegistrations(authToken)

            if (response.isSuccessful && response.body() != null) {
                val registrations = response.body()!!.data ?: emptyList()
                // แสดงข้อมูลจริงที่ได้รับ
                Log.d("RegistrationRepo", "Raw registrations data: ${response.body()}")
                return Result.success(registrations)
            } else {
                Log.e("RegistrationRepo", "Error: ${response.errorBody()?.string()}")
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get registrations"))
            }
        } catch (e: Exception) {
            Log.e("RegistrationRepo", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}