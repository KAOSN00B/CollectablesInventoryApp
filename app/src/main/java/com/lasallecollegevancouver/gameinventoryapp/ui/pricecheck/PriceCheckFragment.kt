package com.lasallecollegevancouver.gameinventoryapp.ui.pricecheck

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.Game
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentPriceCheckBinding
import com.lasallecollegevancouver.gameinventoryapp.network.PriceChartingProduct
import com.lasallecollegevancouver.gameinventoryapp.network.PriceChartingRepository
import kotlinx.coroutines.launch

// Fast price-lookup screen — for use while browsing a store to decide if a deal is worth it
// No save required; shows market prices and lets the user optionally add to collection or wishlist
class PriceCheckFragment : Fragment() {

    private var _binding: FragmentPriceCheckBinding? = null
    private val binding get() = _binding!!

    private val priceChartingRepository = PriceChartingRepository()
    private lateinit var database: AppDatabase

    // The last looked-up product — used by the action buttons below the result
    private var lastFoundProduct: PriceChartingProduct? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPriceCheckBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())

        binding.searchButton.setOnClickListener { performPriceSearch() }

        // Add to Collection — saves the found item as a game with the market price as estimated value
        binding.addToCollectionButton.setOnClickListener { addFoundItemToCollection() }

        // Add to Wishlist — navigates to the wishlist add form with prefilled data
        binding.addToWishlistButton.setOnClickListener { navigateToAddWishlist() }

        // Find Nearby — opens Google Maps with a relevant store search
        binding.findNearbyButton.setOnClickListener { openNearbyStoresSearch() }
    }

    // Searches PriceCharting by the typed query and displays the top result
    private fun performPriceSearch() {
        val query = binding.searchInput.text.toString().trim()
        if (query.isEmpty()) { binding.searchInput.error = "Enter an item name"; return }

        binding.searchButton.isEnabled = false
        binding.searchButton.text = "Searching..."
        binding.resultCard.visibility = View.GONE
        binding.actionButtonGroup.visibility = View.GONE

        lifecycleScope.launch {
            val results = priceChartingRepository.searchByName(query)
            val topResult = results.firstOrNull()

            if (topResult == null) {
                binding.resultTitle.text = "No results found for \"$query\""
                binding.resultCard.visibility = View.VISIBLE
            } else {
                lastFoundProduct = topResult
                displayResult(topResult, query)
            }

            binding.searchButton.isEnabled = true
            binding.searchButton.text = "Search"
        }
    }

    // Shows the market price breakdown and color-codes against the user's entered store price
    private fun displayResult(product: PriceChartingProduct, searchQuery: String) {
        binding.resultTitle.text = product.productName
        binding.resultPlatform.text = product.consoleName ?: ""

        // Show all available price tiers
        binding.priceLoose.text = "Loose: $${String.format("%.2f", product.loosePriceDollars())}"
        binding.priceComplete.text = "Complete: $${String.format("%.2f", product.completePriceDollars())}"
        binding.priceNew.text = "New / Sealed: $${String.format("%.2f", product.newPriceDollars())}"

        val marketPrice = product.bestPrice()
        val storePriceText = binding.storePriceInput.text.toString()
        val storePrice = storePriceText.toDoubleOrNull()

        if (storePrice != null && storePrice > 0.0) {
            val difference = marketPrice - storePrice
            when {
                difference > 0 -> {
                    // Store price is below market — green = good deal
                    binding.dealIndicator.text = "GOOD DEAL — $${String.format("%.2f", difference)} below market"
                    binding.dealIndicator.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                }
                difference < 0 -> {
                    // Store price is above market — red = overpriced
                    binding.dealIndicator.text = "OVERPRICED — $${String.format("%.2f", -difference)} above market"
                    binding.dealIndicator.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                else -> {
                    binding.dealIndicator.text = "AT MARKET VALUE"
                    binding.dealIndicator.setTextColor(resources.getColor(android.R.color.darker_gray, null))
                }
            }
            binding.dealIndicator.visibility = View.VISIBLE
        } else {
            binding.dealIndicator.visibility = View.GONE
        }

        binding.resultCard.visibility = View.VISIBLE
        binding.actionButtonGroup.visibility = View.VISIBLE
    }

    // Saves the found product to the Games collection with the market price as estimated value
    private fun addFoundItemToCollection() {
        val product = lastFoundProduct ?: return
        lifecycleScope.launch {
            val game = Game(
                title = product.productName,
                platform = product.consoleName ?: "Unknown",
                genre = "Other",
                condition = "Good",
                completionStatus = "Not Started",
                purchasePrice = binding.storePriceInput.text.toString().toDoubleOrNull() ?: 0.0,
                estimatedValue = product.bestPrice(),
                notes = "",
                priceChartingId = product.id,
                lastPriceCheck = System.currentTimeMillis()
            )
            database.gameDao().insertGame(game)
            binding.dealIndicator.text = "Added to your Games collection!"
            binding.dealIndicator.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            binding.dealIndicator.visibility = View.VISIBLE
        }
    }

    // Navigates to the wishlist add form with data from the found product prefilled
    private fun navigateToAddWishlist() {
        val product = lastFoundProduct ?: return
        val bundle = Bundle().apply {
            putString("prefillTitle", product.productName)
            putString("prefillPlatform", product.consoleName ?: "")
            putDouble("prefillEstimatedValue", product.bestPrice())
            product.id.let { putInt("prefillPriceChartingId", it) }
        }
        findNavController().navigate(R.id.action_priceCheck_to_wishlistList, bundle)
    }

    // Opens Google Maps searching for game or collectible stores nearby
    private fun openNearbyStoresSearch() {
        val searchQuery = Uri.encode("video game stores near me")
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$searchQuery"))
        mapsIntent.setPackage("com.google.android.apps.maps")
        if (mapsIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapsIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$searchQuery"))
            startActivity(browserIntent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
