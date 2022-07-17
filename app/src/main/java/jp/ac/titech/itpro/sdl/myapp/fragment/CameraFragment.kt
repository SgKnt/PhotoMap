package jp.ac.titech.itpro.sdl.myapp.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import jp.ac.titech.itpro.sdl.myapp.viewmodel.PhotoViewModel
import jp.ac.titech.itpro.sdl.myapp.R
import jp.ac.titech.itpro.sdl.myapp.databinding.FragmentCameraBinding
import java.util.*

class CameraFragment : Fragment() {
    private val args: CameraFragmentArgs by navArgs()
    private lateinit var fragmentCameraBinding: FragmentCameraBinding
    private val photoViewModel: PhotoViewModel by activityViewModels()
    private var imageCapture: ImageCapture? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermissions()
        }
        fragmentCameraBinding.cameraButton.setOnClickListener{ takePhoto() }
    }

    private fun requestCameraPermissions() {
        val launcher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) onRequestPermissionResult@{
                for (permission in REQUIRED_PERMISSIONS) {
                    if (it[permission]!!) {
                        startCamera()
                        return@onRequestPermissionResult
                    }
                }
                Toast.makeText(
                    requireContext(),
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d(TAG, "All permission rejected")
                requireActivity().finish()
            }
        launcher.launch(REQUIRED_PERMISSIONS)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this.requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
                }
            // Image Capture
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture!!)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this.requireContext()))
    }

    private fun takePhoto() {
        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(this.requireContext()),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(e: ImageCaptureException) {
                    Log.e(TAG, "photo capture failed: ${e.message}", e)
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    photoViewModel.also {
                        it.image = image
                        it.latlng = args.mylatlng.v
                        it.date = Date()
                    }
                    findNavController().navigate(R.id.action_camera_to_photo)
                }
            }
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
        private const val TAG = "Camera Fragment"
    }
}