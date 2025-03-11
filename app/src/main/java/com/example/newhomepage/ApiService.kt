// แก้ไขไฟล์ app/src/main/java/com/example/newhomepage/api/ApiService.kt
package com.example.newhomepage.api

import com.example.newhomepage.api.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth Endpoints
    @POST("api/auth/register")
    suspend fun registerUser(@Body registerRequest: RegisterRequest): Response<ApiResponse<Int>>

    @POST("api/auth/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @GET("api/auth/profile")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<ApiResponse<User>>

    // Event Endpoints
    @GET("api/events")
    suspend fun getAllEvents(): Response<EventListResponse>

    @GET("api/events/{id}")
    suspend fun getEventById(@Path("id") eventId: Int): Response<ApiResponse<EventResponse>>

    @GET("api/events/search")
    suspend fun searchEvents(
        @Query("title") title: String? = null,
        @Query("category") category: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): Response<EventListResponse>

    @GET("api/events/categories")
    suspend fun getAllCategories(): Response<ApiResponse<List<String>>>

    @POST("api/events")
    suspend fun createEvent(
        @Body eventData: EventRequest,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Int>>

    @PUT("api/events/{id}")
    suspend fun updateEvent(
        @Path("id") eventId: Int,
        @Body eventData: Map<String, Any>,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Boolean>>

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(
        @Path("id") eventId: Int,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Boolean>>

    @Multipart
    @POST("api/events/{id}/image")  // ตรวจสอบว่า URL นี้ถูกต้อง
    suspend fun uploadEventImage(
        @Path("id") eventId: Int,
        @Part image: MultipartBody.Part,
        @Header("Authorization") token: String
    ): Response<ApiResponse<String>>

    // Registration Endpoints
    @POST("api/registrations/events/{eventId}")
    suspend fun registerForEvent(
        @Path("eventId") eventId: Int,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Int>>

    @DELETE("api/registrations/events/{eventId}")
    suspend fun cancelRegistration(
        @Path("eventId") eventId: Int,
        @Header("Authorization") token: String
    ): Response<ApiResponse<Boolean>>

    @GET("api/registrations/user")
    suspend fun getUserRegistrations(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<RegistrationResponse>>>

}