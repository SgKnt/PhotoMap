package jp.ac.titech.itpro.sdl.photomap.viewmodel

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import java.util.*

class PhotoViewModel : ViewModel() {
    var image: ImageProxy? = null
    var latlng: LatLng? = null
    var date: Date? = null
}
