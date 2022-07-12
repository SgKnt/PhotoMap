package jp.ac.titech.itpro.sdl.myapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import jp.ac.titech.itpro.sdl.myapp.databinding.FragmentMapBinding

class MapFragment : Fragment(), OnMapReadyCallback {
    private lateinit var fragmentCameraBinding: FragmentMapBinding
    private lateinit var map: GoogleMap
    private lateinit var locationClient: FusedLocationProviderClient

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
        // Camera
        fragmentCameraBinding.cameraButton.setOnClickListener(
            Navigation.createNavigateOnClickListener(R.id.action_map_to_camera)
        )
    }

    override fun onResume() {
        super.onResume()
        setLocation()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.moveCamera(CameraUpdateFactory.zoomTo(15f))
    }

    private fun setLocation() {
        requestLocationPermissions()
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
                locationClient.lastLocation.addOnSuccessListener(this.requireActivity()) Loc@{loc ->
                    loc?.let {
                        val ll = LatLng(it.latitude, it.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLng(ll))
                    } ?: run {
                        val request = LocationRequest.create().apply {
                            interval = 10000
                            fastestInterval = 5000
                            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                        }
                        locationClient.requestLocationUpdates(
                            request,
                            object : LocationCallback() {
                                override fun onLocationResult(result: LocationResult) {
                                    super.onLocationResult(result)
                                    val loc = result.lastLocation
                                    val ll = LatLng(loc.latitude, loc.longitude)
                                    map.animateCamera(CameraUpdateFactory.newLatLng(ll))
                                    locationClient.removeLocationUpdates(this)
                                }
                            }, Looper.getMainLooper()
                        )
                    }

                }
                return
            }
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