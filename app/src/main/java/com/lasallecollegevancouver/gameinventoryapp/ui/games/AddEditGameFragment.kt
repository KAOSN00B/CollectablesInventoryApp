package com.lasallecollegevancouver.gameinventoryapp.ui.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.Game
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAddEditGameBinding
import kotlinx.coroutines.launch

class AddEditGameFragment : Fragment() {

    private var _binding: FragmentAddEditGameBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase

    // The game being edited — null when the user is adding a new game
    private var existingGame: Game? = null

    // Set when the barcode scanner or AI flow pre-identifies the item
    private var prefillPriceChartingId: Int? = null

    // Tracks the currently chosen value for each dropdown field
    private var selectedPlatform = "PlayStation 5"
    private var selectedGenre = "Action"
    private var selectedCondition = "Good"
    private var selectedCompletionStatus = "Not Started"

    private val platformOptions = arrayOf("PlayStation 5", "Xbox Series X", "Nintendo Switch", "PC", "Other")
    private val genreOptions = arrayOf("Action", "RPG", "Sports", "Racing", "Shooter", "Platformer", "Strategy", "Horror", "Adventure", "Other")
    private val conditionOptions = arrayOf("New", "Like New", "Good", "Fair", "Poor")
    private val completionOptions = arrayOf("Not Started", "In Progress", "Completed", "100%")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())

        val gameId = arguments?.getInt("gameId", -1) ?: -1

        // Load existing data when editing; leave fields blank for a new game
        if (gameId != -1) {
            loadExistingGame(gameId)
        } else {
            // Apply any pre-filled data from barcode scan or AI identification
            applyPrefillBundle()
        }

        setupDropdownButtons()
        binding.saveButton.setOnClickListener { saveGame() }
    }

    // Wires each outlined button to open a single-choice AlertDialog
    private fun setupDropdownButtons() {
        updateButtonLabels()

        binding.platformButton.setOnClickListener {
            showSelectionDialog("Platform", platformOptions, selectedPlatform) { selection ->
                selectedPlatform = selection
                binding.platformButton.text = selectedPlatform
            }
        }
        binding.genreButton.setOnClickListener {
            showSelectionDialog("Genre", genreOptions, selectedGenre) { selection ->
                selectedGenre = selection
                binding.genreButton.text = selectedGenre
            }
        }
        binding.conditionButton.setOnClickListener {
            showSelectionDialog("Condition", conditionOptions, selectedCondition) { selection ->
                selectedCondition = selection
                binding.conditionButton.text = selectedCondition
            }
        }
        binding.completionButton.setOnClickListener {
            showSelectionDialog("Completion Status", completionOptions, selectedCompletionStatus) { selection ->
                selectedCompletionStatus = selection
                binding.completionButton.text = selectedCompletionStatus
            }
        }
    }

    // Reusable AlertDialog that shows a list of options and calls back with the chosen one
    private fun showSelectionDialog(
        title: String,
        options: Array<String>,
        currentValue: String,
        onSelected: (String) -> Unit
    ) {
        val currentIndex = options.indexOf(currentValue).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(options, currentIndex) { dialog, index ->
                onSelected(options[index])
                dialog.dismiss()
            }
            .show()
    }

    // Sets button labels to their default selected values
    private fun updateButtonLabels() {
        binding.platformButton.text = selectedPlatform
        binding.genreButton.text = selectedGenre
        binding.conditionButton.text = selectedCondition
        binding.completionButton.text = selectedCompletionStatus
    }

    // Applies title, platform, genre, estimated value from a barcode or AI result bundle
    private fun applyPrefillBundle() {
        val args = arguments ?: return
        val prefillTitle = args.getString("prefillTitle") ?: return

        binding.titleInput.setText(prefillTitle)

        val prefillPlatform = args.getString("prefillPlatform", "")
        if (!prefillPlatform.isNullOrEmpty() && platformOptions.contains(prefillPlatform)) {
            selectedPlatform = prefillPlatform
        }

        val prefillGenre = args.getString("prefillGenre", "")
        if (!prefillGenre.isNullOrEmpty() && genreOptions.contains(prefillGenre)) {
            selectedGenre = prefillGenre
        }

        val prefillValue = args.getDouble("prefillEstimatedValue", 0.0)
        if (prefillValue > 0.0) {
            binding.estimatedValueInput.setText(String.format("%.2f", prefillValue))
        }

        val priceChartingId = args.getInt("prefillPriceChartingId", -1)
        if (priceChartingId != -1) {
            prefillPriceChartingId = priceChartingId
        }

        updateButtonLabels()
    }

    // Loads the existing game from Room and fills all form inputs with its current data
    private fun loadExistingGame(gameId: Int) {
        lifecycleScope.launch {
            existingGame = database.gameDao().getGameById(gameId)
            existingGame?.let { game ->
                binding.titleInput.setText(game.title)
                binding.purchasePriceInput.setText(game.purchasePrice.toString())
                binding.estimatedValueInput.setText(game.estimatedValue.toString())
                binding.notesInput.setText(game.notes)

                // Restore the dropdown selections to what was previously saved
                selectedPlatform = game.platform
                selectedGenre = game.genre
                selectedCondition = game.condition
                selectedCompletionStatus = game.completionStatus
                updateButtonLabels()
            }
        }
    }

    // Validates required fields, builds the Game object, and inserts or updates it in Room
    private fun saveGame() {
        val title = binding.titleInput.text.toString().trim()
        if (title.isEmpty()) {
            binding.titleInput.error = "Title is required"
            return
        }

        val purchasePrice = binding.purchasePriceInput.text.toString().toDoubleOrNull() ?: 0.0
        val estimatedValue = binding.estimatedValueInput.text.toString().toDoubleOrNull() ?: 0.0
        val notes = binding.notesInput.text.toString().trim()

        val game = Game(
            // Use the existing id when editing; 0 tells Room to auto-generate an id for new games
            id = existingGame?.id ?: 0,
            title = title,
            platform = selectedPlatform,
            genre = selectedGenre,
            condition = selectedCondition,
            completionStatus = selectedCompletionStatus,
            purchasePrice = purchasePrice,
            estimatedValue = estimatedValue,
            notes = notes,
            // Preserve the original add date when editing; record now when adding
            dateAdded = existingGame?.dateAdded ?: System.currentTimeMillis(),
            // Preserve PriceCharting ID from edit or carry over from barcode/AI prefill
            priceChartingId = existingGame?.priceChartingId ?: prefillPriceChartingId,
            lastPriceCheck = existingGame?.lastPriceCheck
        )

        lifecycleScope.launch {
            if (existingGame == null) {
                database.gameDao().insertGame(game)
            } else {
                database.gameDao().updateGame(game)
            }
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
