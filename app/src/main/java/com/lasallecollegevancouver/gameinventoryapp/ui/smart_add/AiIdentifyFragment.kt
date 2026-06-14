package com.lasallecollegevancouver.gameinventoryapp.ui.smart_add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAiIdentifyBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CatalogItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.RetrofitClient
import kotlinx.coroutines.launch

class AiIdentifyFragment : Fragment() {

    private var _binding: FragmentAiIdentifyBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private lateinit var resultAdapter: CatalogSearchResultAdapter
    private var selectedPlatform: String? = null

    private val platformOptions = arrayOf(
        "Any Platform",
        "NES", "SNES", "N64", "GameCube", "Wii", "Switch",
        "Game Boy", "Game Boy Color", "GBA", "Virtual Boy", "DS", "3DS",
        "PS1", "PS2", "PS3", "PS4", "PS5", "PSP",
        "Xbox", "Xbox 360",
        "Genesis", "Saturn", "Dreamcast",
        "Atari 2600", "Atari 7800", "Jaguar", "Lynx"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAiIdentifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupResultsList()

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

    private fun setupResultsList() {
        resultAdapter = CatalogSearchResultAdapter(
            rawgRepository = RetrofitClient.rawgRepository,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onItemClick = ::navigateWithItem
        )
        binding.resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsRecycler.adapter = resultAdapter
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

    private fun showResults(results: List<CatalogItem>) {
        binding.statusText.text = "${results.size} results"
        binding.statusText.visibility = View.VISIBLE
        binding.identifyTextButton.isEnabled = true
        resultAdapter.submitList(results)
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
            "CONSOLE" -> R.id.action_aiIdentify_to_addEditConsole
            "COLLECTIBLE" -> R.id.action_aiIdentify_to_addEditCollectible
            else -> R.id.action_aiIdentify_to_addEditGame
        }
        findNavController().navigate(destination, bundle)
    }

    private fun showLoading(message: String) {
        binding.statusText.text = message
        binding.statusText.visibility = View.VISIBLE
        binding.identifyTextButton.isEnabled = false
        resultAdapter.submitList(emptyList())
    }

    private fun showError(message: String) {
        binding.statusText.text = message
        binding.statusText.visibility = View.VISIBLE
        binding.identifyTextButton.isEnabled = true
        resultAdapter.submitList(emptyList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
