package com.lasallecollegevancouver.gameinventoryapp.ui.smart_add

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentPhotoScanBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CatalogItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import kotlinx.coroutines.launch

// Allows user to identify a game by taking a photo of the box, cart, or disc label
// ML Kit reads the text, extracts the most likely title, and searches the catalog
class PhotoScanFragment : Fragment() {

    private var _binding: FragmentPhotoScanBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Words that appear on game packaging but are not the title
    private val noiseWords = setOf(
        "nintendo", "sony", "sega", "microsoft", "xbox", "playstation", "gamecube",
        "switch", "presents", "licensed", "official", "product", "trademark",
        "esrb", "pegi", "rating", "rated", "everyone", "teen", "mature",
        "entertainment", "software", "inc", "llc", "corp", "ltd",
        "tm", "r", "c", "www", "http", "https", "com",
        "game", "games", "video", "only", "for", "the", "and", "in", "of",
        "players", "player", "online", "multiplayer", "single",
        "content", "descriptors", "violence", "mild", "language",
        "blood", "fantasy", "comic", "suggestive", "themes",
        "manufactured", "printed", "made", "japan", "usa", "europe",
        "pal", "ntsc", "hd", "4k", "remastered", "edition", "complete"
    )

    // Launches system camera and returns a small bitmap of the photo
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            binding.photoPreview.setImageBitmap(bitmap)
            binding.photoPreview.visibility = View.VISIBLE
            readTextFromPhoto(bitmap)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhotoScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.takePhotoButton.setOnClickListener {
            cameraLauncher.launch(null)
        }

        binding.searchButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            if (title.isNotEmpty()) searchCatalog(title)
        }

        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }
    }

    // Runs ML Kit text recognition on the captured bitmap
    private fun readTextFromPhoto(bitmap: Bitmap) {
        binding.statusText.text = "Reading text from photo..."
        binding.statusText.visibility = View.VISIBLE
        binding.takePhotoButton.isEnabled = false

        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedTitle = extractTitle(visionText.text)
                if (detectedTitle != null) {
                    binding.statusText.text = "Text detected — edit if needed, then search"
                    binding.titleInput.setText(detectedTitle)
                    binding.titleLayout.visibility = View.VISIBLE
                    binding.searchButton.isEnabled = true
                    binding.searchButton.visibility = View.VISIBLE
                } else {
                    binding.statusText.text = "Could not read a title from the photo. Try better lighting or a closer shot."
                }
                binding.takePhotoButton.isEnabled = true
                binding.takePhotoButton.text = "Retake Photo"
            }
            .addOnFailureListener {
                binding.statusText.text = "Could not process the photo. Try again."
                binding.takePhotoButton.isEnabled = true
            }
    }

    // Picks the most likely game title from raw OCR text
    // Strategy: find the longest line that isn't made up entirely of noise words
    private fun extractTitle(rawText: String): String? {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { line ->
                // Must have at least 2 characters and not be all numbers or symbols
                line.length >= 2 && line.any { it.isLetter() }
            }

        // Score each line — higher score = more likely to be the title
        val scored = lines.map { line ->
            val words = line.split(" ", "-", ":", "\t").filter { it.isNotEmpty() }
            val noiseCount = words.count { noiseWords.contains(it.lowercase()) }
            val realWordCount = words.size - noiseCount
            // Prefer longer lines with fewer noise words
            val score = (realWordCount * 3) - noiseCount + (line.length / 5)
            Pair(line, score)
        }

        val best = scored.filter { it.second > 0 }.maxByOrNull { it.second }
        return best?.first?.take(60) // cap at 60 chars so it doesn't overfill the search
    }

    private fun searchCatalog(query: String) {
        binding.statusText.text = "Searching for \"$query\"..."
        binding.searchButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val results = repository.searchCatalog(query)
                if (results.isEmpty()) {
                    binding.statusText.text = "No results found. Edit the title and try again."
                    binding.searchButton.isEnabled = true
                } else {
                    showResultsPicker(results)
                }
            } catch (exception: Exception) {
                binding.statusText.text = "Could not reach server. Check your connection."
                binding.searchButton.isEnabled = true
            }
        }
    }

    private fun showResultsPicker(results: List<CatalogItem>) {
        val displayNames = results.map { "${it.title} (${it.platform})" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("${results.size} results — pick the right one")
            .setItems(displayNames) { _, index ->
                navigateWithItem(results[index])
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.searchButton.isEnabled = true
                binding.statusText.text = "Pick a result or edit the title and search again."
            }
            .show()
    }

    private fun navigateWithItem(item: CatalogItem) {
        val bundle = Bundle().apply {
            putString("prefillTitle", item.title)
            putString("prefillPlatform", item.platform)
            putInt("prefillCatalogItemId", item.id)
            putDouble("prefillLooseValue", item.looseValue)
            putDouble("prefillCibValue", item.cibValue)
            putDouble("prefillNewValue", item.newValue)
        }
        findNavController().navigate(R.id.action_photoScan_to_addEditGame, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recognizer.close()
        _binding = null
    }
}
