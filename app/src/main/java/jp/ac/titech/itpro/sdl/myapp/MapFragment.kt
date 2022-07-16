package jp.ac.titech.itpro.sdl.myapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import jp.ac.titech.itpro.sdl.myapp.database.AppDatabase
import jp.ac.titech.itpro.sdl.myapp.databinding.FragmentMapBinding
import java.io.Serializable
import kotlin.concurrent.thread

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var fragmentCameraBinding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationUpdateCallback: LocationCallback
    private lateinit var appDatabase: AppDatabase
    private var latlng: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "On Create View")
        fragmentCameraBinding = FragmentMapBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "On View Created")

        // Map
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
            val latitude = ll.latitude
            val longitude = ll.longitude
            val action = MapFragmentDirections.actionMapToCamera(LatLong(latitude, longitude))
            it.findNavController().navigate(action)
        }

        appDatabase = AppDatabase.getInstance(requireContext())
        thread {
            val photoDao = appDatabase.photoDao()
            val photos = photoDao.all
            for (photo in photos) {
                Log.d(TAG, "id = ${photo.id}, uri = ${photo.photoURI}, latitude = ${photo.latitude}, longitude = ${photo.longitude}, date = ${photo.date}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdate()
        Handler(Looper.getMainLooper()).postDelayed({ setLocation() }, 3000)
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdate()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.moveCamera(CameraUpdateFactory.zoomTo(15f))
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
        }
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
        @JvmStatic
        fun newInstance() = CameraFragment()
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        private const val TAG = "Map Fragment"
    }
}

data class LatLong(
    val latitude: Double,
    val longitude: Double,
) : Serializable
