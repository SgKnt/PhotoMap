package jp.ac.titech.itpro.sdl.myapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageProxy
import androidx.fragment.app.activityViewModels
import jp.ac.titech.itpro.sdl.myapp.databinding.PhotoFragmentBinding

class PhotoFragment : Fragment() {
    private lateinit var binding: PhotoFragmentBinding
    private val photoViewModel: PhotoViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = PhotoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        photoViewModel.photo?.let {
            binding.photo.setImageBitmap(imageProxyToBitmap(it))
        }
        Log.d(TAG, "start onViewCreated")
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    companion object {
        private const val TAG = "Photo Content"
    }

}