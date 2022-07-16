package jp.ac.titech.itpro.sdl.myapp

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.*
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageProxy
import androidx.fragment.app.activityViewModels
import jp.ac.titech.itpro.sdl.myapp.databinding.PhotoFragmentBinding
import java.util.*
import java.text.SimpleDateFormat
import kotlin.concurrent.thread

class PhotoFragment : Fragment() {
    private lateinit var binding: PhotoFragmentBinding
    private val photoViewModel: PhotoViewModel by activityViewModels()
    private lateinit var backToMapHandler: Handler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PhotoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val image = photoViewModel.image ?: return
        val date = photoViewModel.date ?: return
        binding.photo.setImageBitmap(imageProxyToBitmap(image))
        Handler(Looper.getMainLooper()).postDelayed({
            val uri = saveImage(image, date)

        }, 3000)
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    /**
     * @return String which describes uri, or null if failed to store image
     */
    private fun saveImage(image: ImageProxy, date: Date) : String? {
        val resolver = requireActivity().contentResolver

        val collection =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

        val value = ContentValues().apply {
            val directory = Environment.getExternalStorageDirectory().absolutePath + "/Pictures/Photo-Map/"
            val fileName = makeImageName(date)
            put(MediaStore.Images.Media.DISPLAY_NAME, makeImageName(date))
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Photo-Map")
                put(MediaStore.Images.Media.IS_PENDING, true)
            } else {
                put(MediaStore.Images.Media.DATA, directory + fileName)
            }
        }

        val uri = resolver.insert(collection, value)!!
        Log.d(TAG, "path is ${uri.path}")
        resolver.openOutputStream(uri)?.let {
            val bitmap = imageProxyToBitmap(image)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        } ?: run {
            resolver.delete(uri, null, null)
            return null
        }

        value.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            value.put(MediaStore.Images.Media.IS_PENDING, false)
        }
        resolver.update(uri, value, null, null)
        return uri.toString()
    }

    private fun makeImageName(date: Date) : String {
        val sdf = SimpleDateFormat(URI_FORMAT, Locale.JAPAN)
        Log.i(TAG, "date long: ${date.time}")
        return sdf.format(date) + ".png"
    }

    companion object {
        private const val TAG = "Photo Content"
        private const val URI_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }

}