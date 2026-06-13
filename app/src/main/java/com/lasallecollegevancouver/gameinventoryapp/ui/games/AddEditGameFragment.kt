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
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.UpdateItemRequest
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
    private var hasCalogPrices = false

    private var selectedPlatform = "SNES"
    private var selectedCondition = "CIB"

    private val platformOptions = arrayOf(
        "SNES", "N64", "Game Boy", "Game Boy Color", "GBA", "Virtual Boy",
        "GameCube", "Switch", "PS1", "PS2", "PS4", "PS5",
        "Genesis", "Saturn", "Dreamcast",
        "Atari 2600", "Atari 7800", "Jaguar", "Lynx", "Xbox"
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
        binding.saveButton.setOnClickListener { saveItem() }
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
                // Auto-update estimated value when condition changes and we have catalog prices
                if (hasCalogPrices) updateEstimatedValueFromCondition()
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

        // Lock title and platform — these come from the catalog and should not be changed
        binding.titleInput.setText(prefillTitle)
        binding.titleInput.isFocusable = false
        binding.titleInput.isClickable = false

        val prefillPlatform = args.getString("prefillPlatform", "")
        if (!prefillPlatform.isNullOrEmpty() && platformOptions.contains(prefillPlatform)) {
            selectedPlatform = prefillPlatform
        }
        binding.platformButton.isEnabled = false

        // Store catalog prices so condition changes auto-update the estimated value
        catalogLooseValue = args.getDouble("prefillLooseValue", 0.0)
        catalogCibValue = args.getDouble("prefillCibValue", 0.0)
        catalogNewValue = args.getDouble("prefillNewValue", 0.0)
        hasCalogPrices = catalogCibValue > 0.0

        val catalogId = args.getInt("prefillCatalogItemId", -1)
        if (catalogId != -1) prefillCatalogItemId = catalogId

        updateButtonLabels()
        updateEstimatedValueFromCondition()
    }

    // Maps the selected condition to the matching catalog price
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

        binding.saveButton.isEnabled = false
        lifecycleScope.launch {
            try {
                if (existingItem == null) {
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
                            notes = null,
                            forTrade = false
                        )
                    )
                } else {
                    repository.updateItem(
                        publicCode,
                        existingItem!!.id,
                        UpdateItemRequest(
                            condition = selectedCondition,
                            purchasePrice = purchasePrice,
                            estimatedValue = existingItem!!.estimatedValue,
                            notes = null,
                            forTrade = existingItem!!.forTrade
                        )
                    )
                }
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
