package com.lasallecollegevancouver.gameinventoryapp.ui.games

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAddEditGameBinding
import com.lasallecollegevancouver.gameinventoryapp.network.AddItemRequest
import com.lasallecollegevancouver.gameinventoryapp.network.Binder
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.UpdateItemRequest
import com.lasallecollegevancouver.gameinventoryapp.ui.binders.BinderPickerHelper
import kotlinx.coroutines.launch

class AddEditGameFragment : Fragment() {

    private var _binding: FragmentAddEditGameBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private var existingItem: CollectionItem? = null
    private var prefillCatalogItemId: Int? = null

    // Catalog prices passed from search — used to auto-set value when condition changes
    private var catalogLooseValue = 0.0
    private var catalogCibValue = 0.0
    private var catalogNewValue = 0.0
    private var hasCatalogPrices = false

    private var selectedPlatform = "SNES"
    private var selectedCondition = "CIB"
    private var selectedBinderId: Int? = null
    private var loadedBinders: List<Binder> = emptyList()

    private val platformOptions = arrayOf(
        "NES", "SNES", "N64", "GameCube", "Wii", "Switch",
        "Game Boy", "Game Boy Color", "GBA", "Virtual Boy", "DS", "3DS",
        "PS1", "PS2", "PS3", "PS4", "PS5", "PSP",
        "Xbox", "Xbox 360",
        "Genesis", "Saturn", "Dreamcast",
        "Atari 2600", "Atari 7800", "Jaguar", "Lynx"
    )
    private val conditionOptions = arrayOf("LOOSE", "CIB", "NEW", "POOR")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEditGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemId = arguments?.getInt("itemId", -1) ?: -1
        if (itemId != -1) {
            loadExistingItem(itemId)
        } else {
            applyPrefill()
        }

        setupDropdowns()
        setupBinderPicker()
        binding.saveButton.setOnClickListener { saveItem() }
    }

    private fun setupBinderPicker() {
        // Hide the binder row when editing an existing item — binder assignment is only for new items
        if (existingItem != null) {
            binding.binderPickerRow.visibility = android.view.View.GONE
            return
        }

        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return

        // Pre-load binders in the background so they appear instantly when the user taps
        lifecycleScope.launch {
            try {
                loadedBinders = repository.getBinders(publicCode)
            } catch (exception: Exception) {
                loadedBinders = emptyList()
            }
        }

        binding.binderPickerRow.setOnClickListener {
            BinderPickerHelper.showPicker(
                context = requireContext(),
                coroutineScope = lifecycleScope,
                binders = loadedBinders,
                publicCode = publicCode,
                repository = repository,
                currentBinderId = selectedBinderId
            ) { binderId, binderName ->
                selectedBinderId = binderId
                binding.binderPickerLabel.text = binderName ?: "None"
                lifecycleScope.launch {
                    try { loadedBinders = repository.getBinders(publicCode) } catch (_: Exception) {}
                }
            }
        }
    }

    private fun setupDropdowns() {
        updateButtonLabels()

        binding.platformButton.setOnClickListener {
            showSelectionDialog("Platform", platformOptions, selectedPlatform) { chosen ->
                selectedPlatform = chosen
                binding.platformButton.text = selectedPlatform
            }
        }
        binding.conditionButton.setOnClickListener {
            showSelectionDialog("Condition", conditionOptions, selectedCondition) { chosen ->
                selectedCondition = chosen
                binding.conditionButton.text = selectedCondition
                if (hasCatalogPrices) updateEstimatedValueFromCondition()
            }
        }
    }

    private fun showSelectionDialog(title: String, options: Array<String>, current: String, onSelected: (String) -> Unit) {
        val index = options.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(options, index) { dialog, i ->
                onSelected(options[i])
                dialog.dismiss()
            }
            .show()
    }

    private fun updateButtonLabels() {
        binding.platformButton.text = selectedPlatform
        binding.conditionButton.text = selectedCondition
    }

    private fun applyPrefill() {
        val args = arguments ?: return
        val prefillTitle = args.getString("prefillTitle") ?: return

        binding.titleInput.setText(prefillTitle)
        binding.titleInput.isFocusable = false
        binding.titleInput.isClickable = false

        val prefillPlatform = args.getString("prefillPlatform", "")
        if (!prefillPlatform.isNullOrEmpty() && platformOptions.contains(prefillPlatform)) {
            selectedPlatform = prefillPlatform
        }
        binding.platformButton.isEnabled = false

        catalogLooseValue = args.getDouble("prefillLooseValue", 0.0)
        catalogCibValue = args.getDouble("prefillCibValue", 0.0)
        catalogNewValue = args.getDouble("prefillNewValue", 0.0)
        hasCatalogPrices = catalogCibValue > 0.0

        val catalogId = args.getInt("prefillCatalogItemId", -1)
        if (catalogId != -1) prefillCatalogItemId = catalogId

        updateButtonLabels()
        updateEstimatedValueFromCondition()
    }

    private fun updateEstimatedValueFromCondition() {
        val value = when (selectedCondition) {
            "LOOSE" -> catalogLooseValue
            "CIB"   -> catalogCibValue
            "NEW"   -> catalogNewValue
            "POOR"  -> catalogLooseValue * 0.5
            else    -> catalogCibValue
        }
        binding.estimatedValueInput.setText(String.format("%.2f", value))
    }

    private fun loadExistingItem(itemId: Int) {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val items = repository.getItems(publicCode)
                existingItem = items.firstOrNull { it.id == itemId }
                existingItem?.let { item ->
                    binding.titleInput.setText(item.title)
                    binding.titleInput.isFocusable = false
                    binding.titleInput.isClickable = false
                    binding.platformButton.isEnabled = false
                    binding.purchasePriceInput.setText(item.purchasePrice.toString())
                    binding.estimatedValueInput.setText(item.estimatedValue.toString())
                    binding.forTradeSwitch.isChecked = item.forTrade
                    binding.notesInput.setText(item.notes ?: "")
                    selectedPlatform = item.platform
                    selectedCondition = item.condition
                    updateButtonLabels()
                }
            } catch (exception: Exception) {
                findNavController().popBackStack()
            }
        }
    }

    private fun saveItem() {
        val title = binding.titleInput.text.toString().trim()
        if (title.isEmpty()) {
            binding.titleInput.error = "Title is required"
            return
        }

        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        val purchasePrice = binding.purchasePriceInput.text.toString().toDoubleOrNull() ?: 0.0
        val estimatedValue = binding.estimatedValueInput.text.toString().toDoubleOrNull() ?: 0.0
        val forTrade = binding.forTradeSwitch.isChecked
        val notes = binding.notesInput.text.toString().trim().ifEmpty { null }

        // Only check for duplicates when adding a new item, not when editing
        if (existingItem == null) {
            binding.saveButton.isEnabled = false
            lifecycleScope.launch {
                try {
                    val existingItems = repository.getItems(publicCode)
                    val duplicate = findDuplicate(existingItems, title, selectedPlatform)
                    if (duplicate != null) {
                        binding.saveButton.isEnabled = true
                        showDuplicateWarning(duplicate.title, duplicate.platform) {
                            // User chose to add anyway
                            performSave(publicCode, title, purchasePrice, estimatedValue, notes, forTrade)
                        }
                    } else {
                        performSave(publicCode, title, purchasePrice, estimatedValue, notes, forTrade)
                    }
                } catch (exception: Exception) {
                    performSave(publicCode, title, purchasePrice, estimatedValue, notes, forTrade)
                }
            }
        } else {
            binding.saveButton.isEnabled = false
            performUpdate(publicCode, purchasePrice, estimatedValue, notes, forTrade)
        }
    }

    // Checks if the collection already has this game by catalogItemId (preferred) or title+platform
    private fun findDuplicate(items: List<CollectionItem>, title: String, platform: String): CollectionItem? {
        val catalogId = prefillCatalogItemId
        if (catalogId != null) {
            return items.firstOrNull { it.catalogItemId == catalogId }
        }
        return items.firstOrNull {
            it.title.equals(title, ignoreCase = true) && it.platform.equals(platform, ignoreCase = true)
        }
    }

    private fun showDuplicateWarning(title: String, platform: String, onAddAnyway: () -> Unit) {
        AlertDialog.Builder(requireContext())
            .setTitle("Already in Collection")
            .setMessage("You already own $title ($platform). Add another copy anyway?")
            .setPositiveButton("Add Anyway") { _, _ -> onAddAnyway() }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }

    private fun performSave(publicCode: String, title: String, purchasePrice: Double, estimatedValue: Double, notes: String?, forTrade: Boolean) {
        binding.saveButton.isEnabled = false
        lifecycleScope.launch {
            try {
                repository.addItem(
                    publicCode,
                    AddItemRequest(
                        catalogItemId = prefillCatalogItemId,
                        type = "GAME",
                        title = title,
                        platform = selectedPlatform,
                        condition = selectedCondition,
                        purchasePrice = purchasePrice,
                        estimatedValue = estimatedValue,
                        notes = notes,
                        forTrade = forTrade,
                        binderId = selectedBinderId
                    )
                )
                findNavController().popBackStack()
            } catch (exception: Exception) {
                binding.saveButton.isEnabled = true
                binding.titleInput.error = "Could not save — check your connection"
            }
        }
    }

    private fun performUpdate(publicCode: String, purchasePrice: Double, estimatedValue: Double, notes: String?, forTrade: Boolean) {
        lifecycleScope.launch {
            try {
                repository.updateItem(
                    publicCode,
                    existingItem!!.id,
                    UpdateItemRequest(
                        condition = selectedCondition,
                        purchasePrice = purchasePrice,
                        estimatedValue = estimatedValue,
                        notes = notes,
                        forTrade = forTrade
                    )
                )
                findNavController().popBackStack()
            } catch (exception: Exception) {
                binding.saveButton.isEnabled = true
                binding.titleInput.error = "Could not save — check your connection"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
