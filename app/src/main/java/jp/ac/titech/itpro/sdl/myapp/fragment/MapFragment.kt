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
import jp.ac.titech.itpro.sdl.myapp.viewmodel.PhotoDetail
import jp.ac.titech.itpro.sdl.myapp.viewmodel.PhotoDetailViewModel
import java.io.Serializable
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.min

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener {
    private lateinit var fragmentCameraBinding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationUpdateCallback: LocationCallback
    private lateinit var appDatabase: AppDatabase
    private lateinit var addMarkerHandler: Handler
    private lateinit var window: ViewGroup
    private val photoDetailViewModel: PhotoDetailViewModel by activityViewModels()
    private var latlng: LatLng? = null
    private val photoInfoMap: MutableMap<LatLng, PhotoInfo> = mutableMapOf()
    private var displayingImage: Bitmap? = null

    inner class PhotoInfoWindowAdaptor : GoogleMap.InfoWindowAdapter {
        private val imageSize: Double = 300.0
        override fun getInfoWindow(marker: Marker): View? {
            val elems = window.findViewById<LinearLayout>(R.id.photo_window_elements)
            elems.removeAllViews()
            val elem = requireActivity().layoutInflater.inflate(R.layout.photo_info_element, null)

            val pi = photoInfoMap[marker.position] ?: return null
            val uri = pi.uri
            val location =
                if (pi.locationName.isNullOrEmpty()) {
                    marker.position.toString()
                } else {
                    pi.locationName
                }

            lateinit var bitmap: Bitmap
            val resolver = requireActivity().contentResolver
            resolver.openFileDescriptor(Uri.parse(uri), "r")?.use {
                bitmap = BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
                displayingImage = bitmap
            }
            val scale = min(imageSize / bitmap.width, imageSize / bitmap.height)
            bitmap = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
            elem.findViewById<ImageView>(R.id.photo_window_image).setImageBitmap(bitmap)

            window.findViewById<TextView>(R.id.photo_window_location).text = location
            elems.addView(elem)
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
        addMarkerHandler = Handler(Looper.getMainLooper())
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
                val photos = photoDao.all
                for (photo in photos) {
                    val pi = PhotoInfo(photo.photoURI, LatLng(photo.latitude, photo.longitude), photo.locationName, photo.memo, photo.date)
                    photoInfoMap[pi.latlng] = pi
                    addMarkerHandler.post{
                        addMarker(MarkerOptions()
                            .position(pi.latlng)
                        )
                    }
                    Log.d(TAG, "id = ${photo.id}, uri = ${photo.photoURI}, latitude = ${photo.latitude}, longitude = ${photo.longitude}, date = ${photo.date}")
                }
            }
        }
    }

    override fun onInfoWindowClick(marker: Marker) {
        Log.d(TAG, "onInfoWindowClick")
        val photoInfo = photoInfoMap[marker.position] ?: return
        val photoDetail = PhotoDetail(displayingImage!!, photoInfo.date, photoInfo.memo)
        photoDetailViewModel.apply {
            latLng = marker.position
            location = photoInfo.locationName
            photos = listOf(photoDetail)
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

data class PhotoInfo(
    val uri: String,
    val latlng: LatLng,
    val locationName: String?,
    val memo: String?,
    val date: Date,
)

// for navigation argument
data class MyLatLng(
    val v: LatLng
) : Serializable
