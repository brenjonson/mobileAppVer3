package com.example.newhomepage.repositories

import android.util.Log
import com.example.newhomepage.api.RetrofitClient
import com.example.newhomepage.api.models.EventRequest
import com.example.newhomepage.api.models.EventResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class EventRepository {
    private val apiService = RetrofitClient.apiService
    private val TAG = "EventRepository"

    /**
     * ดึงข้อมูลกิจกรรมทั้งหมด
     */
    suspend fun getAllEvents(): Result<List<EventResponse>> {
        return try {
            val response = apiService.getAllEvents()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.events)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get events"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ดึงข้อมูลกิจกรรมตาม ID
     */
    suspend fun getEventById(eventId: Int): Result<EventResponse> {
        return try {
            val response = apiService.getEventById(eventId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ค้นหากิจกรรมตามเงื่อนไข
     */
    suspend fun searchEvents(
        title: String? = null,
        category: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<List<EventResponse>> {
        return try {
            val response = apiService.searchEvents(title, category, startDate, endDate)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.events)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Search failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ดึงข้อมูลหมวดหมู่ทั้งหมด
     */
    suspend fun getAllCategories(): Result<List<String>> {
        return try {
            val response = apiService.getAllCategories()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data ?: emptyList())
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get categories"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * สร้างกิจกรรมใหม่
     */
    suspend fun createEvent(eventData: EventRequest, token: String): Result<Int> {
        return try {
            val authToken = "Bearer $token"
            Log.d("EventRepository", "Creating event with data: $eventData")

            val response = apiService.createEvent(eventData, authToken)
            Log.d("EventRepository", "Create event response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                // แสดงข้อมูลการตอบกลับทั้งหมด
                Log.d("EventRepository", "Response body: ${response.body()}")

                val eventId = response.body()!!.data

                // ตรวจสอบค่า ID ที่ได้รับ
                if (eventId == null || eventId <= 0) {
                    Log.e("EventRepository", "Invalid event ID received: $eventId")
                    return Result.failure(Exception("Invalid event ID received: $eventId"))
                }

                Log.d("EventRepository", "Event created with ID: $eventId")
                Result.success(eventId)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to create event"
                Log.e("EventRepository", "Create event error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("EventRepository", "Exception creating event: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * อัพเดทกิจกรรม
     */
    suspend fun updateEvent(eventId: Int, eventData: Map<String, Any>, token: String): Result<Boolean> {
        return try {
            val authToken = "Bearer $token"
            val response = apiService.updateEvent(eventId, eventData, authToken)

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to update event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * ลบกิจกรรม
     */
    suspend fun deleteEvent(eventId: Int, token: String): Result<Boolean> {
        return try {
            val authToken = "Bearer $token"
            val response = apiService.deleteEvent(eventId, authToken)

            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to delete event"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * อัพโหลดรูปภาพกิจกรรม
     */
    suspend fun uploadEventImage(eventId: Int, imageFile: File, token: String): Result<String> {
        return try {
            Log.d(TAG, "Preparing to upload image for event ID: $eventId")
            Log.d(TAG, "Image file: ${imageFile.absolutePath}, size: ${imageFile.length()} bytes")

            // ตรวจสอบว่าไฟล์มีอยู่จริง
            if (!imageFile.exists()) {
                Log.e(TAG, "Image file does not exist")
                return Result.failure(Exception("Image file does not exist"))
            }

            // สร้าง RequestBody จากไฟล์
            val mediaType = "image/*".toMediaTypeOrNull()
            val requestBody = imageFile.asRequestBody(mediaType)

            // สร้าง MultipartBody.Part
            val imagePart = MultipartBody.Part.createFormData("image", imageFile.name, requestBody)
            Log.d(TAG, "Created MultipartBody.Part for file: ${imageFile.name}")

            // เพิ่ม Authorization header
            val authToken = "Bearer $token"

            // เรียก API
            Log.d(TAG, "Calling uploadEventImage API...")
            val response = apiService.uploadEventImage(eventId, imagePart, authToken)
            Log.d(TAG, "Upload response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val imageUrl = response.body()!!.data ?: ""
                Log.d(TAG, "Upload successful. Image URL: $imageUrl")
                Result.success(imageUrl)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to upload image"
                Log.e(TAG, "Upload error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during image upload: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * อัพโหลดรูปภาพกิจกรรม (รับ MultipartBody.Part โดยตรง)
     */
    suspend fun uploadEventImage(eventId: Int, imagePart: MultipartBody.Part, token: String): Result<String> {
        return try {
            Log.d(TAG, "Uploading image using MultipartBody.Part for event ID: $eventId")

            val authToken = "Bearer $token"
            val response = apiService.uploadEventImage(eventId, imagePart, authToken)

            Log.d(TAG, "Upload response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                val imageUrl = response.body()!!.data ?: ""
                Log.d(TAG, "Uploaded image URL: $imageUrl")
                Result.success(imageUrl)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to upload image"
                Log.e(TAG, "Upload error: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}