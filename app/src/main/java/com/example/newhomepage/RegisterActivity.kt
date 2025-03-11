package com.example.newhomepage

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.newhomepage.repositories.AuthRepository
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var facultySpinner: Spinner
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize repository
        authRepository = AuthRepository()

        // ค้นหา view elements
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        facultySpinner = findViewById(R.id.facultySpinner)
        registerButton = findViewById(R.id.registerButton)
        loginLink = findViewById(R.id.loginLink)

        // ตั้งค่า Spinner สำหรับการเลือกคณะ
        setupFacultySpinner()

        // ตั้งค่า listener ให้กับปุ่มลงทะเบียน
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val faculty = facultySpinner.selectedItem.toString()

            // ตรวจสอบข้อมูลว่าไม่ว่างเปล่า
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "กรุณากรอกข้อมูลให้ครบถ้วน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ตรวจสอบว่ารหัสผ่านและการยืนยันรหัสผ่านตรงกัน
            if (password != confirmPassword) {
                Toast.makeText(this, "รหัสผ่านและการยืนยันรหัสผ่านไม่ตรงกัน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ตรวจสอบความยาวของรหัสผ่าน
            if (password.length < 6) {
                Toast.makeText(this, "รหัสผ่านต้องมีความยาวอย่างน้อย 6 ตัวอักษร", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ตรวจสอบว่าได้เลือกคณะหรือไม่
            if (faculty == "เลือกคณะ") {
                Toast.makeText(this, "กรุณาเลือกคณะของคุณ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // เรียกใช้ API สำหรับลงทะเบียน
            lifecycleScope.launch {
                try {
                    Log.d("RegisterDebug", "Calling API with username: $username, email: $email")
                    val result = authRepository.registerUser(username, email, password)

                    result.onSuccess { userId ->
                        Log.d("RegisterDebug", "Registration successful: User ID=$userId")
                        Toast.makeText(this@RegisterActivity, "ลงทะเบียนสำเร็จ", Toast.LENGTH_SHORT).show()
                        finish() // กลับไปยังหน้าก่อนหน้า (หน้าเข้าสู่ระบบ)
                    }.onFailure { exception ->
                        Log.e("RegisterDebug", "Registration failed: ${exception.message}", exception)
                        Toast.makeText(this@RegisterActivity, "ลงทะเบียนไม่สำเร็จ: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("RegisterDebug", "Exception during registration: ${e.message}", e)
                    Toast.makeText(this@RegisterActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ตั้งค่า listener สำหรับลิงก์เข้าสู่ระบบ
        loginLink.setOnClickListener {
            finish() // กลับไปยังหน้าเข้าสู่ระบบซึ่งเป็นหน้าก่อนหน้า
        }
    }

    private fun setupFacultySpinner() {
        // สร้างรายการคณะต่างๆ
        val faculties = arrayOf(
            "เลือกคณะ",
            "คณะวิศวกรรมศาสตร์",
            "คณะแพทยศาสตร์",
            "คณะวิทยาศาสตร์",
            "คณะเกษตรศาสตร์",
            "คณะทันตแพทยศาสตร์",
            "คณะเภสัชศาสตร์",
            "คณะเทคนิคการแพทย์",
            "คณะพยาบาลศาสตร์",
            "คณะสาธารณสุขศาสตร์",
            "คณะมนุษยศาสตร์และสังคมศาสตร์",
            "คณะบริหารธุรกิจและการบัญชี",
            "คณะศึกษาศาสตร์",
            "คณะศิลปกรรมศาสตร์",
            "คณะสถาปัตยกรรมศาสตร์",
            "คณะสัตวแพทยศาสตร์",
            "วิทยาลัยการปกครองท้องถิ่น",
            "วิทยาลัยนานาชาติ",
            "คณะนิติศาสตร์",
            "คณะเศรษฐศาสตร์",
            "วิทยาลัยบัณฑิตศึกษาการจัดการ"
        )

        // สร้าง adapter สำหรับ spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, faculties)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        facultySpinner.adapter = adapter
    }
}