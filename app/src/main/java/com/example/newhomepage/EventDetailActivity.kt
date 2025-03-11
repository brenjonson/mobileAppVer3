// แก้ไขไฟล์ app/src/main/java/com/example/newhomepage/EventDetailActivity.kt
package com.example.newhomepage

import LocationImagesAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.newhomepage.repositories.RegistrationRepository
import com.example.newhomepage.utils.SessionManager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class EventDetailActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val delay: Long = 3000
    private lateinit var viewPager2: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: LocationImagesAdapter
    private lateinit var joinEventButton: Button
    private lateinit var sessionManager: SessionManager
    private lateinit var registrationRepository: RegistrationRepository
    private var eventId: Int = -1 // เพิ่มตัวแปรเก็บ ID ของกิจกรรม

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)




        // เริ่มต้นตัวแปร repository และ session manager
        registrationRepository = RegistrationRepository()
        sessionManager = SessionManager(this)



        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        // รับข้อมูลจาก Intent
        val eventName = intent.getStringExtra("eventName")
        val eventDate = intent.getStringExtra("eventDate")
        val eventImageResId = intent.getIntExtra("eventImageResId", R.drawable.moon)
        eventId = intent.getIntExtra("eventId", -1) // รับ eventId จาก Intent

        val eventNameTextView: TextView = findViewById(R.id.eventNameTextView)
        val eventDateTextView: TextView = findViewById(R.id.eventDateTextView)
        val eventImageView: ImageView = findViewById(R.id.eventImageView)
        val eventImageUrl = intent.getStringExtra("eventImageUrl")

        if (eventImageUrl != null && eventImageUrl.isNotEmpty()) {
            val baseUrl = "http://10.0.2.2:3000" // หรือใช้ URL ของเซิร์ฟเวอร์ของคุณ
            val fullImageUrl = baseUrl + eventImageUrl

            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.event1) // รูปพื้นฐานระหว่างโหลด
                .error(intent.getIntExtra("eventImageResId", R.drawable.event1)) // ถ้าโหลดไม่สำเร็จ
                .into(eventImageView)
        } else {
            // ถ้าไม่มี URL รูปภาพ ให้ใช้รูปจากทรัพยากร
            eventImageView.setImageResource(intent.getIntExtra("eventImageResId", R.drawable.event1))
        }

        eventNameTextView.text = eventName ?: "กิจกรรมไม่ระบุ"
        eventDateTextView.text = eventDate ?: "วันที่ไม่ระบุ"
        eventImageView.setImageResource(eventImageResId)

        // เชื่อมต่อปุ่มเข้าร่วมกิจกรรม
        joinEventButton = findViewById(R.id.joinEventButton)
        joinEventButton.setOnClickListener {
            registerForEvent()
        }

        val images = listOf(R.drawable.event4, R.drawable.event5, R.drawable.event6)

        viewPager2 = findViewById(R.id.locationImagesViewPager)
        adapter = LocationImagesAdapter(images)
        viewPager2.adapter = adapter

        tabLayout = findViewById(R.id.tabDots)
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
        }.attach()

        autoScrollImages()
    }

    private fun registerForEvent() {
        // ตรวจสอบว่าผู้ใช้เข้าสู่ระบบแล้วหรือไม่
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "กรุณาเข้าสู่ระบบก่อนเข้าร่วมกิจกรรม", Toast.LENGTH_SHORT).show()
            return
        }

        val token = sessionManager.getToken()
        Log.d("EventDetail", "Token: $token")

        // ตรวจสอบว่ามี event ID หรือไม่
        if (eventId == -1) {
            Toast.makeText(this, "ไม่พบข้อมูลกิจกรรม (ID: $eventId)", Toast.LENGTH_SHORT).show()
            return
        }

        // แสดง Loading ก่อนเรียก API
        joinEventButton.isEnabled = false
        joinEventButton.text = "กำลังลงทะเบียน..."

        // เรียกใช้ API สำหรับการลงทะเบียน
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: ""
                Log.d("EventDetail", "Registering for event ID: $eventId with token: $token")

                val result = registrationRepository.registerForEvent(eventId, token)

                result.onSuccess { registrationId ->
                    Log.d("EventDetail", "Registration success: ID=$registrationId")
                    Toast.makeText(this@EventDetailActivity, "ลงทะเบียนเข้าร่วมกิจกรรมสำเร็จ", Toast.LENGTH_SHORT).show()
                        checkRegistrationStatus()
                    joinEventButton.text = "ยกเลิกการเข้าร่วม"
                    joinEventButton.setOnClickListener {
                        cancelRegistration()
                    }
                }.onFailure { exception ->
                    Log.e("EventDetail", "Registration failed: ${exception.message}", exception)
                    Toast.makeText(this@EventDetailActivity, "เกิดข้อผิดพลาด: เกิดข้อผิดพลาด: คุณได้สมัครเข้าร่วมกิจกรรมนี้ไปแล้ว", Toast.LENGTH_SHORT).show()
                    joinEventButton.text = "เข้าร่วมกิจกรรม"
                }
            } catch (e: Exception) {
                Log.e("EventDetail", "Exception during registration: ${e.message}", e)
                Toast.makeText(this@EventDetailActivity, "เกิดข้อผิดพลาด: เกิดข้อผิดพลาด: คุณได้สมัครเข้าร่วมกิจกรรมนี้ไปแล้ว", Toast.LENGTH_SHORT).show()
            } finally {
                joinEventButton.isEnabled = true
            }
        }
    }

    private fun cancelRegistration() {
        // ตรวจสอบว่ามี event ID หรือไม่
        if (eventId == -1) {
            Toast.makeText(this, "ไม่พบข้อมูลกิจกรรม โปรดลองใหม่อีกครั้ง", Toast.LENGTH_SHORT).show()
            return
        }

        // แสดง Loading ก่อนเรียก API
        joinEventButton.isEnabled = false
        joinEventButton.text = "กำลังยกเลิก..."

        // เรียกใช้ API สำหรับการยกเลิกลงทะเบียน
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: ""
                val result = registrationRepository.cancelRegistration(eventId, token)

                result.onSuccess {
                    Toast.makeText(this@EventDetailActivity, "ยกเลิกการเข้าร่วมกิจกรรมสำเร็จ", Toast.LENGTH_SHORT).show()
                    joinEventButton.text = "เข้าร่วมกิจกรรม"
                    joinEventButton.setOnClickListener {
                        registerForEvent()
                    }
                }.onFailure { exception ->
                    Toast.makeText(this@EventDetailActivity, "เกิดข้อผิดพลาด: คุณได้สมัครเข้าร่วมกิจกรรมนี้ไปแล้ว", Toast.LENGTH_SHORT).show()
                    joinEventButton.text = "ยกเลิกการเข้าร่วม"
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventDetailActivity, "เกิดข้อผิดพลาด: คุณได้สมัครเข้าร่วมกิจกรรมนี้ไปแล้ว", Toast.LENGTH_SHORT).show()
            } finally {
                joinEventButton.isEnabled = true
            }
        }
    }

    // ตรวจสอบสถานะการลงทะเบียนเมื่อเปิดหน้ากิจกรรม
    private fun checkRegistrationStatus() {
        if (!sessionManager.isLoggedIn() || eventId == -1) {
            Log.d("EventDetail", "User not logged in or invalid event ID: $eventId")
            return
        }

        // แสดงสถานะกำลังโหลด
        joinEventButton.isEnabled = false
        joinEventButton.text = "กำลังตรวจสอบ..."

        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: ""
                Log.d("EventDetail", "Checking registration status for event ID: $eventId")
                val registrationsResult = registrationRepository.getUserRegistrations(token)

                registrationsResult.onSuccess { registrations ->
                    Log.d("EventDetail", "Got ${registrations.size} registrations")

                    // ตรวจสอบว่าผู้ใช้ลงทะเบียนกิจกรรมนี้แล้วหรือไม่
                    // ปรับให้ตรงกับรูปแบบข้อมูล RegistrationResponse
                    val isRegistered = registrations.any { registration ->
                        registration.event_id == eventId
                    }

                    Log.d("EventDetail", "Registration status: ${if (isRegistered) "REGISTERED" else "NOT REGISTERED"}")

                    // อัพเดทสถานะปุ่ม
                    runOnUiThread {
                        joinEventButton.isEnabled = true
                        if (isRegistered) {
                            joinEventButton.text = "ยกเลิกการเข้าร่วม"
                            joinEventButton.setOnClickListener {
                                cancelRegistration()
                            }
                        } else {
                            joinEventButton.text = "เข้าร่วมกิจกรรม"
                            joinEventButton.setOnClickListener {
                                registerForEvent()
                            }
                        }
                    }
                }.onFailure { exception ->
                    Log.e("EventDetail", "Failed to get registrations: ${exception.message}")
                    runOnUiThread {
                        joinEventButton.isEnabled = true
                        Toast.makeText(this@EventDetailActivity, "ไม่สามารถตรวจสอบสถานะได้", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("EventDetail", "Error checking registration: ${e.message}", e)
                runOnUiThread {
                    joinEventButton.isEnabled = true
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        Log.d("EventDetail", "onResume called, checking registration status")
        checkRegistrationStatus()
    }

    private fun autoScrollImages() {
        val runnable = object : Runnable {
            override fun run() {
                val currentItem = viewPager2.currentItem
                val nextItem = if (currentItem + 1 < viewPager2.adapter?.itemCount ?: 0) {
                    currentItem + 1
                } else {
                    0 //
                }
                viewPager2.setCurrentItem(nextItem, true)
                handler.postDelayed(this, delay)
            }
        }
        handler.postDelayed(runnable, delay)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}