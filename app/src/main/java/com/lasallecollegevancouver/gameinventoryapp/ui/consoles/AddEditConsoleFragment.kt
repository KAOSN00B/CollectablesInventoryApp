package com.lasallecollegevancouver.gameinventoryapp.ui.consoles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.Console
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAddEditConsoleBinding
import kotlinx.coroutines.launch

class AddEditConsoleFragment : Fragment() {

    private var _binding: FragmentAddEditConsoleBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase

    // The console being edited — null when the user is adding a new console
    private var existingConsole: Console? = null

    // Set when the barcode scanner or AI flow pre-identifies the item
    private var prefillPriceChartingId: Int? = null

    // Tracks the currently chosen value for each dropdown field
    private var selectedBrand = "Sony"
    private var selectedCondition = "Good"

    private val brandOptions = arrayOf("Sony", "Microsoft", "Nintendo", "Sega", "Atari", "Other")
    private val conditionOptions = arrayOf("New", "Like New", "Good", "Fair", "Poor")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditConsoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())

        val consoleId = arguments?.getInt("consoleId", -1) ?: -1

        // Load existing data when editing; leave fields blank for a new console
        if (consoleId != -1) {
            loadExistingConsole(consoleId)
        } else {
            // Apply any pre-filled data from barcode scan or AI identification
            applyPrefillBundle()
        }

        setupDropdownButtons()
        binding.saveButton.setOnClickListener { saveConsole() }
    }

    // Wires each outlined button to open a single-choice AlertDialog
    private fun setupDropdownButtons() {
        updateButtonLabels()

        binding.brandButton.setOnClickListener {
            showSelectionDialog("Brand", brandOptions, selectedBrand) { selection ->
                selectedBrand = selection
                binding.brandButton.text = selectedBrand
            }
        }
        binding.conditionButton.setOnClickListener {
            showSelectionDialog("Condition", conditionOptions, selectedCondition) { selection ->
                selectedCondition = selection
                binding.conditionButton.text = selectedCondition
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
        binding.brandButton.text = selectedBrand
        binding.conditionButton.text = selectedCondition
    }

    // Applies name, brand, and estimated value from a barcode or AI result bundle
    private fun applyPrefillBundle() {
        val args = arguments ?: return
        val prefillTitle = args.getString("prefillTitle") ?: return

        binding.nameInput.setText(prefillTitle)

        val prefillPlatform = args.getString("prefillPlatform", "")
        if (!prefillPlatform.isNullOrEmpty() && brandOptions.contains(prefillPlatform)) {
            selectedBrand = prefillPlatform
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

    // Loads the existing console from Room and fills all form inputs with its current data
    private fun loadExistingConsole(consoleId: Int) {
        lifecycleScope.launch {
            existingConsole = database.consoleDao().getConsoleById(consoleId)
            existingConsole?.let { console ->
                binding.nameInput.setText(console.name)
                binding.modelInput.setText(console.model)
                binding.purchasePriceInput.setText(console.purchasePrice.toString())
                binding.estimatedValueInput.setText(console.estimatedValue.toString())
                binding.notesInput.setText(console.notes)

                // Restore the dropdown selections to what was previously saved
                selectedBrand = console.brand
                selectedCondition = console.condition
                updateButtonLabels()
            }
        }
    }

    // Validates required fields, builds the Console object, and inserts or updates it in Room
    private fun saveConsole() {
        val name = binding.nameInput.text.toString().trim()
        if (name.isEmpty()) {
            binding.nameInput.error = "Name is required"
            return
        }

        val model = binding.modelInput.text.toString().trim()
        val purchasePrice = binding.purchasePriceInput.text.toString().toDoubleOrNull() ?: 0.0
        val estimatedValue = binding.estimatedValueInput.text.toString().toDoubleOrNull() ?: 0.0
        val notes = binding.notesInput.text.toString().trim()

        val console = Console(
            // Use the existing id when editing; 0 tells Room to auto-generate an id for new consoles
            id = existingConsole?.id ?: 0,
            name = name,
            brand = selectedBrand,
            model = model,
            condition = selectedCondition,
            purchasePrice = purchasePrice,
            estimatedValue = estimatedValue,
            notes = notes,
            // Preserve the original add date when editing; record now when adding
            dateAdded = existingConsole?.dateAdded ?: System.currentTimeMillis(),
            // Preserve PriceCharting ID from edit or carry over from barcode/AI prefill
            priceChartingId = existingConsole?.priceChartingId ?: prefillPriceChartingId,
            lastPriceCheck = existingConsole?.lastPriceCheck
        )

        lifecycleScope.launch {
            if (existingConsole == null) {
                database.consoleDao().insertConsole(console)
            } else {
                database.consoleDao().updateConsole(console)
            }
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
