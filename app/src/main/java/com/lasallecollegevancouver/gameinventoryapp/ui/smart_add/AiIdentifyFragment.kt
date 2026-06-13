package com.lasallecollegevancouver.gameinventoryapp.ui.smart_add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAiIdentifyBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CatalogItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import kotlinx.coroutines.launch

class AiIdentifyFragment : Fragment() {

    private var _binding: FragmentAiIdentifyBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private var selectedPlatform: String? = null

    private val platformOptions = arrayOf(
        "Any Platform",
        "SNES", "N64", "Game Boy", "Game Boy Color", "GBA", "Virtual Boy",
        "GameCube", "Switch", "PS1", "PS2", "PS4", "PS5",
        "Genesis", "Saturn", "Dreamcast",
        "Atari 2600", "Atari 7800", "Jaguar", "Lynx", "Xbox"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAiIdentifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.platformFilterButton.setOnClickListener { showPlatformPicker() }

        binding.identifyTextButton.setOnClickListener {
            val query = binding.descriptionInput.text.toString().trim()
            if (query.isEmpty()) {
                binding.descriptionInput.error = "Enter a game name to search"
            } else {
                searchCatalog(query)
            }
        }

        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }
    }

    private fun showPlatformPicker() {
        val currentIndex = if (selectedPlatform == null) 0
            else platformOptions.indexOf(selectedPlatform).coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Platform")
            .setSingleChoiceItems(platformOptions, currentIndex) { dialog, index ->
                selectedPlatform = if (index == 0) null else platformOptions[index]
                binding.platformFilterButton.text = selectedPlatform ?: "Any Platform"
                dialog.dismiss()
            }
            .show()
    }

    private fun searchCatalog(query: String) {
        val platformLabel = selectedPlatform?.let { " on $it" } ?: ""
        showLoading("Searching for \"$query\"$platformLabel...")
        lifecycleScope.launch {
            try {
                val results = repository.searchCatalog(query, selectedPlatform)
                if (results.isEmpty()) {
                    showError("No results found. Try a broader search or change the platform.")
                } else {
                    showResults(results)
                }
            } catch (exception: Exception) {
                showError("Could not reach server. Check your connection.")
            }
        }
    }

    // Results dialog is scrollable by default when the list is long
    private fun showResults(results: List<CatalogItem>) {
        binding.statusText.visibility = View.GONE
        binding.identifyTextButton.isEnabled = true

        val displayNames = results.map { item ->
            "${item.title} (${item.platform})"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("${results.size} results — pick the right one")
            .setItems(displayNames) { _, index ->
                navigateWithItem(results[index])
            }
            .setNegativeButton("Cancel", null)
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
        findNavController().navigate(R.id.action_aiIdentify_to_addEditGame, bundle)
    }

    private fun showLoading(message: String) {
        binding.statusText.text = message
        binding.statusText.visibility = View.VISIBLE
        binding.identifyTextButton.isEnabled = false
    }

    private fun showError(message: String) {
        binding.statusText.text = message
        binding.statusText.visibility = View.VISIBLE
        binding.identifyTextButton.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
