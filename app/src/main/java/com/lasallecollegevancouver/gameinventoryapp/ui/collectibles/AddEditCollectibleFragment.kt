package com.lasallecollegevancouver.gameinventoryapp.ui.collectibles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.Collectible
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAddEditCollectibleBinding
import kotlinx.coroutines.launch

// Handles add and edit for all 4 collectible types
// Conditionally shows/hides field groups based on the selected type
class AddEditCollectibleFragment : Fragment() {

    private var _binding: FragmentAddEditCollectibleBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private var existingCollectible: Collectible? = null
    private var prefillPriceChartingId: Int? = null

    private var selectedType = "COMIC"
    private var selectedCondition = "Good"

    private val typeOptions = arrayOf("COMIC", "TCG", "TOY", "LEGO")
    private val conditionOptions = arrayOf("New", "Like New", "Good", "Fair", "Poor")
    private val rarityOptions = arrayOf("Common", "Uncommon", "Rare", "Ultra Rare", "Secret Rare", "Other")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditCollectibleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())

        val collectibleId = arguments?.getInt("collectibleId", -1) ?: -1

        if (collectibleId != -1) {
            loadExistingCollectible(collectibleId)
        } else {
            applyPrefillBundle()
        }

        setupDropdownButtons()
        updateTypeFieldVisibility()
        binding.saveButton.setOnClickListener { saveCollectible() }
    }

    private fun applyPrefillBundle() {
        val args = arguments ?: return
        val prefillTitle = args.getString("prefillTitle") ?: return
        binding.nameInput.setText(prefillTitle)

        val prefillType = args.getString("prefillType", "")?.uppercase()
        if (!prefillType.isNullOrEmpty() && typeOptions.contains(prefillType)) {
            selectedType = prefillType
        }

        val prefillValue = args.getDouble("prefillEstimatedValue", 0.0)
        if (prefillValue > 0.0) {
            binding.estimatedValueInput.setText(String.format("%.2f", prefillValue))
        }

        val priceChartingId = args.getInt("prefillPriceChartingId", -1)
        if (priceChartingId != -1) prefillPriceChartingId = priceChartingId

        updateDropdownLabels()
        updateTypeFieldVisibility()
    }

    private fun setupDropdownButtons() {
        updateDropdownLabels()

        binding.typeButton.setOnClickListener {
            val currentIndex = typeOptions.indexOf(selectedType).coerceAtLeast(0)
            AlertDialog.Builder(requireContext())
                .setTitle("Type")
                .setSingleChoiceItems(typeOptions, currentIndex) { dialog, index ->
                    selectedType = typeOptions[index]
                    binding.typeButton.text = selectedType
                    updateTypeFieldVisibility()
                    dialog.dismiss()
                }
                .show()
        }

        binding.conditionButton.setOnClickListener {
            val currentIndex = conditionOptions.indexOf(selectedCondition).coerceAtLeast(0)
            AlertDialog.Builder(requireContext())
                .setTitle("Condition")
                .setSingleChoiceItems(conditionOptions, currentIndex) { dialog, index ->
                    selectedCondition = conditionOptions[index]
                    binding.conditionButton.text = selectedCondition
                    dialog.dismiss()
                }
                .show()
        }

        binding.rarityButton.setOnClickListener {
            val currentRarity = binding.rarityButton.text.toString()
            val currentIndex = rarityOptions.indexOf(currentRarity).coerceAtLeast(0)
            AlertDialog.Builder(requireContext())
                .setTitle("Rarity")
                .setSingleChoiceItems(rarityOptions, currentIndex) { dialog, index ->
                    binding.rarityButton.text = rarityOptions[index]
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun updateDropdownLabels() {
        binding.typeButton.text = selectedType
        binding.conditionButton.text = selectedCondition
    }

    // Shows only the field group relevant to the currently selected type
    private fun updateTypeFieldVisibility() {
        binding.comicFieldGroup.visibility = if (selectedType == "COMIC") View.VISIBLE else View.GONE
        binding.tcgFieldGroup.visibility = if (selectedType == "TCG") View.VISIBLE else View.GONE
        binding.toyFieldGroup.visibility = if (selectedType == "TOY") View.VISIBLE else View.GONE
        binding.legoFieldGroup.visibility = if (selectedType == "LEGO") View.VISIBLE else View.GONE
    }

    private fun loadExistingCollectible(collectibleId: Int) {
        lifecycleScope.launch {
            existingCollectible = database.collectibleDao().getById(collectibleId)
            existingCollectible?.let { collectible ->
                binding.nameInput.setText(collectible.name)
                binding.notesInput.setText(collectible.notes)
                binding.purchasePriceInput.setText(collectible.purchasePrice.toString())
                binding.estimatedValueInput.setText(collectible.estimatedValue.toString())

                selectedType = collectible.type
                selectedCondition = collectible.condition
                updateDropdownLabels()
                updateTypeFieldVisibility()

                // Fill in type-specific fields
                when (collectible.type) {
                    "COMIC" -> {
                        binding.issueNumberInput.setText(collectible.issueNumber ?: "")
                        binding.publisherInput.setText(collectible.publisher ?: "")
                        binding.seriesInput.setText(collectible.series ?: "")
                        binding.gradeInput.setText(collectible.grade ?: "")
                    }
                    "TCG" -> {
                        binding.cardSetInput.setText(collectible.cardSet ?: "")
                        binding.rarityButton.text = collectible.rarity ?: "Common"
                        binding.tcgGameInput.setText(collectible.tcgGame ?: "")
                        binding.quantityInput.setText(collectible.quantity?.toString() ?: "1")
                    }
                    "TOY" -> {
                        binding.franchiseInput.setText(collectible.franchise ?: "")
                        binding.brandInput.setText(collectible.brand ?: "")
                        binding.sealedCheckbox.isChecked = collectible.isSealed ?: false
                    }
                    "LEGO" -> {
                        binding.setNumberInput.setText(collectible.setNumber ?: "")
                        binding.themeInput.setText(collectible.theme ?: "")
                        binding.hasBoxCheckbox.isChecked = collectible.hasBox ?: false
                        binding.hasInstructionsCheckbox.isChecked = collectible.hasInstructions ?: false
                        binding.isCompleteCheckbox.isChecked = collectible.isComplete ?: false
                    }
                }
            }
        }
    }

    private fun saveCollectible() {
        val name = binding.nameInput.text.toString().trim()
        if (name.isEmpty()) { binding.nameInput.error = "Name is required"; return }

        val purchasePrice = binding.purchasePriceInput.text.toString().toDoubleOrNull() ?: 0.0
        val estimatedValue = binding.estimatedValueInput.text.toString().toDoubleOrNull() ?: 0.0
        val notes = binding.notesInput.text.toString().trim()

        val collectible = Collectible(
            id = existingCollectible?.id ?: 0,
            type = selectedType,
            name = name,
            condition = selectedCondition,
            purchasePrice = purchasePrice,
            estimatedValue = estimatedValue,
            notes = notes,
            dateAdded = existingCollectible?.dateAdded ?: System.currentTimeMillis(),
            priceChartingId = existingCollectible?.priceChartingId ?: prefillPriceChartingId,
            lastPriceCheck = existingCollectible?.lastPriceCheck,
            // Comic fields
            issueNumber = if (selectedType == "COMIC") binding.issueNumberInput.text.toString().trim().ifEmpty { null } else null,
            publisher = if (selectedType == "COMIC") binding.publisherInput.text.toString().trim().ifEmpty { null } else null,
            series = if (selectedType == "COMIC") binding.seriesInput.text.toString().trim().ifEmpty { null } else null,
            grade = if (selectedType == "COMIC") binding.gradeInput.text.toString().trim().ifEmpty { null } else null,
            // TCG fields
            cardSet = if (selectedType == "TCG") binding.cardSetInput.text.toString().trim().ifEmpty { null } else null,
            rarity = if (selectedType == "TCG") binding.rarityButton.text.toString() else null,
            tcgGame = if (selectedType == "TCG") binding.tcgGameInput.text.toString().trim().ifEmpty { null } else null,
            quantity = if (selectedType == "TCG") binding.quantityInput.text.toString().toIntOrNull() ?: 1 else null,
            // Toy fields
            franchise = if (selectedType == "TOY") binding.franchiseInput.text.toString().trim().ifEmpty { null } else null,
            brand = if (selectedType == "TOY") binding.brandInput.text.toString().trim().ifEmpty { null } else null,
            isSealed = if (selectedType == "TOY") binding.sealedCheckbox.isChecked else null,
            // LEGO fields
            setNumber = if (selectedType == "LEGO") binding.setNumberInput.text.toString().trim().ifEmpty { null } else null,
            theme = if (selectedType == "LEGO") binding.themeInput.text.toString().trim().ifEmpty { null } else null,
            hasBox = if (selectedType == "LEGO") binding.hasBoxCheckbox.isChecked else null,
            hasInstructions = if (selectedType == "LEGO") binding.hasInstructionsCheckbox.isChecked else null,
            isComplete = if (selectedType == "LEGO") binding.isCompleteCheckbox.isChecked else null
        )

        lifecycleScope.launch {
            if (existingCollectible == null) database.collectibleDao().insert(collectible)
            else database.collectibleDao().update(collectible)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
