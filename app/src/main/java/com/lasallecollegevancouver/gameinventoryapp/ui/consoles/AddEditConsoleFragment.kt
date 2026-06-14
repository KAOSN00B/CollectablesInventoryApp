package com.lasallecollegevancouver.gameinventoryapp.ui.consoles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAddEditConsoleBinding
import com.lasallecollegevancouver.gameinventoryapp.network.AddItemRequest
import com.lasallecollegevancouver.gameinventoryapp.network.Binder
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.UpdateItemRequest
import com.lasallecollegevancouver.gameinventoryapp.ui.binders.BinderPickerHelper
import kotlinx.coroutines.launch

class AddEditConsoleFragment : Fragment() {

    private var _binding: FragmentAddEditConsoleBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private var existingItem: CollectionItem? = null
    private var prefillCatalogItemId: Int? = null

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
        _binding = FragmentAddEditConsoleBinding.inflate(inflater, container, false)
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
        if (existingItem != null) {
            binding.binderPickerRow.visibility = android.view.View.GONE
            return
        }

        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return

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
        // The "Model" field doesn't apply to CollectOS — hide it
        binding.modelInput.visibility = View.GONE

        // brandButton is repurposed for platform selection in CollectOS
        binding.brandButton.setOnClickListener {
            showSelectionDialog("Platform", platformOptions, selectedPlatform) {
                selectedPlatform = it
                binding.brandButton.text = selectedPlatform
            }
        }
        binding.conditionButton.setOnClickListener {
            showSelectionDialog("Condition", conditionOptions, selectedCondition) {
                selectedCondition = it
                binding.conditionButton.text = selectedCondition
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
        binding.brandButton.text = selectedPlatform
        binding.conditionButton.text = selectedCondition
    }


    private fun applyPrefill() {
        val args = arguments ?: return
        val prefillTitle = args.getString("prefillTitle") ?: return

        binding.nameInput.setText(prefillTitle)
        binding.nameInput.isFocusable = false
        binding.nameInput.isClickable = false

        val prefillPlatform = args.getString("prefillPlatform", "")
        if (!prefillPlatform.isNullOrEmpty() && platformOptions.contains(prefillPlatform)) {
            selectedPlatform = prefillPlatform
        }
        binding.brandButton.isEnabled = false

        val catalogId = args.getInt("prefillCatalogItemId", -1)
        if (catalogId != -1) prefillCatalogItemId = catalogId

        val estimatedValue = args.getDouble("prefillCibValue", 0.0).takeIf { it > 0.0 }
            ?: args.getDouble("prefillLooseValue", 0.0).takeIf { it > 0.0 }
            ?: args.getDouble("prefillNewValue", 0.0)
        if (estimatedValue > 0.0) {
            binding.estimatedValueInput.setText(String.format("%.2f", estimatedValue))
        }

        updateButtonLabels()
    }
    private fun loadExistingItem(itemId: Int) {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val items = repository.getItems(publicCode)
                existingItem = items.firstOrNull { it.id == itemId }
                existingItem?.let { item ->
                    binding.nameInput.setText(item.title)
                    binding.purchasePriceInput.setText(item.purchasePrice.toString())
                    binding.estimatedValueInput.setText(item.estimatedValue.toString())
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
        val title = binding.nameInput.text.toString().trim()
        if (title.isEmpty()) {
            binding.nameInput.error = "Name is required"
            return
        }

        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        val purchasePrice = binding.purchasePriceInput.text.toString().toDoubleOrNull() ?: 0.0
        val estimatedValue = binding.estimatedValueInput.text.toString().toDoubleOrNull() ?: 0.0
        val notes = binding.notesInput.text.toString().trim().ifEmpty { null }

        binding.saveButton.isEnabled = false
        lifecycleScope.launch {
            try {
                if (existingItem == null) {
                    repository.addItem(
                        publicCode,
                        AddItemRequest(
                            catalogItemId = prefillCatalogItemId,
                            type = "CONSOLE",
                            title = title,
                            platform = selectedPlatform,
                            condition = selectedCondition,
                            purchasePrice = purchasePrice,
                            estimatedValue = estimatedValue,
                            notes = notes,
                            forTrade = false,
                            binderId = selectedBinderId
                        )
                    )
                } else {
                    repository.updateItem(
                        publicCode,
                        existingItem!!.id,
                        UpdateItemRequest(
                            condition = selectedCondition,
                            purchasePrice = purchasePrice,
                            estimatedValue = estimatedValue,
                            notes = notes,
                            forTrade = existingItem!!.forTrade
                        )
                    )
                }
                findNavController().popBackStack()
            } catch (exception: Exception) {
                binding.saveButton.isEnabled = true
                binding.nameInput.error = "Could not save — check your connection"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
