// แก้ไขไฟล์ app/src/main/java/com/example/newhomepage/EventModel.kt
package com.example.newhomepage

import android.os.Parcel
import android.os.Parcelable

data class EventModel(
    val id: Int,
    val eventName: String,
    val eventDate: String,
    val imageResId: Int,
    val category: String,
    val imageUrl: String? = null // เพิ่มฟิลด์นี้
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString() // อ่านค่า imageUrl
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(eventName)
        parcel.writeString(eventDate)
        parcel.writeInt(imageResId)
        parcel.writeString(category)
        parcel.writeString(imageUrl) // เขียนค่า imageUrl
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<EventModel> {
        override fun createFromParcel(parcel: Parcel): EventModel {
            return EventModel(parcel)
        }

        override fun newArray(size: Int): Array<EventModel?> {
            return arrayOfNulls(size)
        }
    }
}