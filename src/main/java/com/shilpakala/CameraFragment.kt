package com.shilpakala

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.shilpakala.databinding.FragmentCameraBinding
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment : Fragment() {

    private var _binding: FragmentCameraBinding? = null
    private val binding get() = _binding!!

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var capturedImagePath: String? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(requireContext(), "Camera permission is required to use this app", Toast.LENGTH_LONG).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCameraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnCapture.setOnClickListener { takePhoto() }

        binding.btnNext.setOnClickListener {
            val path = capturedImagePath
            if (path != null) {
                val bundle = Bundle().apply { putString("photoPath", path) }
                findNavController().navigate(R.id.action_camera_to_details, bundle)
            } else {
                Toast.makeText(requireContext(), "Please take a photo first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            requireContext().cacheDir,
            "shilpakala_${System.currentTimeMillis()}.jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        binding.btnCapture.isEnabled = false
        binding.tvGuide.text = "Capturing..."

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedImagePath = photoFile.absolutePath
                    binding.tvGuide.text = "✓ Photo captured! Tap Next to add details."
                    binding.btnCapture.isEnabled = true
                    binding.btnCapture.text = "Retake"

                    // Show thumbnail
                    binding.ivCapturedThumb.visibility = View.VISIBLE
                    val thumb = BitmapFactory.decodeFile(photoFile.absolutePath)
                    binding.ivCapturedThumb.setImageBitmap(thumb)

                    // Show Next button
                    binding.btnNext.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "Photo captured!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    binding.btnCapture.isEnabled = true
                    binding.tvGuide.text = "Capture failed, please try again"
                    Toast.makeText(requireContext(), "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }

    companion object {
        private const val TAG = "ShilpaKalaCamera"
    }
}
