package com.shilpakala

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.shilpakala.databinding.FragmentDetailsBinding
import kotlinx.coroutines.launch

class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private var photoPath: String = ""

    // Stores the AI-generated description so it can be passed to PreviewFragment.
    // Empty string means the user didn't generate one — that's fine.
    private var aiDescription: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        photoPath = arguments?.getString("photoPath") ?: ""

        // ── Show thumbnail of captured photo ──────────────────────────────
        if (photoPath.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            if (bitmap != null) {
                binding.ivPhotoThumb.setImageBitmap(bitmap)
            } else {
                binding.ivPhotoThumb.setBackgroundColor(android.graphics.Color.LTGRAY)
            }
        }

        // ── AI Description button ─────────────────────────────────────────
        binding.btnGenerateAI.setOnClickListener {
            val artisanName = binding.etArtisanName.text.toString().trim()
            val productName = binding.etProductName.text.toString().trim()
            val price       = binding.etPrice.text.toString().trim()

            // We need at least a product name to generate a meaningful description
            if (productName.isEmpty()) {
                binding.etProductName.error = "Enter product name first"
                binding.etProductName.requestFocus()
                return@setOnClickListener
            }

            val woodType = getSelectedWoodType()

            // Show loading, hide any previous result
            binding.btnGenerateAI.isEnabled  = false
            binding.layoutAiLoading.visibility = View.VISIBLE
            binding.cardAiDescription.visibility = View.GONE
            aiDescription = ""

            viewLifecycleOwner.lifecycleScope.launch {
                val result = GeminiHelper.generateDescription(
                    productName = productName,
                    woodType    = woodType,
                    artisanName = artisanName.ifEmpty { "a Karnataka artisan" },
                    price       = price.ifEmpty { "—" }
                )

                // Guard: fragment may have been destroyed while Gemini was working
                if (!isAdded || _binding == null) return@launch

                binding.layoutAiLoading.visibility = View.GONE
                binding.btnGenerateAI.isEnabled = true

                result.fold(
                    onSuccess = { description ->
                        aiDescription = description
                        binding.tvAiDescription.text = description
                        binding.cardAiDescription.visibility = View.VISIBLE
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            requireContext(),
                            "AI error: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }

        // ── Generate Photo button ─────────────────────────────────────────
        binding.btnGeneratePhoto.setOnClickListener {
            val artisanName = binding.etArtisanName.text.toString().trim()
            val productName = binding.etProductName.text.toString().trim()
            val price       = binding.etPrice.text.toString().trim()

            // Validation
            if (artisanName.isEmpty()) {
                binding.etArtisanName.error = "Please enter your name"
                binding.etArtisanName.requestFocus()
                return@setOnClickListener
            }
            if (productName.isEmpty()) {
                binding.etProductName.error = "Please enter product name"
                binding.etProductName.requestFocus()
                return@setOnClickListener
            }
            if (price.isEmpty()) {
                binding.etPrice.error = "Please enter price"
                binding.etPrice.requestFocus()
                return@setOnClickListener
            }
            if (photoPath.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "No photo found. Please go back and take a photo.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            binding.btnGeneratePhoto.isEnabled = false
            binding.btnGeneratePhoto.text = "Generating…"

            val bundle = Bundle().apply {
                putString("photoPath",     photoPath)
                putString("artisanName",   artisanName)
                putString("productName",   productName)
                putString("woodType",      getSelectedWoodType())
                putString("price",         price)
                putString("aiDescription", aiDescription)   // may be empty — that's fine
            }
            findNavController().navigate(R.id.action_details_to_preview, bundle)
        }
    }

    /**
     * Returns the wood type string for whichever chip is currently checked.
     * Uses [chipGroupWood.checkedChipId] — the correct way to read a
     * single-selection ChipGroup without the double-toggle bug.
     */
    private fun getSelectedWoodType(): String {
        return when (binding.chipGroupWood.checkedChipId) {
            R.id.chipRosewood   -> "Rosewood"
            R.id.chipTeak       -> "Teak"
            R.id.chipSandalwood -> "Sandalwood"
            else                -> "Ivory Wood"   // chipIvory or nothing selected
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
