package com.example.newhomepage

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newhomepage.api.models.EventResponse
import com.example.newhomepage.repositories.EventRepository
import com.example.newhomepage.utils.SessionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ManageEventsActivity : AppCompatActivity() {

    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var eventsAdapter: ManageEventsAdapter
    private lateinit var searchEditText: EditText
    private lateinit var categoryFilterSpinner: Spinner
    private lateinit var statusFilterSpinner: Spinner
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var totalEventsTextView: TextView
    private lateinit var activeEventsTextView: TextView
    private lateinit var totalRegistrationsTextView: TextView
    private lateinit var createNewEventButton: Button

    private lateinit var eventRepository: EventRepository
    private lateinit var sessionManager: SessionManager

    private var allEvents: List<EventResponse> = emptyList()
    private var categories: List<String> = emptyList()
    private val statusOptions = listOf("ทั้งหมด", "กำลังจะมาถึง", "กำลังดำเนินการ", "เสร็จสิ้น")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_events)

        // เริ่มต้นตัวแปร repository และ session manager
        eventRepository = EventRepository()
        sessionManager = SessionManager(this)

        // ตรวจสอบสิทธิ์แอดมิน
        val user = sessionManager.getUser()
        if (user == null || user.role != "admin") {
            Toast.makeText(this, "คุณไม่มีสิทธิ์เข้าถึงหน้านี้", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // อ้างอิง views
        eventsRecyclerView = findViewById(R.id.eventsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        categoryFilterSpinner = findViewById(R.id.categoryFilterSpinner)
        statusFilterSpinner = findViewById(R.id.statusFilterSpinner)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        totalEventsTextView = findViewById(R.id.totalEventsTextView)
        activeEventsTextView = findViewById(R.id.activeEventsTextView)
        totalRegistrationsTextView = findViewById(R.id.totalRegistrationsTextView)
        createNewEventButton = findViewById(R.id.createNewEventButton)

        // ตั้งค่า RecyclerView
        eventsRecyclerView.layoutManager = LinearLayoutManager(this)
        eventsAdapter = ManageEventsAdapter(
            mutableListOf(),
            { event -> onEditEvent(event) },
            { event -> onDeleteEvent(event) },
            { event -> onViewRegistrations(event) }
        )
        eventsRecyclerView.adapter = eventsAdapter

        // ตั้งค่า spinner สถานะ
        setupStatusSpinner()

        // ตั้งค่าการฟังเหตุการณ์
        setupListeners()

        // โหลดข้อมูลกิจกรรมและหมวดหมู่
        loadEvents()
        loadCategories()
    }

    private fun setupStatusSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        statusFilterSpinner.adapter = adapter
    }

    private fun setupCategorySpinner(categories: List<String>) {
        val categoriesWithAll = listOf("ทั้งหมด") + categories
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoriesWithAll)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categoryFilterSpinner.adapter = adapter
    }

    private fun setupListeners() {
        // ฟังก์ชันสำหรับปุ่มสร้างกิจกรรมใหม่
        createNewEventButton.setOnClickListener {
            val intent = Intent(this, CreateEventActivity::class.java)
            startActivity(intent)
        }

        // ฟังก์ชันค้นหา
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                eventsAdapter.filterBySearchQuery(s.toString(), allEvents)
                updateStatistics()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ฟังก์ชันกรองตามหมวดหมู่
        categoryFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = parent?.getItemAtPosition(position).toString()
                filterEventsByCategory(selectedCategory)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ฟังก์ชันกรองตามสถานะ
        statusFilterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = parent?.getItemAtPosition(position).toString()
                filterEventsByStatus(selectedStatus)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadEvents() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val result = eventRepository.getAllEvents()

                result.onSuccess { events ->
                    allEvents = events
                    eventsAdapter.updateEventsList(events)
                    updateStatistics()
                    showLoading(false)
                }.onFailure { exception ->
                    Toast.makeText(this@ManageEventsActivity,
                        "ไม่สามารถโหลดข้อมูลกิจกรรม: ${exception.message}",
                        Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageEventsActivity,
                    "เกิดข้อผิดพลาด: ${e.message}",
                    Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val result = eventRepository.getAllCategories()

                result.onSuccess { categoriesList ->
                    categories = categoriesList
                    setupCategorySpinner(categoriesList)
                }.onFailure { exception ->
                    Toast.makeText(this@ManageEventsActivity,
                        "ไม่สามารถโหลดหมวดหมู่: ${exception.message}",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageEventsActivity,
                    "เกิดข้อผิดพลาด: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterEventsByCategory(category: String) {
        if (category == "ทั้งหมด") {
            eventsAdapter.updateEventsList(allEvents)
        } else {
            val filteredEvents = allEvents.filter { it.category == category }
            eventsAdapter.updateEventsList(filteredEvents)
        }
        updateStatistics()
    }

    private fun filterEventsByStatus(status: String) {
        if (status == "ทั้งหมด") {
            eventsAdapter.updateEventsList(allEvents)
            return
        }

        // ต้องกำหนดวิธีการตรวจสอบสถานะตามวันที่
        val today = System.currentTimeMillis()
        val filteredEvents = when (status) {
            "กำลังจะมาถึง" -> {
                // กิจกรรมที่ยังไม่เริ่ม
                allEvents.filter { event ->
                    try {
                        val eventDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .parse(event.event_date)?.time ?: 0
                        eventDate > today
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            "กำลังดำเนินการ" -> {
                // กิจกรรมที่กำลังดำเนินการอยู่ (วันนี้)
                allEvents.filter { event ->
                    try {
                        val eventDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .parse(event.event_date)?.time ?: 0
                        val dayDiff = (today - eventDate) / (24 * 60 * 60 * 1000)
                        dayDiff == 0L
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            "เสร็จสิ้น" -> {
                // กิจกรรมที่ผ่านไปแล้ว
                allEvents.filter { event ->
                    try {
                        val eventDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            .parse(event.event_date)?.time ?: 0
                        eventDate < today
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            else -> allEvents
        }

        eventsAdapter.updateEventsList(filteredEvents)
        updateStatistics()
    }

    private fun updateStatistics() {
        val currentEvents = (eventsRecyclerView.adapter as ManageEventsAdapter).itemCount
        val totalEvents = allEvents.size

        // จำนวนกิจกรรมที่กำลังดำเนินการ
        val today = System.currentTimeMillis()
        val activeEvents = allEvents.count { event ->
            try {
                val eventDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    .parse(event.event_date)?.time ?: 0
                eventDate >= today
            } catch (e: Exception) {
                false
            }
        }

        totalEventsTextView.text = "กิจกรรมทั้งหมด: $totalEvents"
        activeEventsTextView.text = "กิจกรรมที่ยังดำเนินการ: $activeEvents"
        totalRegistrationsTextView.text = "แสดง: $currentEvents / $totalEvents"
    }

    private fun onEditEvent(event: EventResponse) {
        // ในอนาคตควรสร้างหน้าแก้ไขกิจกรรม
        Toast.makeText(this, "กำลังแก้ไขกิจกรรม: ${event.title}", Toast.LENGTH_SHORT).show()

        // ตัวอย่างการเปิดหน้าแก้ไข (ต้องสร้างหน้านี้เพิ่มเติม)
        // val intent = Intent(this, EditEventActivity::class.java)
        // intent.putExtra("eventId", event.id)
        // startActivity(intent)
    }

    private fun onDeleteEvent(event: EventResponse) {
        AlertDialog.Builder(this)
            .setTitle("ยืนยันการลบ")
            .setMessage("คุณต้องการลบกิจกรรม '${event.title}' ใช่หรือไม่?")
            .setPositiveButton("ใช่") { _, _ ->
                deleteEvent(event.id)
            }
            .setNegativeButton("ไม่") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteEvent(eventId: Int) {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: ""
                val result = eventRepository.deleteEvent(eventId, token)

                result.onSuccess {
                    Toast.makeText(this@ManageEventsActivity,
                        "ลบกิจกรรมสำเร็จ",
                        Toast.LENGTH_SHORT).show()
                    // โหลดข้อมูลใหม่
                    loadEvents()
                }.onFailure { exception ->
                    Toast.makeText(this@ManageEventsActivity,
                        "ไม่สามารถลบกิจกรรมได้: ${exception.message}",
                        Toast.LENGTH_SHORT).show()
                    showLoading(false)
                }
            } catch (e: Exception) {
                Toast.makeText(this@ManageEventsActivity,
                    "เกิดข้อผิดพลาด: ${e.message}",
                    Toast.LENGTH_SHORT).show()
                showLoading(false)
            }
        }
    }

    private fun onViewRegistrations(event: EventResponse) {
        val intent = Intent(this, EventRegistrationsActivity::class.java)
        intent.putExtra("eventId", event.id)
        startActivity(intent)
    }

    private fun showLoading(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        eventsRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        // โหลดข้อมูลใหม่เมื่อกลับมายังหน้านี้ (เผื่อมีการเปลี่ยนแปลงจากหน้าอื่น)
        loadEvents()
    }
}