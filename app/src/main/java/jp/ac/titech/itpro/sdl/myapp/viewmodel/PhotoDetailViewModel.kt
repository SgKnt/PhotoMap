package jp.ac.titech.itpro.sdl.myapp.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import java.util.*

class PhotoDetailViewModel : ViewModel() {
    var photos: List<PhotoDetail>? = null
    var latLng: LatLng? = null
    var location: String? = null
}

data class PhotoDetail(
    val image: Bitmap,
    val date: Date,
    val memo: String?
)