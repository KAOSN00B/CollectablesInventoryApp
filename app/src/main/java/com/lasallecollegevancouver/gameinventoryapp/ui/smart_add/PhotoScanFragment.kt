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

// Identifies a game by photo: ML Kit reads the text, detects the platform from known
// keywords, extracts the title, then searches the catalog scoped to that platform.
class PhotoScanFragment : Fragment() {

    private var _binding: FragmentPhotoScanBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Detected platform from OCR — null means unknown, user can set it manually
    private var detectedPlatform: String? = null

    private val allPlatforms = arrayOf(
        "Any Platform",
        "NES", "SNES", "N64", "GameCube", "Wii", "Switch",
        "Game Boy", "Game Boy Color", "GBA", "Virtual Boy", "DS", "3DS",
        "PS1", "PS2", "PS3", "PS4", "PS5", "PSP",
        "Xbox", "Xbox 360",
        "Genesis", "Saturn", "Dreamcast",
        "Atari 2600", "Atari 7800", "Jaguar", "Lynx"
    )

    // Maps platform keywords (found in OCR text) to catalog platform names
    // Listed longest-first so "game boy color" matches before "game boy"
    private val platformKeywords = listOf(
        "super nintendo entertainment system" to "SNES",
        "super nintendo"            to "SNES",
        "nintendo entertainment system" to "NES",
        "nintendo ds"               to "DS",
        "nintendo 3ds"              to "3DS",
        "new nintendo 3ds"          to "3DS",
        "game boy color"            to "Game Boy Color",
        "game boy advance"          to "GBA",
        "gameboy advance"           to "GBA",
        "gameboy color"             to "Game Boy Color",
        "game boy"                  to "Game Boy",
        "gameboy"                   to "Game Boy",
        "virtual boy"               to "Virtual Boy",
        "nintendo 64"               to "N64",
        "nintendo gamecube"         to "GameCube",
        "gamecube"                  to "GameCube",
        "nintendo wii"              to "Wii",
        "nintendo switch"           to "Switch",
        "playstation 5"             to "PS5",
        "playstation 4"             to "PS4",
        "playstation 3"             to "PS3",
        "playstation 2"             to "PS2",
        "playstation portable"      to "PSP",
        "playstation"               to "PS1",
        "psp"                       to "PSP",
        "sega genesis"              to "Genesis",
        "mega drive"                to "Genesis",
        "sega saturn"               to "Saturn",
        "sega dreamcast"            to "Dreamcast",
        "dreamcast"                 to "Dreamcast",
        "atari 2600"                to "Atari 2600",
        "atari 7800"                to "Atari 7800",
        "atari jaguar"              to "Jaguar",
        "atari lynx"                to "Lynx",
        "xbox series"               to "Xbox",
        "xbox 360"                  to "Xbox 360",
        "xbox one"                  to "Xbox",
        "gba"                       to "GBA",
        "gbc"                       to "Game Boy Color",
        "n64"                       to "N64",
        "ps5"                       to "PS5",
        "ps4"                       to "PS4",
        "ps3"                       to "PS3",
        "ps2"                       to "PS2",
        "psx"                       to "PS1",
        "ps1"                       to "PS1",
        "snes"                      to "SNES",
        "nes"                       to "NES",
        "3ds"                       to "3DS",
        "ds"                        to "DS",
        "wii"                       to "Wii",
        "genesis"                   to "Genesis",
        "saturn"                    to "Saturn",
        "jaguar"                    to "Jaguar",
        "lynx"                      to "Lynx",
        "xbox"                      to "Xbox"
    )

    // Words that are never part of a game title — filtered during title extraction
    private val noiseWords = setOf(
        "nintendo", "sony", "sega", "microsoft", "presents", "licensed",
        "official", "product", "trademark", "esrb", "pegi", "rating",
        "rated", "everyone", "teen", "mature", "entertainment", "software",
        "inc", "llc", "corp", "ltd", "tm", "www", "http", "https", "com",
        "players", "player", "online", "multiplayer", "single",
        "content", "descriptors", "violence", "mild", "language",
        "blood", "fantasy", "comic", "suggestive", "themes",
        "manufactured", "printed", "made", "japan", "usa", "europe",
        "pal", "ntsc", "hd", "4k", "remastered", "complete"
    )

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            binding.photoPreview.setImageBitmap(bitmap)
            binding.photoPreview.visibility = View.VISIBLE
            analyzePhoto(bitmap)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPhotoScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.takePhotoButton.setOnClickListener { cameraLauncher.launch(null) }

        binding.platformButton.setOnClickListener { showPlatformPicker() }

        binding.searchButton.setOnClickListener {
            val title = binding.titleInput.text.toString().trim()
            if (title.isNotEmpty()) searchCatalog(title)
        }

        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }
    }

    private fun analyzePhoto(bitmap: Bitmap) {
        binding.statusText.text = "Reading text from photo..."
        binding.statusText.visibility = View.VISIBLE
        binding.takePhotoButton.isEnabled = false

        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val rawText = visionText.text

                // Detect platform first — used to scope search and exclude those words from title
                detectedPlatform = detectPlatform(rawText)
                updatePlatformButton()

                // Extract title excluding the detected platform's keywords
                val title = extractTitle(rawText, detectedPlatform)

                if (title != null) {
                    val platformLabel = detectedPlatform?.let { " on $it" } ?: ""
                    binding.statusText.text = "Detected$platformLabel — edit title if wrong, then search"
                    binding.titleInput.setText(title)
                    binding.titleLayout.visibility = View.VISIBLE
                    binding.platformRow.visibility = View.VISIBLE
                    binding.searchButton.isEnabled = true
                    binding.searchButton.visibility = View.VISIBLE
                } else {
                    binding.statusText.text = "Could not read a title. Try better lighting or a closer shot."
                    binding.platformRow.visibility = View.VISIBLE
                }

                binding.takePhotoButton.isEnabled = true
                binding.takePhotoButton.text = "Retake Photo"
            }
            .addOnFailureListener {
                binding.statusText.text = "Could not process the photo. Try again."
                binding.takePhotoButton.isEnabled = true
            }
    }

    // Scans the full OCR text for known platform name keywords
    private fun detectPlatform(rawText: String): String? {
        val lower = rawText.lowercase()
        // Iterate longest keywords first so "game boy color" beats "game boy"
        for ((keyword, platform) in platformKeywords) {
            if (lower.contains(keyword)) return platform
        }
        return null
    }

    // Extracts the most likely game title, skipping lines that are the detected platform name
    private fun extractTitle(rawText: String, knownPlatform: String?): String? {
        // Build a set of words to exclude — noise + the detected platform's own keywords
        val platformNoise = knownPlatform?.lowercase()?.split(" ")?.toSet() ?: emptySet()
        val allNoise = noiseWords + platformNoise

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { line ->
                line.length >= 2 && line.any { it.isLetter() }
            }

        val scored = lines.map { line ->
            val words = line.split(" ", "-", ":", "\t").filter { it.isNotEmpty() }
            val noiseCount = words.count { allNoise.contains(it.lowercase()) }
            val realWordCount = words.size - noiseCount

            // Skip lines that are entirely made up of noise/platform words
            if (realWordCount == 0) return@map Pair(line, -1)

            // Also skip lines that look like they ARE the platform name
            val lineLower = line.lowercase()
            val isPlatformLine = platformKeywords.any { (keyword, _) -> lineLower == keyword }
            if (isPlatformLine) return@map Pair(line, -1)

            val score = (realWordCount * 3) - noiseCount + (line.length / 5)
            Pair(line, score)
        }

        val best = scored.filter { it.second > 0 }.maxByOrNull { it.second }
        return best?.first?.take(60)
    }

    private fun updatePlatformButton() {
        binding.platformButton.text = detectedPlatform ?: "Unknown — tap to set"
    }

    private fun showPlatformPicker() {
        val currentIndex = if (detectedPlatform == null) 0
            else allPlatforms.indexOf(detectedPlatform).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle("Set Console")
            .setSingleChoiceItems(allPlatforms, currentIndex) { dialog, index ->
                detectedPlatform = if (index == 0) null else allPlatforms[index]
                updatePlatformButton()
                dialog.dismiss()
            }
            .show()
    }

    private fun searchCatalog(query: String) {
        val platformLabel = detectedPlatform?.let { " on $it" } ?: ""
        binding.statusText.text = "Searching for \"$query\"$platformLabel..."
        binding.searchButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val results = repository.searchCatalog(query, detectedPlatform)
                if (results.isEmpty()) {
                    // If platform-scoped search found nothing, try without platform filter
                    if (detectedPlatform != null) {
                        val fallback = repository.searchCatalog(query, null)
                        if (fallback.isNotEmpty()) {
                            binding.statusText.text = "No ${detectedPlatform} results — showing all platforms"
                            showResultsPicker(fallback)
                            return@launch
                        }
                    }
                    binding.statusText.text = "No results. Edit the title or change the console and try again."
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
            .setItems(displayNames) { _, index -> navigateWithItem(results[index]) }
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
        val destination = when (item.type) {
            "CONSOLE"     -> R.id.action_photoScan_to_addEditConsole
            "COLLECTIBLE" -> R.id.action_photoScan_to_addEditCollectible
            else          -> R.id.action_photoScan_to_addEditGame
        }
        findNavController().navigate(destination, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recognizer.close()
        _binding = null
    }
}
