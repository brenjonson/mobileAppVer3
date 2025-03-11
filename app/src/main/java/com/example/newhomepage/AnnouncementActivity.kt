package com.example.newhomepage

import android.content.Intent
import android.os.Bundle
import android.widget.CalendarView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AnnouncementActivity : AppCompatActivity() {

    private lateinit var eventListRecyclerView: RecyclerView
    private lateinit var eventAdapter: EventAdapter
    private lateinit var eventList: MutableList<EventModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_announcement)

        val calendarView = findViewById<CalendarView>(R.id.calendarView)
        eventListRecyclerView = findViewById(R.id.eventListRecyclerView)

        // รับข้อมูลจาก Intent
        val eventListFromMain = intent.getParcelableArrayListExtra<EventModel>("eventList")

        // หากข้อมูลไม่ว่างเปล่าให้ใช้งาน
        eventList = eventListFromMain ?: mutableListOf()

        // ตั้งค่า RecyclerView
        eventAdapter = EventAdapter(eventList) { event ->
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("eventName", event.eventName)
            intent.putExtra("eventDate", event.eventDate)
            intent.putExtra("eventImageResId", event.imageResId)
            startActivity(intent)
        }

        eventListRecyclerView.layoutManager = LinearLayoutManager(this)
        eventListRecyclerView.adapter = eventAdapter

        // การตั้งค่ากับ CalendarView
        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$dayOfMonth-${month + 1}-$year"
            Toast.makeText(this, "Selected Date: $selectedDate", Toast.LENGTH_SHORT).show()
            filterEventsByDate(selectedDate)
        }
    }

    // ฟังก์ชันกรองกิจกรรมตามวันที่เลือก
    private fun filterEventsByDate(date: String) {
        val filteredList = eventList.filter { it.eventDate == date }
        if (filteredList.isEmpty()) {
            Toast.makeText(this, "No events for this date", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "${filteredList.size} events found", Toast.LENGTH_SHORT).show()
        }
        eventAdapter.updateEventList(filteredList.toMutableList()) // อัปเดต RecyclerView
    }
}


