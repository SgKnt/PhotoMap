package jp.ac.titech.itpro.sdl.myapp.fragment

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
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.model.LatLng
import jp.ac.titech.itpro.sdl.myapp.viewmodel.PhotoViewModel
import jp.ac.titech.itpro.sdl.myapp.R
import jp.ac.titech.itpro.sdl.myapp.database.AppDatabase
import jp.ac.titech.itpro.sdl.myapp.database.entity.Photo
import jp.ac.titech.itpro.sdl.myapp.databinding.FragmentPhotoBinding
import java.util.*
import java.text.SimpleDateFormat
import kotlin.Exception
import kotlin.concurrent.thread

class PhotoFragment : Fragment() {
    private lateinit var photoFragmentBinding: FragmentPhotoBinding
    private val photoViewModel: PhotoViewModel by activityViewModels()
    private lateinit var appDatabase: AppDatabase
    private lateinit var image: ImageProxy
    private lateinit var latlng: LatLng
    private lateinit var date: Date
    private lateinit var backToMapHandler: Handler

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        photoFragmentBinding = FragmentPhotoBinding.inflate(inflater, container, false)
        return photoFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        appDatabase = AppDatabase.getInstance(requireContext())
        backToMapHandler = Handler(Looper.getMainLooper())

        image = photoViewModel.image ?: run {
            Log.e(TAG, "Null image", Exception())
            return
        }
        date = photoViewModel.date ?: run {
            Log.e(TAG, "Null date", Exception())
            return
        }
        latlng = photoViewModel.latlng ?: run {
            Log.e(TAG, "Null latlng", Exception())
            return
        }

        photoFragmentBinding.photoInput.setImageBitmap(imageProxyToBitmap(image))

        photoFragmentBinding.savePhotoButton.setOnClickListener{ savePhoto() }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun savePhoto() {
        thread {
            saveImage(image, date)?.let { uri ->
                val location = photoFragmentBinding.locationInput.text.toString()
                val memo = photoFragmentBinding.memoInput.text.toString()
                insertPhotoToDB(uri, latlng.latitude, latlng.longitude, location, memo, date)
            }
            backToMapHandler.post {
                findNavController().navigate(R.id.action_photo_to_map)
            }
        }
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

    private fun insertPhotoToDB(uri: String, lat: Double, lng: Double, location: String, memo: String, date: Date) {
        val photoDao = appDatabase.photoDao()
        val photo = Photo(0, uri, lat, lng, location, memo, date)
        photoDao.insert(photo)
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