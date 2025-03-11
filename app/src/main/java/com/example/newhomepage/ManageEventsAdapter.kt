package com.example.newhomepage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.newhomepage.api.models.EventResponse
import java.text.SimpleDateFormat
import java.util.Locale

class ManageEventsAdapter(
    private var eventsList: MutableList<EventResponse>,
    private val onEditClick: (EventResponse) -> Unit,
    private val onDeleteClick: (EventResponse) -> Unit,
    private val onViewRegistrationsClick: (EventResponse) -> Unit
) : RecyclerView.Adapter<ManageEventsAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventNameTextView: TextView = itemView.findViewById(R.id.eventNameTextView)
        val eventDateTextView: TextView = itemView.findViewById(R.id.eventDateTextView)
        val eventCategoryTextView: TextView = itemView.findViewById(R.id.eventCategoryTextView)
        val registrationCountTextView: TextView = itemView.findViewById(R.id.registrationCountTextView)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val viewRegistrationsButton: ImageButton = itemView.findViewById(R.id.viewRegistrationsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventsList[position]

        holder.eventNameTextView.text = event.title
        holder.eventDateTextView.text = formatDate(event.event_date)
        holder.eventCategoryTextView.text = event.category

        // สำหรับการแสดงจำนวนผู้ลงทะเบียน อาจต้องมีข้อมูลเพิ่มเติมในอนาคต
        // สมมติว่ายังไม่ทราบจำนวนผู้ลงทะเบียน
        val registrationCount = "?/${event.capacity ?: "∞"}"
        holder.registrationCountTextView.text = registrationCount

        holder.editButton.setOnClickListener {
            onEditClick(event)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(event)
        }

        holder.viewRegistrationsButton.setOnClickListener {
            onViewRegistrationsClick(event)
        }
    }

    override fun getItemCount(): Int = eventsList.size

    fun updateEventsList(newEventsList: List<EventResponse>) {
        eventsList.clear()
        eventsList.addAll(newEventsList)
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

    fun filterBySearchQuery(query: String, originalList: List<EventResponse>) {
        val filteredList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
        }
        updateEventsList(filteredList)
    }

    fun filterByCategory(category: String, originalList: List<EventResponse>) {
        if (category == "ทั้งหมด") {
            updateEventsList(originalList)
            return
        }

        val filteredList = originalList.filter { it.category == category }
        updateEventsList(filteredList)
    }
}