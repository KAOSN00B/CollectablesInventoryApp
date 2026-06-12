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
import com.lasallecollegevancouver.gameinventoryapp.network.PriceChartingProduct
import com.lasallecollegevancouver.gameinventoryapp.network.PriceChartingRepository
import kotlinx.coroutines.launch

// Search-by-name screen: user types an item name, we query PriceCharting, they pick from results,
// and the selected item's data pre-fills the add form.
// Replaces the Claude AI flow — same end result with no API cost.
class AiIdentifyFragment : Fragment() {

    private var _binding: FragmentAiIdentifyBinding? = null
    private val binding get() = _binding!!

    private val priceChartingRepository = PriceChartingRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiIdentifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.identifyTextButton.setOnClickListener {
            val query = binding.descriptionInput.text.toString().trim()
            if (query.isEmpty()) {
                binding.descriptionInput.error = "Enter an item name to search"
            } else {
                searchByName(query)
            }
        }

        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }
    }

    private fun searchByName(query: String) {
        showLoading("Searching for \"$query\"...")
        lifecycleScope.launch {
            val results = priceChartingRepository.searchByName(query)
            if (results.isEmpty()) {
                showError("No results found for \"$query\". Try a different name.")
                return@launch
            }
            showResults(results)
        }
    }

    // Shows a dialog listing the top search results so the user can pick the right one
    private fun showResults(results: List<PriceChartingProduct>) {
        binding.statusText.visibility = View.GONE
        binding.identifyTextButton.isEnabled = true

        val displayNames = results.take(10).map { product ->
            "${product.productName}${if (!product.consoleName.isNullOrEmpty()) " (${product.consoleName})" else ""}"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Select the right item")
            .setItems(displayNames) { _, selectedIndex ->
                val selectedProduct = results[selectedIndex]
                navigateWithProduct(selectedProduct)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Passes the selected product's data to the add game form via Bundle
    private fun navigateWithProduct(product: PriceChartingProduct) {
        val bundle = Bundle().apply {
            putString("prefillTitle", product.productName)
            putString("prefillPlatform", product.consoleName ?: "")
            putInt("prefillPriceChartingId", product.id)
            putDouble("prefillEstimatedValue", product.bestPrice())
        }
        // Default to the add game form — user can switch to console or collectible manually
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
