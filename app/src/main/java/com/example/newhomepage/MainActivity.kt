// ในไฟล์ MainActivity.kt
package com.example.newhomepage
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newhomepage.AnnouncementActivity
import com.example.newhomepage.CategoryAdapter
import com.example.newhomepage.CategoryModel
import com.example.newhomepage.EventAdapter
import com.example.newhomepage.EventDetailActivity
import com.example.newhomepage.EventModel
import com.example.newhomepage.R
import com.example.newhomepage.api.models.EventResponse
import com.example.newhomepage.repositories.EventRepository
import com.example.newhomepage.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var eventAdapter: EventAdapter
    private lateinit var eventList: List<EventModel>
    private lateinit var filteredEventList: MutableList<EventModel>
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryList: List<CategoryModel>
    private lateinit var eventRepository: EventRepository
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // เริ่มต้นตัวแปร repository และ session manager
        eventRepository = EventRepository()
        sessionManager = SessionManager(this)

        // ตั้งค่า RecyclerView สำหรับกิจกรรม
        val latestEventsRecyclerView: RecyclerView = findViewById(R.id.latestEventsRecyclerView)
        latestEventsRecyclerView.layoutManager = LinearLayoutManager(this)

        // สร้าง adapter ด้วยรายการว่าง
        filteredEventList = mutableListOf()
        eventAdapter = EventAdapter(filteredEventList) { event ->
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("eventName", event.eventName)
            intent.putExtra("eventDate", event.eventDate)
            intent.putExtra("eventImageResId", event.imageResId)
            intent.putExtra("eventId", event.id)
            startActivity(intent)
        }
        latestEventsRecyclerView.adapter = eventAdapter

        // ตั้งค่าข้อมูลหมวดหมู่กิจกรรม (จะแทนที่ด้วยข้อมูลจาก API ในภายหลัง)
        categoryList = listOf(
            CategoryModel("กีฬา", R.drawable.image1),
            CategoryModel("การศึกษา", R.drawable.image2),
            CategoryModel("ศิลปะ", R.drawable.image3),
            CategoryModel("ดนตรี", R.drawable.image4)
        )

        // ตั้งค่า RecyclerView สำหรับหมวดหมู่
        val categoryRecyclerView: RecyclerView = findViewById(R.id.categoryRecyclerView)
        categoryRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        categoryAdapter = CategoryAdapter(categoryList) { category ->
            filterEventsByCategory(category)
        }
        categoryRecyclerView.adapter = categoryAdapter

        // ตั้งค่าปุ่มเปลี่ยนธีม
        val themeToggleButton: ImageButton = findViewById(R.id.themeToggleButton)
        updateThemeIcon(themeToggleButton)
        themeToggleButton.setOnClickListener {
            toggleTheme(themeToggleButton)
        }

        // ตั้งค่าช่องค้นหา
        val searchBar: EditText = findViewById(R.id.searchBar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterEvents(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // ตั้งค่า Bottom Navigation
        setupBottomNavigation()

        // โหลดข้อมูลจาก API
        loadEventsFromApi()
        loadCategoriesFromApi()
    }

    // โหลดข้อมูลกิจกรรมจาก API
    private fun loadEventsFromApi() {
        lifecycleScope.launch {
            try {
                showLoading(true) // แสดง loading indicator (ต้องสร้างฟังก์ชันนี้)

                val result = eventRepository.getAllEvents()

                result.onSuccess { apiEvents ->
                    // แปลงข้อมูลจาก API เป็น EventModel
                    val events = apiEvents.map { event ->
                        EventModel(
                            id = event.id,
                            eventName = event.title,
                            eventDate = formatDate(event.event_date),
                            imageResId = getImageResourceForEvent(event), // รูปเริ่มต้น
                            category = event.category,
                            imageUrl = event.image_url // เพิ่มการแมพฟิลด์นี้
                        )
                    }

                    eventList = events
                    filteredEventList.clear()
                    filteredEventList.addAll(events)
                    eventAdapter.notifyDataSetChanged()

                    if (events.isEmpty()) {
                        showEmptyState(true) // แสดงข้อความเมื่อไม่มีข้อมูล (ต้องสร้างฟังก์ชันนี้)
                    } else {
                        showEmptyState(false)
                    }

                }.onFailure { exception ->
                    Toast.makeText(this@MainActivity, "ไม่สามารถโหลดข้อมูลกิจกรรมได้: ${exception.message}", Toast.LENGTH_SHORT).show()
                    showEmptyState(true)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                showEmptyState(true)
            } finally {
                showLoading(false)
            }
        }
    }

    // โหลดข้อมูลหมวดหมู่จาก API
    private fun loadCategoriesFromApi() {
        lifecycleScope.launch {
            try {
                val result = eventRepository.getAllCategories()

                result.onSuccess { categories ->
                    // สร้างรายการหมวดหมู่ใหม่จากข้อมูล API
                    val updatedCategories = categories.map { categoryName ->
                        CategoryModel(categoryName, getCategoryImage(categoryName))
                    }

                    // อัพเดท RecyclerView หมวดหมู่
                    categoryList = updatedCategories
                    categoryAdapter = CategoryAdapter(categoryList) { category ->
                        filterEventsByCategory(category)
                    }
                    findViewById<RecyclerView>(R.id.categoryRecyclerView).adapter = categoryAdapter

                }.onFailure { exception ->
                    // ถ้าไม่สามารถโหลดหมวดหมู่ได้ ใช้ข้อมูลจำลองที่มีอยู่แล้ว
                    Toast.makeText(this@MainActivity, "ไม่สามารถโหลดข้อมูลหมวดหมู่ได้", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Helper functions

    // แปลงรูปแบบวันที่
    private fun formatDate(dateString: String): String {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

            val date = inputFormat.parse(dateString)
            return outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            // ถ้าแปลงวันที่ไม่สำเร็จ ให้คืนค่าวันที่เดิม
            return dateString
        }
    }

    // เลือกรูปภาพตามหมวดหมู่
    private fun getImageResourceForEvent(event: EventResponse): Int {
        return when (event.category.toLowerCase()) {
            "กีฬา" -> R.drawable.banner1
            "การศึกษา" -> R.drawable.event2
            "ศิลปะ" -> R.drawable.event3
            "ดนตรี" -> R.drawable.event3
            else -> R.drawable.event1
        }
    }

    // เลือกรูปภาพสำหรับหมวดหมู่
    private fun getCategoryImage(categoryName: String): Int {
        return when (categoryName.toLowerCase()) {
            "กีฬา" -> R.drawable.image1
            "การศึกษา" -> R.drawable.image2
            "ศิลปะ" -> R.drawable.image3
            "ดนตรี" -> R.drawable.image4
            else -> R.drawable.image1
        }
    }

    // แสดง/ซ่อน loading indicator
    private fun showLoading(isLoading: Boolean) {
        // ต้องสร้าง ProgressBar ใน layout และอ้างอิงที่นี่
        // findViewById<ProgressBar>(R.id.progressBar).visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    // แสดง/ซ่อนข้อความเมื่อไม่มีข้อมูล
    private fun showEmptyState(isEmpty: Boolean) {
        // ต้องสร้าง TextView สำหรับแสดงข้อความเมื่อไม่มีข้อมูล
        // findViewById<TextView>(R.id.emptyTextView).visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    // ฟังก์ชันที่มีอยู่แล้ว
    private fun filterEventsByCategory(category: CategoryModel) {
        filteredEventList.clear()
        for (event in eventList) {
            if (event.category == category.categoryName) {
                filteredEventList.add(event)
            }
        }
        eventAdapter.notifyDataSetChanged()
    }

    private fun filterEvents(query: String) {
        filteredEventList.clear()
        if (query.isEmpty()) {
            filteredEventList.addAll(eventList)
        } else {
            for (event in eventList) {
                if (event.eventName.contains(query, ignoreCase = true)) {
                    filteredEventList.add(event)
                }
            }
        }
        eventAdapter.notifyDataSetChanged()
    }

    private fun toggleTheme(themeToggleButton: ImageButton) {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            themeToggleButton.setImageResource(R.drawable.sun1)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            themeToggleButton.setImageResource(R.drawable.moon1)
        }
        recreate()
    }

    private fun updateThemeIcon(themeToggleButton: ImageButton) {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            themeToggleButton.setImageResource(R.drawable.moon1)
        } else {
            themeToggleButton.setImageResource(R.drawable.sun1)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.subscriptions -> {
                    val intent = Intent(this, AnnouncementActivity::class.java)
                    intent.putExtra("eventList", ArrayList(eventList)) // ส่งข้อมูล eventList
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
}