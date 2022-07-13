package jp.ac.titech.itpro.sdl.myapp

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel

class PhotoViewModel : ViewModel() {
    var photo: ImageProxy? = null
    var latitude: Double = 0.0
    var longitude: Double = 0.0
}
