package com.example.newhomepage.repositories

import android.util.Log
import com.example.newhomepage.api.RetrofitClient
import com.example.newhomepage.api.models.LoginRequest
import com.example.newhomepage.api.models.RegisterRequest
import com.example.newhomepage.api.models.User

class AuthRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun registerUser(username: String, email: String, password: String): Result<Int> {
        return try {
            Log.d("AuthRepository", "Creating RegisterRequest: username=$username, email=$email")
            val request = RegisterRequest(username, email, password)
            Log.d("AuthRepository", "Sending register request to API")
            val response = apiService.registerUser(request)

            Log.d("AuthRepository", "Register API response code: ${response.code()}")

            if (response.isSuccessful && response.body() != null) {
                Log.d("AuthRepository", "Registration successful: ${response.body()}")
                Result.success(response.body()?.data ?: -1)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                Log.e("AuthRepository", "Registration failed: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Registration exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<Pair<String, User>> {
        return try {
            val request = LoginRequest(email, password)
            val response = apiService.loginUser(request)

            if (response.isSuccessful && response.body() != null) {
                val token = response.body()!!.token
                val user = response.body()!!.user
                Result.success(Pair(token, user))
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(token: String): Result<User> {
        return try {
            val authToken = "Bearer $token"
            val response = apiService.getUserProfile(authToken)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.data!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to get profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}