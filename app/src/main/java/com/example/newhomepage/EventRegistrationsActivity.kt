package com.example.newhomepage

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newhomepage.api.models.EventResponse
import com.example.newhomepage.api.models.RegistrationResponse
import com.example.newhomepage.repositories.EventRepository
import com.example.newhomepage.repositories.RegistrationRepository
import com.example.newhomepage.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class EventRegistrationsActivity : AppCompatActivity() {

    private lateinit var eventNameTextView: TextView
    private lateinit var registrationCountTextView: TextView
    private lateinit var registrationsRecyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var backButton: ImageButton

    private lateinit var sessionManager: SessionManager
    private lateinit var eventRepository: EventRepository
    private lateinit var registrationRepository: RegistrationRepository

    private var eventId: Int = -1
    private var eventName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_registrations)

        // รับ eventId จาก intent
        eventId = intent.getIntExtra("eventId", -1)
        if (eventId == -1) {
            Toast.makeText(this, "ไม่พบข้อมูลกิจกรรม", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // เริ่มต้น repositories และ session manager
        sessionManager = SessionManager(this)
        eventRepository = EventRepository()
        registrationRepository = RegistrationRepository()

        // ตรวจสอบสิทธิ์แอดมิน
        val user = sessionManager.getUser()
        if (user == null || user.role != "admin") {
            Toast.makeText(this, "คุณไม่มีสิทธิ์เข้าถึงหน้านี้", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // อ้างอิง views
        eventNameTextView = findViewById(R.id.eventNameTextView)
        registrationCountTextView = findViewById(R.id.registrationCountTextView)
        registrationsRecyclerView = findViewById(R.id.registrationsRecyclerView)
        emptyTextView = findViewById(R.id.emptyTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        backButton = findViewById(R.id.backButton)

        // ตั้งค่า RecyclerView
        registrationsRecyclerView.layoutManager = LinearLayoutManager(this)
        registrationsRecyclerView.adapter = RegistrationsAdapter(mutableListOf())

        // ตั้งค่าปุ่มกลับ
        backButton.setOnClickListener {
            finish()
        }

        // โหลดข้อมูลกิจกรรมและผู้ลงทะเบียน
        loadEventDetails()
        loadRegistrations()
    }

    private fun loadEventDetails() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val result = eventRepository.getEventById(eventId)

                result.onSuccess { event ->
                    eventName = event.title
                    eventNameTextView.text = "ชื่อกิจกรรม: ${event.title}"
                    showLoading(false)
                }.onFailure { exception ->
                    Toast.makeText(this@EventRegistrationsActivity,
                        "ไม่สามารถโหลดข้อมูลกิจกรรม: ${exception.message}",
                        Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@EventRegistrationsActivity,
                    "เกิดข้อผิดพลาด: ${e.message}",
                    Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun loadRegistrations() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: ""

                // ในส่วนนี้ต้องมีเมธอดใหม่ใน RegistrationRepository เพื่อดึงรายชื่อผู้ลงทะเบียนตาม eventId
                // ตัวอย่างเช่น: registrationRepository.getEventRegistrations(eventId, token)

                // แสดงข้อมูลตัวอย่างสำหรับการพัฒนา
                val sampleRegistrations = listOf(
                    RegistrationResponse(
                        id = 1,
                        user_id = 123,
                        event_id = eventId,
                        status = "registered",
                        created_at = "2023-01-01T12:00:00",
                        title = "ผู้ใช้ 1",
                        event_date = null
                    ),
                    RegistrationResponse(
                        id = 2,
                        user_id = 456,
                        event_id = eventId,
                        status = "registered",
                        created_at = "2023-01-02T14:30:00",
                        title = "ผู้ใช้ 2",
                        event_date = null
                    )
                )

                updateRegistrationsList(sampleRegistrations)
                showLoading(false)

            } catch (e: Exception) {
                Log.e("EventRegistrations", "Error loading registrations: ${e.message}", e)
                Toast.makeText(this@EventRegistrationsActivity,
                    "เกิดข้อผิดพลาด: ${e.message}",
                    Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun updateRegistrationsList(registrations: List<RegistrationResponse>) {
        if (registrations.isEmpty()) {
            registrationsRecyclerView.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
        } else {
            registrationsRecyclerView.visibility = View.VISIBLE
            emptyTextView.visibility = View.GONE

            (registrationsRecyclerView.adapter as RegistrationsAdapter).updateRegistrationsList(registrations)
            registrationCountTextView.text = "จำนวนผู้ลงทะเบียน: ${registrations.size}"
        }
    }

    private fun showLoading(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        if (isLoading) {
            registrationsRecyclerView.visibility = View.GONE
            emptyTextView.visibility = View.GONE
        }
    }

    // Adapter สำหรับ RecyclerView
    inner class RegistrationsAdapter(
        private var registrationsList: MutableList<RegistrationResponse>
    ) : RecyclerView.Adapter<RegistrationsAdapter.RegistrationViewHolder>() {

        inner class RegistrationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val registrationIdTextView: TextView = itemView.findViewById(R.id.registrationIdTextView)
            val usernameTextView: TextView = itemView.findViewById(R.id.usernameTextView)
            val emailTextView: TextView = itemView.findViewById(R.id.emailTextView)
            val registrationDateTextView: TextView = itemView.findViewById(R.id.registrationDateTextView)
            val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RegistrationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_registration, parent, false)
            return RegistrationViewHolder(view)
        }

        override fun onBindViewHolder(holder: RegistrationViewHolder, position: Int) {
            val registration = registrationsList[position]

            holder.registrationIdTextView.text = registration.id.toString()
            holder.usernameTextView.text = registration.title ?: "ไม่ระบุชื่อ"

            // ในกรณีจริงควรมีข้อมูลอีเมลด้วย
            holder.emailTextView.text = "user${registration.user_id}@example.com"

            holder.registrationDateTextView.text = formatDate(registration.created_at)

            // แปลสถานะเป็นภาษาไทย
            val status = when (registration.status) {
                "registered" -> "ลงทะเบียนแล้ว"
                "cancelled" -> "ยกเลิกแล้ว"
                else -> registration.status
            }
            holder.statusTextView.text = status
        }

        override fun getItemCount(): Int = registrationsList.size

        fun updateRegistrationsList(newList: List<RegistrationResponse>) {
            registrationsList.clear()
            registrationsList.addAll(newList)
            notifyDataSetChanged()
        }

        private fun formatDate(dateString: String): String {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val date = inputFormat.parse(dateString)
                return outputFormat.format(date ?: return dateString)
            } catch (e: Exception) {
                return dateString
            }
        }
    }
}