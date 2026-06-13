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
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.UpdateItemRequest
import kotlinx.coroutines.launch

class AddEditConsoleFragment : Fragment() {

    private var _binding: FragmentAddEditConsoleBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private var existingItem: CollectionItem? = null

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
        _binding = FragmentAddEditConsoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemId = arguments?.getInt("itemId", -1) ?: -1
        if (itemId != -1) loadExistingItem(itemId)

        setupDropdowns()
        binding.saveButton.setOnClickListener { saveItem() }
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
                            catalogItemId = null,
                            type = "CONSOLE",
                            title = title,
                            platform = selectedPlatform,
                            condition = selectedCondition,
                            purchasePrice = purchasePrice,
                            estimatedValue = estimatedValue,
                            notes = notes,
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
