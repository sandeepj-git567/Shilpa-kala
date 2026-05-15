package com.shilpakala

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.shilpakala.databinding.FragmentPreviewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class PreviewFragment : Fragment() {

    private var _binding: FragmentPreviewBinding? = null
    private val binding get() = _binding!!
    private var brandedBitmap: Bitmap? = null

    private var photoPath = ""
    private var artisanName = ""
    private var productName = ""
    private var woodType = ""
    private var price = ""
    private var aiDescription = ""   // empty if user skipped AI generation

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoPath   = arguments?.getString("photoPath") ?: ""
        artisanName = arguments?.getString("artisanName") ?: ""
        productName = arguments?.getString("productName") ?: ""
        woodType    = arguments?.getString("woodType") ?: "Ivory Wood"
        price       = arguments?.getString("price") ?: "0"
        // aiDescription is optional — empty string if user skipped AI generation
        aiDescription = arguments?.getString("aiDescription") ?: ""

        // Show loading state
        binding.progressBar.visibility = View.VISIBLE
        binding.ivBrandedPhoto.visibility = View.INVISIBLE

        // Process image using coroutines (avoids ANR, lifecycle-safe)
        viewLifecycleOwner.lifecycleScope.launch {
            val branded = withContext(Dispatchers.IO) {
                runCatching {
                    val original = BitmapFactory.decodeFile(photoPath)
                    if (original != null) {
                        ImageProcessor.addHeritageBranding(
                            original, artisanName, productName, woodType, price
                        )
                    } else null
                }.getOrNull()
            }

            // Back on Main thread
            if (!isAdded || _binding == null) return@launch

            binding.progressBar.visibility = View.GONE
            if (branded != null) {
                brandedBitmap = branded
                binding.ivBrandedPhoto.visibility = View.VISIBLE
                binding.ivBrandedPhoto.setImageBitmap(branded)
                binding.tvProductLabel.text = productName
                binding.tvArtisanLabel.text = "By $artisanName" +
                        if (aiDescription.isNotBlank()) "\n\n$aiDescription" else ""
            } else {
                Toast.makeText(requireContext(), "Could not process image. Please try again.", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
            }
        }

        binding.btnShareWhatsapp.setOnClickListener {
            val bmp = brandedBitmap
            if (bmp != null) shareImage(bmp, "com.whatsapp")
            else Toast.makeText(requireContext(), "Photo still loading, please wait...", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareFacebook.setOnClickListener {
            val bmp = brandedBitmap
            if (bmp != null) shareImage(bmp, "com.facebook.katana")
            else Toast.makeText(requireContext(), "Photo still loading, please wait...", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveGallery.setOnClickListener {
            val bmp = brandedBitmap
            if (bmp != null) saveToGallery(bmp)
            else Toast.makeText(requireContext(), "Photo still loading, please wait...", Toast.LENGTH_SHORT).show()
        }

        binding.btnShareAny.setOnClickListener {
            val bmp = brandedBitmap
            if (bmp != null) shareImage(bmp, null)
            else Toast.makeText(requireContext(), "Photo still loading, please wait...", Toast.LENGTH_SHORT).show()
        }

        binding.btnNewPhoto.setOnClickListener {
            findNavController().navigate(R.id.action_preview_to_home)
        }
    }

    private fun shareImage(bitmap: Bitmap, targetPackage: String?) {
        try {
            val file = File(requireContext().cacheDir, "shilpakala_share.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }

            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )

            val shareText = buildString {
                appendLine("✨ $productName")
                appendLine("🌿 Handmade in Karnataka by $artisanName")
                appendLine("🪵 $woodType")
                appendLine("💰 ₹$price")
                if (aiDescription.isNotBlank()) {
                    appendLine()
                    appendLine(aiDescription)
                }
                append("Made with Shilpa-Kala")
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (targetPackage != null) setPackage(targetPackage)
            }

            try {
                startActivity(Intent.createChooser(intent, "Share your craft"))
            } catch (e: android.content.ActivityNotFoundException) {
                // Target app not installed, fall back to generic share
                val fallback = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(fallback, "Share your craft"))
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Share failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveToGallery(bitmap: Bitmap) {
        viewLifecycleOwner.lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                runCatching {
                    val filename = "ShilpaKala_${System.currentTimeMillis()}.jpg"
                    val contentValues = android.content.ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/ShilpaKala")
                    }
                    val uri = requireContext().contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                    uri?.let {
                        requireContext().contentResolver.openOutputStream(it)?.use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                        }
                        true
                    } ?: false
                }.getOrElse { false }
            }

            if (!isAdded || _binding == null) return@launch

            if (success) {
                Toast.makeText(
                    requireContext(),
                    "✅ Saved to Gallery → Pictures/ShilpaKala",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Save failed. Please check storage permission.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}