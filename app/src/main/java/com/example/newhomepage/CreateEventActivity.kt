package com.example.newhomepage

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.newhomepage.api.RetrofitClient
import com.example.newhomepage.api.models.EventRequest
import com.example.newhomepage.repositories.EventRepository
import com.example.newhomepage.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : AppCompatActivity() {
    private val TAG = "CreateEventActivity"

    private lateinit var titleEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var dateEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var deadlineEditText: EditText
    private lateinit var capacityEditText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var imagePreview: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var createButton: Button


    private lateinit var eventRepository: EventRepository
    private lateinit var sessionManager: SessionManager

    private val calendar = Calendar.getInstance()
    private var selectedImageUri: Uri? = null
    private var categories = arrayOf("กีฬา", "การศึกษา", "ศิลปะ", "ดนตรี", "อื่นๆ")

    // ลงทะเบียน Activity Result สำหรับการเลือกรูปภาพ
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imagePreview.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)


        // เริ่มต้นค่าสำหรับรูปภาพ
        selectedImageUri = null
        imagePreview = findViewById(R.id.eventImagePreview)
        selectImageButton = findViewById(R.id.selectImageButton)

        // ตั้งค่าปุ่มเลือกรูปภาพ
        selectImageButton.setOnClickListener {
            openImagePicker()
        }

        // Initialize views
        titleEditText = findViewById(R.id.eventTitleEditText)
        descriptionEditText = findViewById(R.id.eventDescriptionEditText)
        locationEditText = findViewById(R.id.eventLocationEditText)
        dateEditText = findViewById(R.id.eventDateEditText)
        timeEditText = findViewById(R.id.eventTimeEditText)
        deadlineEditText = findViewById(R.id.deadlineEditText)
        capacityEditText = findViewById(R.id.eventCapacityEditText)
        categorySpinner = findViewById(R.id.categorySpinner)
        imagePreview = findViewById(R.id.eventImagePreview)
        selectImageButton = findViewById(R.id.selectImageButton)
        createButton = findViewById(R.id.createEventButton)

        eventRepository = EventRepository()
        sessionManager = SessionManager(this)

        // Set up category spinner
        setupCategorySpinner()

        // Set up image selection
        selectImageButton.setOnClickListener {
            getContent.launch("image/*")
        }

        // Set up date picker for event date
        dateEditText.setOnClickListener {
            showDatePicker(dateEditText)
        }

        // Set up time picker
        timeEditText.setOnClickListener {
            showTimePicker()
        }

        // Set up date picker for registration deadline
        deadlineEditText.setOnClickListener {
            showDatePicker(deadlineEditText)
        }

        // Set up create button
        createButton.setOnClickListener {
            createEvent()
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun showDatePicker(targetEditText: EditText) {
        val currentCalendar = Calendar.getInstance()

        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)
        val day = currentCalendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)

            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            targetEditText.setText(dateFormat.format(selectedCalendar.time))
        }, year, month, day).show()
    }

    private fun showTimePicker() {
        val currentCalendar = Calendar.getInstance()

        val hour = currentCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = currentCalendar.get(Calendar.MINUTE)

        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            selectedCalendar.set(Calendar.MINUTE, selectedMinute)

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            timeEditText.setText(timeFormat.format(selectedCalendar.time))
        }, hour, minute, true).show()
    }

    private fun openImagePicker() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "เลือกรูปภาพ"), PICK_IMAGE_REQUEST)
    }

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            selectedImageUri = data.data

            try {
                // แสดงตัวอย่างรูปภาพ
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedImageUri)
                imagePreview.setImageBitmap(bitmap)

                // แสดง log
                Log.d("CreateEvent", "Image selected: $selectedImageUri")
            } catch (e: Exception) {
                Log.e("CreateEvent", "Error loading image: ${e.message}", e)
                Toast.makeText(this, "ไม่สามารถโหลดรูปภาพได้", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun createEvent() {
        // บันทึก log เพื่อตรวจสอบว่าฟังก์ชันนี้ถูกเรียก
        Log.d(TAG, "createEvent() called")

        createButton.isEnabled = false
        createButton.text = "กำลังสร้างกิจกรรม..."

        val title = titleEditText.text.toString()
        val description = descriptionEditText.text.toString()
        val location = locationEditText.text.toString()
        val date = dateEditText.text.toString()
        val time = timeEditText.text.toString()
        val category = if (categorySpinner.selectedItemPosition >= 0) categories[categorySpinner.selectedItemPosition] else ""
        val deadline = deadlineEditText.text.toString()
        val capacityText = capacityEditText.text.toString()

        // แสดง log ข้อมูลที่รับมา
        Log.d(TAG, "Form data: title=$title, description=$description, location=$location")
        Log.d(TAG, "Form data: date=$date, time=$time, category=$category")

        // ตรวจสอบข้อมูลที่จำเป็น
        if (title.isEmpty() || description.isEmpty() || location.isEmpty() || date.isEmpty() || time.isEmpty()) {
            Toast.makeText(this, "กรุณากรอกข้อมูลที่จำเป็นให้ครบถ้วน", Toast.LENGTH_SHORT).show()
            return
        }

        // แปลงรูปแบบวันที่
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dbDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

        try {
            // แปลงวันที่และเวลาให้อยู่ในรูปแบบที่ถูกต้อง
            val dateParsed = dateFormat.parse(date)
            val timeParsed = timeFormat.parse(time)

            if (dateParsed != null && timeParsed != null) {
                val eventDateCalendar = Calendar.getInstance()
                eventDateCalendar.time = dateParsed

                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = timeParsed

                eventDateCalendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                eventDateCalendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))

                val eventDateString = dbDateFormat.format(eventDateCalendar.time)
                Log.d(TAG, "Formatted event date: $eventDateString")

                // แปลงวันสุดท้ายของการลงทะเบียน (ถ้ามี)
                var deadlineString: String? = null
                if (deadline.isNotEmpty()) {
                    val deadlineParsed = dateFormat.parse(deadline)
                    if (deadlineParsed != null) {
                        deadlineString = dbDateFormat.format(deadlineParsed)
                    }
                }

                // แปลงความจุเป็นตัวเลข (ถ้ามี)
                var capacity: Int? = null
                if (capacityText.isNotEmpty()) {
                    capacity = capacityText.toIntOrNull()
                }

                // สร้าง EventRequest
                val eventRequest = EventRequest(
                    title = title,
                    description = description,
                    location = location,
                    event_date = eventDateString,
                    category = category,
                    registration_deadline = deadlineString,
                    capacity = capacity
                )

                Log.d(TAG, "Event request: $eventRequest")

                // ดึง token จาก SessionManager
                val token = sessionManager.getToken() ?: ""
                if (token.isEmpty()) {
                    Toast.makeText(this, "กรุณาเข้าสู่ระบบใหม่", Toast.LENGTH_SHORT).show()
                    return
                }

                Log.d(TAG, "Token: $token")

                // แสดงการโหลด
                createButton.isEnabled = false
                createButton.text = "กำลังสร้างกิจกรรม..."

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        // สร้างกิจกรรม
                        val eventRequest = EventRequest(
                            title = title,
                            description = description,
                            location = location,
                            event_date = eventDateString,
                            category = category,
                            registration_deadline = deadlineString,
                            capacity = capacity
                        )


                        val token = sessionManager.getToken() ?: ""
                        val result = withContext(Dispatchers.IO) {
                            eventRepository.createEvent(eventRequest, token)
                        }

                        result.fold(
                            onSuccess = { eventId ->
                                Log.d("CreateEvent", "Event created with ID: $eventId")

                                // ตรวจสอบค่า ID
                                if (eventId <= 0) {
                                    Log.e("CreateEvent", "Invalid event ID received: $eventId")

                                    Toast.makeText(this@CreateEventActivity,
                                        "สร้างกิจกรรมสำเร็จแต่ได้รับ ID ไม่ถูกต้อง: $eventId",
                                        Toast.LENGTH_SHORT).show()
                                    finish()
                                    return@fold
                                }

                                // ถ้ามีการเลือกรูปภาพ ให้อัพโหลดรูปภาพ
                                if (selectedImageUri != null) {
                                    try {
                                        uploadImageForEvent(eventId, selectedImageUri!!, token)
                                    } catch (e: Exception) {
                                        Log.e("CreateEvent", "Error uploading image: ${e.message}", e)
                                        Toast.makeText(this@CreateEventActivity,
                                            "สร้างกิจกรรมสำเร็จแต่ไม่สามารถอัพโหลดรูปภาพได้",
                                            Toast.LENGTH_SHORT).show()
                                        finish()
                                    }
                                } else {
                                    Toast.makeText(this@CreateEventActivity, "สร้างกิจกรรมสำเร็จ", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            },
                            onFailure = { exception ->
                                createButton.isEnabled = true
                                createButton.text = "สร้างกิจกรรม"
                                Toast.makeText(this@CreateEventActivity,
                                    "เกิดข้อผิดพลาด: ${exception.message}",
                                    Toast.LENGTH_SHORT).show()
                            }
                        )
                    } catch (e: Exception) {
                        createButton.isEnabled = true
                        createButton.text = "สร้างกิจกรรม"
                        Toast.makeText(this@CreateEventActivity,
                            "เกิดข้อผิดพลาด: ${e.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "รูปแบบวันที่หรือเวลาไม่ถูกต้อง", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date/time: ${e.message}", e)
            Toast.makeText(this, "รูปแบบวันที่หรือเวลาไม่ถูกต้อง: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun uploadImageForEvent(eventId: Int, imageUri: Uri, token: String) {
        withContext(Dispatchers.IO) {
            try {
                // ดึง MimeType
                val mimeType = contentResolver.getType(imageUri) ?: "image/jpeg"
                Log.d("CreateEvent", "MIME type: $mimeType")

                // แปลง URI เป็นไฟล์
                val inputStream = contentResolver.openInputStream(imageUri)

                if (inputStream != null) {
                    // สร้างไฟล์ชั่วคราว
                    val fileName = "event_image_${System.currentTimeMillis()}.jpg"
                    val tempFile = File(cacheDir, fileName)
                    tempFile.createNewFile()

                    // คัดลอกไฟล์
                    inputStream.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    Log.d("CreateEvent", "Temp file created: ${tempFile.absolutePath}")
                    Log.d("CreateEvent", "Temp file size: ${tempFile.length()} bytes")

                    // เตรียมข้อมูลสำหรับอัพโหลด
                    val mediaType = mimeType.toMediaTypeOrNull() ?: "image/jpeg".toMediaTypeOrNull()
                    val requestBody = tempFile.asRequestBody(mediaType)

                    // ตรวจสอบให้แน่ใจว่าชื่อไฟล์มีนามสกุล
                    val fileNameWithExt = if (fileName.contains(".")) fileName else "$fileName.jpg"
                    val imagePart = MultipartBody.Part.createFormData("image", fileNameWithExt, requestBody)

                    Log.d("CreateEvent", "Created MultipartBody.Part with filename: $fileNameWithExt")

                    // เรียก API
                    val response = RetrofitClient.apiService.uploadEventImage(eventId, imagePart, "Bearer $token")

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@CreateEventActivity, "อัพโหลดรูปภาพสำเร็จ", Toast.LENGTH_SHORT).show()
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "ไม่ทราบสาเหตุ"
                            Toast.makeText(this@CreateEventActivity, "อัพโหลดรูปภาพล้มเหลว: $errorBody", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CreateEventActivity, "ไม่สามารถอ่านข้อมูลรูปภาพได้", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Log.e("CreateEvent", "Upload error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateEventActivity, "เกิดข้อผิดพลาดขณะอัพโหลดรูปภาพ: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    // เพิ่มฟังก์ชันนี้สำหรับแปลง URI เป็น File
    private fun createTempFileFromUri(uri: Uri): File? {
        try {
            val contentResolver = applicationContext.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            // สร้างไฟล์ชั่วคราว
            val tempFile = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
            tempFile.createNewFile()

            // คัดลอกข้อมูล
            inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            return tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file: ${e.message}", e)
            return null
        }
    }
}