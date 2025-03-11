import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class LocationImagesAdapter(private val images: List<Int>) : RecyclerView.Adapter<LocationImagesAdapter.LocationImageViewHolder>() {

    // สร้าง ViewHolder สำหรับแสดงภาพ
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationImageViewHolder {
        val imageView = ImageView(parent.context)
        imageView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        return LocationImageViewHolder(imageView)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY  // แสดงรายละเอียดทั้งหมด
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // เชื่อมโยงข้อมูลกับ ViewHolder
    override fun onBindViewHolder(holder: LocationImageViewHolder, position: Int) {
        holder.imageView.setImageResource(images[position])  // กำหนดแหล่งที่มาของภาพ
    }

    // คืนค่าจำนวนของรายการ
    override fun getItemCount(): Int = images.size

    // ViewHolder สำหรับ ImageView
    class LocationImageViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
}
