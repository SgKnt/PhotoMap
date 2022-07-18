package jp.ac.titech.itpro.sdl.myapp.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import jp.ac.titech.itpro.sdl.myapp.database.AppDatabase
import jp.ac.titech.itpro.sdl.myapp.databinding.FragmentMapBinding
import jp.ac.titech.itpro.sdl.myapp.R
import jp.ac.titech.itpro.sdl.myapp.database.entity.Photo as PhotoInfo
import jp.ac.titech.itpro.sdl.myapp.viewmodel.PhotoDetail
import jp.ac.titech.itpro.sdl.myapp.viewmodel.PhotoDetailViewModel
import java.io.Serializable
import kotlin.concurrent.thread
import kotlin.math.min

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {
    private lateinit var fragmentCameraBinding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationUpdateCallback: LocationCallback
    private lateinit var appDatabase: AppDatabase
    private lateinit var handler: Handler
    private lateinit var window: ViewGroup
    private val photoDetailViewModel: PhotoDetailViewModel by activityViewModels()
    private var latlng: LatLng? = null
    private val locationMap: MutableMap<Long, LocationInfo> = mutableMapOf()
    private val photoInfoMap: MutableMap<Long, MutableList<PhotoInfo>> = mutableMapOf()
    private var displayingImageMap: MutableMap<Long, Bitmap> = mutableMapOf()

    inner class PhotoInfoWindowAdaptor : GoogleMap.InfoWindowAdapter {
        private val imageSize: Double = 300.0
        override fun getInfoWindow(marker: Marker): View? {
            val elems = window.findViewById<LinearLayout>(R.id.photo_window_elements)
            elems.removeAllViews()
            displayingImageMap.clear()

            val photoInfoList = photoInfoMap[marker.tag] ?: return null
            photoInfoList.forEach{photoInfo ->
                val elem = requireActivity().layoutInflater.inflate(R.layout.photo_info_element, null)

                val uri = photoInfo.photoURI
                lateinit var bitmap: Bitmap
                val resolver = requireActivity().contentResolver
                resolver.openFileDescriptor(Uri.parse(uri), "r")?.use {
                    bitmap = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                    displayingImageMap[photoInfo.id] = bitmap
                }
                val scale = min(imageSize / bitmap.width, imageSize / bitmap.height)
                bitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
                elem.findViewById<ImageView>(R.id.photo_window_image).setImageBitmap(bitmap)
                elems.addView(elem)
            }

            val location = locationMap[marker.tag]!!.let {
                if (it.name.isNullOrEmpty()) {
                    "lat: ${it.latlng.latitude}, lng: ${it.latlng.longitude}"
                } else {
                    it.name
                }
            }

            window.findViewById<TextView>(R.id.photo_window_location).text = location
            return window
        }

        override fun getInfoContents(p0: Marker): View? {
            return null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocationPermissions()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "On Create View")
        fragmentCameraBinding = FragmentMapBinding.inflate(inflater, container, false)
        window = inflater.inflate(R.layout.photo_info_window, null) as ConstraintLayout
        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "On View Created")

        appDatabase = AppDatabase.getInstance(requireContext())

        // Map
        handler = Handler(Looper.getMainLooper())
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Location
        locationClient = LocationServices.getFusedLocationProviderClient(this.requireActivity())
        locationUpdateCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val loc = result.lastLocation
                latlng = LatLng(loc.latitude, loc.longitude)
            }
        }

        // Camera
        fragmentCameraBinding.cameraButton.setOnClickListener cameraButtonAction@{
            val ll = latlng ?: return@cameraButtonAction
            val action = MapFragmentDirections.actionMapToCamera(MyLatLng(ll))
            it.findNavController().navigate(action)
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdate()
        setLocation()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdate()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        with(map) {
            setInfoWindowAdapter(PhotoInfoWindowAdaptor())
            setOnInfoWindowClickListener(this@MapFragment)
            moveCamera(CameraUpdateFactory.zoomTo(15f))
            thread {
                val photoDao = appDatabase.photoDao()
                val locationDao = appDatabase.locationDao()
                val locations = locationDao.all
                val photos = photoDao.all
                locations.forEach{
                    val latlng = LatLng(it.latitude, it.longitude)
                    val name = it.name
                    locationMap[it.id] = LocationInfo(latlng, name)
                    handler.post{
                        addMarker(MarkerOptions()
                            .position(latlng)
                        )?.setTag(it.id)
                    }
                }
                photos.forEach{photo ->
                    photoInfoMap[photo.locationId]?.add(photo) ?: run {
                        photoInfoMap[photo.locationId] = mutableListOf(photo)
                    }
                }
            }
        }
    }

    override fun onInfoWindowClick(marker: Marker) {
        Log.d(TAG, "onInfoWindowClick")
        val photoInfoList = photoInfoMap[marker.tag] ?: return
        val location = locationMap[marker.tag] ?: return
        val photoDetails = photoInfoList.map{
            Log.d(TAG, "memo = ${it.memo}")
            PhotoDetail(displayingImageMap[it.id]!!, it.date, it.memo)
        }
        photoDetailViewModel.apply {
            latLng = marker.position
            this.location = location.name
            photos = photoDetails
        }
        Log.d(TAG, "map to photo")
        findNavController().navigate(R.id.action_map_to_photoDetail)
    }

    private fun startLocationUpdate() {
        requestLocationPermissions()
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                val request = LocationRequest.create().apply {
                    interval = 5000
                    fastestInterval = 3000
                    priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                }
                locationClient.requestLocationUpdates(request, locationUpdateCallback, Looper.getMainLooper())
            }
        }
    }

    private fun stopLocationUpdate() {
        locationClient.removeLocationUpdates(locationUpdateCallback)
    }

    private fun setLocation() {
        latlng?.let {
            map.moveCamera(CameraUpdateFactory.newLatLng(it))
        } ?: Handler(Looper.getMainLooper()).postDelayed({setLocation()}, 100)
    }

    private fun requestLocationPermissions() {
        if (allPermissionsGranted()) {
            return
        }
        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) onRequestPermissionResult@{
            for (permission in REQUIRED_PERMISSIONS) {
                if (it[permission]!!) {
                    return@onRequestPermissionResult
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            Log.d(TAG, "All permission rejected")
            requireActivity().finish()
        }
        launcher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        private const val TAG = "Map Fragment"
    }
}

data class LocationInfo(
    val latlng: LatLng,
    val name: String?
)

// for navigation argument
data class MyLatLng(
    val v: LatLng
) : Serializable
