package jp.ac.titech.itpro.sdl.myapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import jp.ac.titech.itpro.sdl.myapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var locationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Google Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // Location (GPS)
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        // Camera
        val cameraButton = findViewById<FloatingActionButton>(R.id.camera_button)
        cameraButton.setOnClickListener{_: View ->

        }
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
        for (permission in LOCATION_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, LOCATION_PERMISSIONS, REQ_LOC_PERMISSION)
                break
            }
        }
        locationClient.lastLocation.addOnSuccessListener(this){loc ->
            map.animateCamera(CameraUpdateFactory.newLatLng(LatLng(loc.latitude, loc.longitude)))
        }
    }


    companion object {
        private val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        private val REQ_LOC_PERMISSION = 8888
    }
}