package com.example.newhomepage


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.newhomepage.repositories.AuthRepository
import com.example.newhomepage.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var forgotPasswordText: TextView

    private lateinit var authRepository: AuthRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize repository and session manager
        authRepository = AuthRepository()
        sessionManager = SessionManager(this)


        // ตรวจสอบว่าผู้ใช้เข้าสู่ระบบอยู่หรือไม่
//        if (sessionManager.isLoggedIn()) {
//            navigateToMainActivity()
//            return
//        }

        // ค้นหา view elements
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerLink = findViewById(R.id.registerLink)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)

        // ตั้งค่า listener ให้กับปุ่มเข้าสู่ระบบ
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // ตรวจสอบว่าช่องกรอกข้อมูลไม่ว่างเปล่า
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกข้อมูลให้ครบถ้วน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // เพิ่ม Log เพื่อดีบัก
            Log.d("LoginDebug", "Attempting login with email: $email")

            // เรียกใช้ API สำหรับเข้าสู่ระบบ
            lifecycleScope.launch {
                try {
                    Log.d("LoginDebug", "Calling API for login")
                    val result = authRepository.loginUser(email, password)

                    result.onSuccess { (token, user) ->
                        Log.d("LoginDebug", "Login successful, token: $token")
                        Log.d("LoginDebug", "User: ${user.username}, ID: ${user.id}")

                        try {
                            // บันทึกข้อมูล token และข้อมูลผู้ใช้
                            sessionManager.saveAuthToken(token)
                            sessionManager.saveUser(user)
                            Log.d("LoginDebug", "Session saved")

                            Toast.makeText(this@LoginActivity, "เข้าสู่ระบบสำเร็จ", Toast.LENGTH_SHORT).show()

                            // ใช้เฉพาะฟังก์ชันนี้สำหรับการนำทาง โดยไม่ต้องมีโค้ดนำทางซ้ำซ้อนข้างล่าง
                            navigateToActivity(user.role)

                          /*  // แยกฟังก์ชัน navigateToMainActivity เพื่อการดีบัก
                            try {
                                Log.d("LoginDebug", "Attempting to navigate to MainActivity")
                                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                Log.d("LoginDebug", "Started MainActivity")
                                finish()
                            } catch (e: Exception) {
                                Log.e("LoginDebug", "Error navigating to MainActivity: ${e.message}", e)
                                Toast.makeText(this@LoginActivity, "ไม่สามารถเปิดหน้าหลักได้: ${e.message}", Toast.LENGTH_LONG).show()
                            }*/
                        } catch (e: Exception) {
                            Log.e("LoginDebug", "Error saving session: ${e.message}", e)
                            Toast.makeText(this@LoginActivity, "ไม่สามารถบันทึกข้อมูลผู้ใช้: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { exception ->
                        Log.e("LoginDebug", "Login failed: ${exception.message}", exception)
                        Toast.makeText(this@LoginActivity, "เข้าสู่ระบบไม่สำเร็จ: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("LoginDebug", "Exception during login: ${e.message}", e)
                    Toast.makeText(this@LoginActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ตั้งค่า listener สำหรับลิงก์ลงทะเบียน
        registerLink.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // ตั้งค่า listener สำหรับลิงก์ลืมรหัสผ่าน
        forgotPasswordText.setOnClickListener {
            Toast.makeText(this, "คุณสมบัติ 'ลืมรหัสผ่าน' จะมาเร็วๆ นี้", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun navigateToActivity(role: String) {
        val intent = when(role) {
            "admin" -> Intent(this, AdminDashboardActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}