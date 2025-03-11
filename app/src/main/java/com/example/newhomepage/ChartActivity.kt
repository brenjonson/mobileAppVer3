package com.example.newhomepage

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newhomepage.api.models.EventResponse
import com.example.newhomepage.api.models.RegistrationResponse
import com.example.newhomepage.repositories.EventRepository
import com.example.newhomepage.repositories.RegistrationRepository
import com.example.newhomepage.utils.SessionManager
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ChartActivity : AppCompatActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var lineChart: LineChart
    private lateinit var chartSpinner: Spinner
    private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var registeredEventsRecyclerView: RecyclerView

    private lateinit var eventRepository: EventRepository
    private lateinit var registrationRepository: RegistrationRepository
    private lateinit var sessionManager: SessionManager

    private var allEvents: List<EventResponse> = emptyList()
    private var userRegistrations: List<RegistrationResponse> = emptyList()
    private val chartTypes = listOf(
        "กิจกรรมตามหมวดหมู่",
        "กิจกรรมตามเดือน",
        "กิจกรรมยอดนิยม",
        "การมีส่วนร่วมของฉัน",
        "แนวโน้มการจัดกิจกรรม",
        "กิจกรรมที่ฉันเข้าร่วม"  // เพิ่มตัวเลือกใหม่
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart)

        // เริ่มต้นตัวแปร repository และ session manager
        eventRepository = EventRepository()
        registrationRepository = RegistrationRepository()
        sessionManager = SessionManager(this)

        // อ้างอิง views
        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)
        lineChart = findViewById(R.id.lineChart)
        chartSpinner = findViewById(R.id.chartTypeSpinner)
        titleTextView = findViewById(R.id.chartTitleTextView)
        descriptionTextView = findViewById(R.id.chartDescriptionTextView)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)
        registeredEventsRecyclerView = findViewById(R.id.registeredEventsRecyclerView)

        // ตั้งค่า RecyclerView
        registeredEventsRecyclerView.layoutManager = LinearLayoutManager(this)

        // ตั้งค่า spinner สำหรับเลือกประเภทกราฟ
        setupChartTypeSpinner()

        // โหลดข้อมูล
        loadEventsData()
        if (sessionManager.isLoggedIn()) {
            loadUserRegistrations()
        }
    }

    private fun setupChartTypeSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, chartTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        chartSpinner.adapter = adapter

        chartSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateChartDisplay(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // ไม่ต้องทำอะไร
            }
        }
    }

    private fun loadEventsData() {
        showLoading(true)
        lifecycleScope.launch {
            try {
                val result = eventRepository.getAllEvents()

                result.onSuccess { events ->
                    allEvents = events
                    updateChartDisplay(chartSpinner.selectedItemPosition)
                    showLoading(false)
                }.onFailure { exception ->
                    Log.e("ChartActivity", "Error loading events: ${exception.message}")
                    showLoading(false)
                }
            } catch (e: Exception) {
                Log.e("ChartActivity", "Exception: ${e.message}")
                showLoading(false)
            }
        }
    }

    private fun loadUserRegistrations() {
        lifecycleScope.launch {
            try {
                val token = sessionManager.getToken() ?: return@launch
                Log.d("ChartActivity", "Loading user registrations with token: $token")

                val result = registrationRepository.getUserRegistrations(token)

                result.onSuccess { registrations ->
                    Log.d("ChartActivity", "Successfully loaded ${registrations.size} registrations")
                    // แสดงรายละเอียดการลงทะเบียนทั้งหมดที่ได้รับ
                    registrations.forEach { registration ->
                        Log.d("ChartActivity", "Registration: eventId=${registration.event_id}, status=${registration.status}")
                    }

                    userRegistrations = registrations

                    // อัพเดทกราฟทันทีหากกำลังแสดงกราฟที่เกี่ยวข้อง
                    if (chartSpinner.selectedItemPosition == 3 || chartSpinner.selectedItemPosition == 5) {
                        updateChartDisplay(chartSpinner.selectedItemPosition)
                    }
                }.onFailure { exception ->
                    Log.e("ChartActivity", "Error loading registrations: ${exception.message}", exception)
                    Toast.makeText(this@ChartActivity, "ไม่สามารถโหลดข้อมูลการลงทะเบียนได้", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ChartActivity", "Exception during loading registrations: ${e.message}", e)
                Toast.makeText(this@ChartActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateChartDisplay(chartTypePosition: Int) {
        if (allEvents.isEmpty()) {
            showEmptyDataMessage()
            return
        }

        // ซ่อนทุกกราฟก่อน
        pieChart.visibility = View.GONE
        barChart.visibility = View.GONE
        lineChart.visibility = View.GONE
        registeredEventsRecyclerView.visibility = View.GONE

        when (chartTypePosition) {
            0 -> showCategoryPieChart()  // กิจกรรมตามหมวดหมู่
            1 -> showMonthlyBarChart()   // กิจกรรมตามเดือน
            2 -> showPopularEventsChart() // กิจกรรมยอดนิยม
            3 -> showUserParticipationChart() // การมีส่วนร่วมของผู้ใช้
            4 -> showEventTrendLineChart() // แนวโน้มการจัดกิจกรรม
            5 -> showUserRegisteredEvents() // กิจกรรมที่ฉันเข้าร่วม
        }
    }

    // แสดง Pie Chart สัดส่วนกิจกรรมตามหมวดหมู่
    private fun showCategoryPieChart() {
        titleTextView.text = "สัดส่วนกิจกรรมตามหมวดหมู่"
        descriptionTextView.text = "แสดงจำนวนกิจกรรมแยกตามหมวดหมู่ต่างๆ"

        // จัดกลุ่มกิจกรรมตามหมวดหมู่
        val categoryCount = allEvents.groupBy { it.category }
            .mapValues { it.value.size }
            .filter { it.key.isNotEmpty() }

        // สร้างข้อมูลสำหรับ Pie Chart
        val entries = categoryCount.map {
            PieEntry(it.value.toFloat(), it.key)
        }

        val dataSet = PieDataSet(entries, "หมวดหมู่กิจกรรม")

        // ตั้งค่าสีให้กับแต่ละส่วน
        val colors = listOf(
            Color.rgb(64, 89, 128), Color.rgb(149, 165, 124),
            Color.rgb(217, 184, 162), Color.rgb(191, 134, 134),
            Color.rgb(179, 48, 80), Color.rgb(193, 37, 82),
            Color.rgb(255, 102, 0), Color.rgb(245, 199, 0)
        )

        dataSet.colors = colors

        // ตั้งค่ารูปแบบการแสดงผล
        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieData.setValueTextSize(14f)
        pieData.setValueTextColor(Color.WHITE)

        pieChart.data = pieData

        // กำหนดคุณสมบัติของ Pie Chart
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.transparentCircleRadius = 61f
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.legend.isEnabled = true

        pieChart.visibility = View.VISIBLE
        pieChart.animateY(1000)
    }

    // แสดง Bar Chart จำนวนกิจกรรมรายเดือน
    private fun showMonthlyBarChart() {
        titleTextView.text = "จำนวนกิจกรรมตามเดือน"
        descriptionTextView.text = "แสดงจำนวนกิจกรรมที่จัดในแต่ละเดือน"

        // จัดกลุ่มกิจกรรมตามเดือน
        val monthlyEventCount = Array(12) { 0 }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val monthNames = resources.getStringArray(R.array.month_names)

        for (event in allEvents) {
            try {
                val date = dateFormat.parse(event.event_date)
                if (date != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    val month = calendar.get(Calendar.MONTH)
                    monthlyEventCount[month]++
                }
            } catch (e: Exception) {
                Log.e("ChartActivity", "Error parsing date: ${e.message}")
            }
        }

        // สร้างข้อมูลสำหรับ Bar Chart
        val entries = monthlyEventCount.mapIndexed { index, count ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        val dataSet = BarDataSet(entries, "จำนวนกิจกรรม")
        dataSet.color = ContextCompat.getColor(this, R.color.primaryColor)
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barChart.data = barData

        // กำหนดคุณสมบัติของ Bar Chart
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.setDrawValueAboveBar(true)
        barChart.setDrawGridBackground(false)
        barChart.setFitBars(true)

        // ตั้งค่าแกน X แสดงชื่อเดือน
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(monthNames)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = 45f

        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisRight.isEnabled = false

        barChart.visibility = View.VISIBLE
        barChart.animateY(1000)
    }

    // แสดง Bar Chart กิจกรรมที่ได้รับความนิยมสูงสุด
    private fun showPopularEventsChart() {
        titleTextView.text = "กิจกรรมยอดนิยม"
        descriptionTextView.text = "แสดงกิจกรรมที่มีคนเข้าร่วมมากที่สุด 5 อันดับแรก"

        // สมมติข้อมูลจำนวนผู้ลงทะเบียนต่อกิจกรรม (ในระบบจริงต้องดึงจาก API)
        val popularEvents = allEvents.take(5)

        // สร้างข้อมูลสำหรับ Bar Chart
        val entries = popularEvents.mapIndexed { index, event ->
            // สมมติจำนวนผู้ลงทะเบียน
            val randomCount = (10..100).random()
            BarEntry(index.toFloat(), randomCount.toFloat())
        }

        val dataSet = BarDataSet(entries, "จำนวนผู้ลงทะเบียน")
        dataSet.color = ContextCompat.getColor(this, R.color.accentColor)
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barChart.data = barData

        // กำหนดคุณสมบัติของ Bar Chart
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = true
        barChart.setDrawValueAboveBar(true)
        barChart.setDrawGridBackground(false)
        barChart.setFitBars(true)

        // ตั้งค่าแกน X แสดงชื่อกิจกรรม
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(popularEvents.map { it.title })
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = 45f

        barChart.axisLeft.setDrawGridLines(true)
        barChart.axisRight.isEnabled = false

        barChart.visibility = View.VISIBLE
        barChart.animateY(1000)
    }

    // แสดง Pie Chart การมีส่วนร่วมของผู้ใช้ตามหมวดหมู่
    private fun showUserParticipationChart() {
        titleTextView.text = "การมีส่วนร่วมของฉันตามหมวดหมู่"
        descriptionTextView.text = "แสดงสัดส่วนการเข้าร่วมกิจกรรมของคุณในแต่ละหมวดหมู่"

        if (!sessionManager.isLoggedIn()) {
            descriptionTextView.text = "กรุณาเข้าสู่ระบบเพื่อดูข้อมูลการมีส่วนร่วมของคุณ"
            pieChart.visibility = View.GONE
            return
        }

        if (userRegistrations.isEmpty()) {
            descriptionTextView.text = "คุณยังไม่ได้เข้าร่วมกิจกรรมใดๆ"
            pieChart.visibility = View.GONE
            return
        }

        // จับคู่ข้อมูลลงทะเบียนกับกิจกรรม
        val eventIds = userRegistrations.map { it.event_id }
        val registeredEvents = allEvents.filter { eventIds.contains(it.id) }

        // จัดกลุ่มตามหมวดหมู่
        val categoryCount = registeredEvents.groupBy { it.category }
            .mapValues { it.value.size }
            .filter { it.key.isNotEmpty() }

        // สร้างข้อมูลสำหรับ Pie Chart
        val entries = categoryCount.map {
            PieEntry(it.value.toFloat(), it.key)
        }

        if (entries.isEmpty()) {
            descriptionTextView.text = "ไม่พบข้อมูลหมวดหมู่กิจกรรมที่คุณเข้าร่วม"
            pieChart.visibility = View.GONE
            return
        }

        val dataSet = PieDataSet(entries, "หมวดหมู่ที่เข้าร่วม")

        // ตั้งค่าสีให้กับแต่ละส่วน
        val colors = listOf(
            Color.rgb(33, 150, 243), Color.rgb(76, 175, 80),
            Color.rgb(255, 193, 7), Color.rgb(255, 87, 34),
            Color.rgb(156, 39, 176)
        )

        dataSet.colors = colors

        // ตั้งค่ารูปแบบการแสดงผล
        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(pieChart))
        pieData.setValueTextSize(14f)
        pieData.setValueTextColor(Color.WHITE)

        pieChart.data = pieData

        // กำหนดคุณสมบัติของ Pie Chart
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.WHITE)
        pieChart.transparentCircleRadius = 61f
        pieChart.setDrawEntryLabels(true)
        pieChart.setEntryLabelTextSize(12f)
        pieChart.setEntryLabelColor(Color.WHITE)
        pieChart.legend.isEnabled = true

        pieChart.visibility = View.VISIBLE
        pieChart.animateY(1000)
    }

    // แสดง Line Chart แนวโน้มการจัดกิจกรรมตามเวลา
    private fun showEventTrendLineChart() {
        titleTextView.text = "แนวโน้มการจัดกิจกรรม"
        descriptionTextView.text = "แสดงจำนวนกิจกรรมที่จัดในแต่ละเดือนของปีนี้"

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val monthlyData = Array(12) { 0 }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val monthNames = resources.getStringArray(R.array.month_names)

        // นับจำนวนกิจกรรมในแต่ละเดือนของปีปัจจุบัน
        for (event in allEvents) {
            try {
                val date = dateFormat.parse(event.event_date)
                if (date != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    val eventYear = calendar.get(Calendar.YEAR)
                    val eventMonth = calendar.get(Calendar.MONTH)

                    if (eventYear == currentYear) {
                        monthlyData[eventMonth]++
                    }
                }
            } catch (e: Exception) {
                Log.e("ChartActivity", "Error parsing date: ${e.message}")
            }
        }

        // สร้างข้อมูลสำหรับ Line Chart
        val entries = monthlyData.mapIndexed { index, count ->
            Entry(index.toFloat(), count.toFloat())
        }

        val dataSet = LineDataSet(entries, "จำนวนกิจกรรมปี $currentYear")
        dataSet.color = Color.rgb(33, 150, 243)
        dataSet.lineWidth = 2f
        dataSet.setCircleColor(Color.rgb(33, 150, 243))
        dataSet.circleRadius = 4f
        dataSet.setDrawCircleHole(true)
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER

        val lineData = LineData(dataSet)
        lineChart.data = lineData

        // กำหนดคุณสมบัติของ Line Chart
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = true
        lineChart.setDrawGridBackground(false)

        // ตั้งค่าแกน X แสดงชื่อเดือน
        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(monthNames)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = 45f

        lineChart.axisLeft.setDrawGridLines(true)
        lineChart.axisRight.isEnabled = false

        lineChart.visibility = View.VISIBLE
        lineChart.animateXY(1000, 1000)
    }

    // แสดงรายการกิจกรรมที่ผู้ใช้เข้าร่วม
    private fun showUserRegisteredEvents() {
        titleTextView.text = "กิจกรรมที่ฉันเข้าร่วม"
        descriptionTextView.text = "รายการกิจกรรมทั้งหมดที่คุณได้ลงทะเบียนเข้าร่วม"

        Log.d("ChartActivity", "Show user registered events called, logged in: ${sessionManager.isLoggedIn()}")

        if (!sessionManager.isLoggedIn()) {
            descriptionTextView.text = "กรุณาเข้าสู่ระบบเพื่อดูกิจกรรมที่คุณเข้าร่วม"
            registeredEventsRecyclerView.visibility = View.GONE
            return
        }

        Log.d("ChartActivity", "User registrations count: ${userRegistrations.size}")

        if (userRegistrations.isEmpty()) {
            descriptionTextView.text = "คุณยังไม่ได้เข้าร่วมกิจกรรมใดๆ"
            registeredEventsRecyclerView.visibility = View.GONE
            return
        }



        // จับคู่ข้อมูลลงทะเบียนกับกิจกรรม
        val eventIds = userRegistrations.map { it.event_id }
        Log.d("ChartActivity", "Event IDs from registrations: $eventIds")
        Log.d("ChartActivity", "All events count: ${allEvents.size}")
        val registeredEvents = allEvents.filter { eventIds.contains(it.id) }
        Log.d("ChartActivity", "Matched registered events: ${registeredEvents.size}")

        if (registeredEvents.isEmpty()) {
            descriptionTextView.text = "ไม่พบข้อมูลกิจกรรมที่คุณเข้าร่วม"
            registeredEventsRecyclerView.visibility = View.GONE
            return
        }

        // สร้าง EventModel จากข้อมูล API
        val eventModels = registeredEvents.map { event ->
            // แปลงรูปแบบวันที่
            val eventDate = try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                val date = inputFormat.parse(event.event_date)
                outputFormat.format(date ?: Date())
            } catch (e: Exception) {
                event.event_date
            }

            // สร้าง EventModel
            EventModel(
                id = event.id,
                eventName = event.title,
                eventDate = eventDate,
                imageResId = getImageResourceForEvent(event.category),
                category = event.category,
                imageUrl = event.image_url
            )
        }

        // ตั้งค่า Adapter
        val adapter = EventAdapter(eventModels.toMutableList()) { event ->
            // เมื่อคลิกที่กิจกรรม
            val intent = Intent(this, EventDetailActivity::class.java)
            intent.putExtra("eventName", event.eventName)
            intent.putExtra("eventDate", event.eventDate)
            intent.putExtra("eventImageResId", event.imageResId)
            intent.putExtra("eventId", event.id)
            intent.putExtra("eventImageUrl", event.imageUrl)
            startActivity(intent)
        }

        registeredEventsRecyclerView.adapter = adapter
        registeredEventsRecyclerView.visibility = View.VISIBLE
    }

    private fun showEmptyDataMessage() {
        titleTextView.text = "ไม่พบข้อมูล"
        descriptionTextView.text = "ไม่สามารถแสดงกราฟได้เนื่องจากไม่มีข้อมูลกิจกรรม"
        pieChart.visibility = View.GONE
        barChart.visibility = View.GONE
        lineChart.visibility = View.GONE
        registeredEventsRecyclerView.visibility = View.GONE
    }

    private fun showLoading(isLoading: Boolean) {
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        pieChart.visibility = View.GONE
        barChart.visibility = View.GONE
        lineChart.visibility = View.GONE
        registeredEventsRecyclerView.visibility = View.GONE
    }

    // Helper function เพื่อเลือกรูปภาพตามหมวดหมู่
    private fun getImageResourceForEvent(category: String): Int {
        return when (category.lowercase()) {
            "กีฬา" -> R.drawable.banner1
            "การศึกษา" -> R.drawable.event2
            "ศิลปะ" -> R.drawable.event3
            "ดนตรี" -> R.drawable.event3
            else -> R.drawable.event1
        }
    }
}