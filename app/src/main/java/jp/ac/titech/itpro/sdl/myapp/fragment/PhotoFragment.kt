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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.camera.core.ImageProxy
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.model.LatLng
import jp.ac.titech.itpro.sdl.myapp.viewmodel.PhotoViewModel
import jp.ac.titech.itpro.sdl.myapp.R
import jp.ac.titech.itpro.sdl.myapp.database.AppDatabase
import jp.ac.titech.itpro.sdl.myapp.database.entity.Location
import jp.ac.titech.itpro.sdl.myapp.database.entity.Photo
import jp.ac.titech.itpro.sdl.myapp.databinding.FragmentPhotoBinding
import java.util.*
import java.text.SimpleDateFormat
import kotlin.Exception
import kotlin.concurrent.thread
import kotlin.math.abs

class PhotoFragment : Fragment(), AdapterView.OnItemSelectedListener {
    private lateinit var photoFragmentBinding: FragmentPhotoBinding
    private val photoViewModel: PhotoViewModel by activityViewModels()
    private lateinit var appDatabase: AppDatabase
    private lateinit var image: ImageProxy
    private lateinit var latlng: LatLng
    private lateinit var date: Date
    private lateinit var handler: Handler
    @Volatile private var locations: List<Location> = listOf()

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
        handler = Handler(Looper.getMainLooper())

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

        changeEditTextAvailability(photoFragmentBinding.locationInput, false)
        photoFragmentBinding.locationSpinner.onItemSelectedListener = this
        addAdapterToSpinner()
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        Log.d(TAG, "pos=$pos, ${parent?.selectedItemPosition}")
        changeEditTextAvailability(photoFragmentBinding.locationInput, pos == LOCATION_NEW)
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
    }

    private fun addAdapterToSpinner() {
        thread {
            locations = appDatabase.locationDao().all
            val selections = mutableListOf<String>("設定しない", "新規").apply {
                addAll(
                    locations
                        .sortedWith(Comparator { l1, l2 ->
                            val dis1 =
                                (abs(l1.latitude - latlng.latitude) + abs(l1.longitude - latlng.longitude))
                            val dis2 =
                                (abs(l2.latitude - latlng.latitude) + abs(l2.longitude - latlng.longitude))
                            compareValues(dis1, dis2)
                        })
                        .mapNotNull { it.name }
                )
            }
            handler.post {
                photoFragmentBinding.locationSpinner.adapter =
                    ArrayAdapter(
                        this.requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        selections
                    )
            }
        }
    }

    private fun savePhoto() {
        thread {
            saveImage(image, date)?.let { uri ->
                var location: String? = photoFragmentBinding.locationInput.text.toString()
                if (photoFragmentBinding.locationSpinner.selectedItemPosition == LOCATION_NOT_SPECIFIED) {
                    location = null
                }
                val memo = photoFragmentBinding.memoInput.text.toString()
                insertPhotoToDB(uri, latlng.latitude, latlng.longitude, location, memo, date)
            }
            handler.post {
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

    private fun insertPhotoToDB(uri: String, lat: Double, lng: Double, location: String?, memo: String, date: Date) {
        val locationDao = appDatabase.locationDao()
        val photoDao = appDatabase.photoDao()
        val locationName =
            if (location.isNullOrEmpty()) {
                null
            } else {
                location
            }

        appDatabase.runInTransaction {
            val spinner = photoFragmentBinding.locationSpinner
            val locationId =
                when {
                    spinner.selectedItemId > 1 -> {
                        locations.find { it.name == spinner.selectedItem }!!.id
                    }
                    spinner.selectedItemPosition == LOCATION_NOT_SPECIFIED -> {
                        locationDao.insert(Location(0, lat, lng, null))
                    }
                    else -> {
                        locations.find{it.name == locationName}?.id
                            ?: locationDao.insert(Location(0, lat, lng, locationName))
                    }
                }
            val photo = Photo(0, uri, locationId, memo, date)
            photoDao.insert(photo)
        }
    }

    private fun changeEditTextAvailability(text: EditText, isEditable: Boolean) {
        /*
        if (isEditable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                text.focusable = EditText.FOCUSABLE
            } else
            text.isEnabled = false
        }
         */
        text.isEnabled = isEditable
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        buffer.rewind()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun makeImageName(date: Date) : String {
        val sdf = SimpleDateFormat(URI_FORMAT, Locale.JAPAN)
        Log.i(TAG, "date long: ${date.time}")
        return sdf.format(date) + ".png"
    }

    companion object {
        private const val TAG = "Photo Content"
        private const val URI_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val LOCATION_NOT_SPECIFIED = 0
        private const val LOCATION_NEW = 1
    }

}