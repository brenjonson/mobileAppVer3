package com.example.newhomepage.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.newhomepage.api.models.User
import com.google.gson.Gson

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val editor = prefs.edit()
    private val gson = Gson()

    companion object {
        const val PREF_NAME = "KKU_LINK_UP_PREFS"
        const val USER_TOKEN = "user_token"
        const val USER_DATA = "user_data"
        const val IS_LOGGED_IN = "is_logged_in"
    }

    // บันทึกข้อมูล token
    fun saveAuthToken(token: String) {
        editor.putString(USER_TOKEN, token)
        editor.apply()
    }

    // ดึงข้อมูล token
    fun getToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    // บันทึกข้อมูลผู้ใช้
    fun saveUser(user: User) {
        val userJson = gson.toJson(user)
        editor.putString(USER_DATA, userJson)
        editor.putBoolean(IS_LOGGED_IN, true)
        editor.apply()
    }

    // ดึงข้อมูลผู้ใช้
    fun getUser(): User? {
        val userJson = prefs.getString(USER_DATA, null) ?: return null
        return try {
            gson.fromJson(userJson, User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ตรวจสอบว่าผู้ใช้เข้าสู่ระบบอยู่หรือไม่
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGGED_IN, false)
    }

    // ออกจากระบบ
    fun logout() {
        editor.clear()
        editor.apply()
    }
}