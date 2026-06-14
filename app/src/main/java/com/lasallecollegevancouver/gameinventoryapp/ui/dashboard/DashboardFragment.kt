package com.lasallecollegevancouver.gameinventoryapp.ui.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.AppConfig
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentDashboardBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonScan.setOnClickListener { findNavController().navigate(R.id.action_global_barcodeScan) }
        binding.buttonSearch.setOnClickListener { findNavController().navigate(R.id.action_global_search) }
        binding.buttonAdd.setOnClickListener {
            SmartAddBottomSheet.newInstance().show(parentFragmentManager, "SmartAdd")
        }
        binding.shareCollectionButton.setOnClickListener { showShareDialog() }
        binding.buttonBinders.setOnClickListener { findNavController().navigate(R.id.action_global_binders) }

        // TCG and Collectibles sections are hidden until items of those types exist
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        binding.loadingIndicator.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                fetchAndDisplayDashboard(publicCode)
            } catch (firstException: Exception) {
                // Render free tier cold-starts after inactivity — wait and retry once
                delay(5000)
                if (!isResumed) return@launch
                try {
                    fetchAndDisplayDashboard(publicCode)
                } catch (retryException: Exception) {
                    binding.grandTotalValue.text = "--"
                    binding.totalItemCount.text = "Could not load — check your connection"
                    binding.gamesCategoryValue.text = "--"
                }
            } finally {
                binding.loadingIndicator.visibility = View.GONE
            }
        }
    }

    private suspend fun fetchAndDisplayDashboard(publicCode: String) {
        val allItems = repository.getItems(publicCode)
        val wishlistItems = repository.getWishlist(publicCode)

        val games = allItems.filter { it.type == "GAME" }
        val consoles = allItems.filter { it.type == "CONSOLE" }
        val tcgCards = allItems.filter { it.type == "TCG" }
        val collectibles = allItems.filter { it.type == "COLLECTIBLE" }

        val gamesTotal = games.sumOf { it.estimatedValue }
        val consolesTotal = consoles.sumOf { it.estimatedValue }
        val videoGamesTotal = gamesTotal + consolesTotal

        val tcgTotal = tcgCards.sumOf { it.estimatedValue }
        val mtgCards = tcgCards.filter { it.tcgGame == "MTG" }
        val pokemonCards = tcgCards.filter { it.tcgGame == "POKEMON" }
        val yugiohCards = tcgCards.filter { it.tcgGame == "YUGIOH" }

        val collectiblesTotal = collectibles.sumOf { it.estimatedValue }
        val grandTotal = videoGamesTotal + tcgTotal + collectiblesTotal

        binding.grandTotalValue.text = "$${String.format("%.2f", grandTotal)}"
        binding.totalItemCount.text = "${allItems.size} items in collection"

        // Video Games row — combines games + consoles
        binding.gamesCategoryValue.text = "$${String.format("%.2f", videoGamesTotal)}"
        binding.gamesCategoryCount.text = "${games.size} games · ${consoles.size} consoles"

        // TCG section — only shown when cards exist
        if (tcgCards.isNotEmpty()) {
            binding.tcgCardSection.visibility = View.VISIBLE
            binding.tcgCategoryValue.text = "$${String.format("%.2f", tcgTotal)}"
            binding.tcgCategoryCount.text = "${tcgCards.size} cards"

            if (mtgCards.isNotEmpty()) {
                binding.tcgMtgRow.visibility = View.VISIBLE
                binding.tcgMtgValue.text = "$${String.format("%.2f", mtgCards.sumOf { it.estimatedValue })}"
            }
            if (pokemonCards.isNotEmpty()) {
                binding.tcgPokemonRow.visibility = View.VISIBLE
                binding.tcgPokemonValue.text = "$${String.format("%.2f", pokemonCards.sumOf { it.estimatedValue })}"
            }
            if (yugiohCards.isNotEmpty()) {
                binding.tcgYugiohRow.visibility = View.VISIBLE
                binding.tcgYugiohValue.text = "$${String.format("%.2f", yugiohCards.sumOf { it.estimatedValue })}"
            }
        } else {
            binding.tcgCardSection.visibility = View.GONE
        }

        // Collectibles section — only shown when collectibles exist
        if (collectibles.isNotEmpty()) {
            binding.collectiblesCardSection.visibility = View.VISIBLE
            binding.collectiblesCategoryValue.text = "$${String.format("%.2f", collectiblesTotal)}"
            binding.collectiblesCategoryCount.text = "${collectibles.size} items"
        } else {
            binding.collectiblesCardSection.visibility = View.GONE
        }

        binding.wishlistCount.text = "${wishlistItems.size} items on wishlist"

        val topFiveItems = allItems.sortedByDescending { it.estimatedValue }.take(5)
        binding.topValuableText.text = if (topFiveItems.isEmpty()) {
            "No items in your collection yet"
        } else {
            topFiveItems.mapIndexed { index, item ->
                "${index + 1}. ${item.title} (${item.platform}) — $${String.format("%.2f", item.estimatedValue)}"
            }.joinToString("\n")
        }

        val recentItems = allItems.sortedByDescending { it.createdAt }.take(5)
        binding.recentGamesText.text = if (recentItems.isEmpty()) {
            "No items added yet"
        } else {
            recentItems.joinToString("\n") { item ->
                "${item.title} (${item.platform}) — $${String.format("%.2f", item.estimatedValue)}"
            }
        }
    }

    private fun showShareDialog() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        val baseUrl = AppConfig.API_BASE_URL.trimEnd('/')
        val shareUrl = "$baseUrl/c/$publicCode"

        AlertDialog.Builder(requireContext())
            .setTitle("Share Your Collection")
            .setMessage("Anyone with this link can view your collection:\n\n$shareUrl")
            .setPositiveButton("Copy Link") { _, _ ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("CollectOS collection link", shareUrl))
                Toast.makeText(requireContext(), "Link copied!", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Share") { _, _ ->
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Check out my game collection on CollectOS: $shareUrl")
                }
                startActivity(Intent.createChooser(intent, "Share collection via"))
            }
            .setNegativeButton("Close", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
