package jp.ac.titech.itpro.sdl.myapp

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import java.util.*

class PhotoViewModel : ViewModel() {
    var image: ImageProxy? = null
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var date: Date? = null
}
