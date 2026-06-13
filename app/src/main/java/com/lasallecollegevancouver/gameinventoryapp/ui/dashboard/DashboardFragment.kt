package com.lasallecollegevancouver.gameinventoryapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentDashboardBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
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

        // CollectOS only has GAME and CONSOLE types; hide unused category rows
        binding.comicsCategoryValue.visibility = View.GONE
        binding.comicsCategoryCount.visibility = View.GONE
        binding.tcgCategoryValue.visibility = View.GONE
        binding.tcgCategoryCount.visibility = View.GONE
        binding.toysCategoryValue.visibility = View.GONE
        binding.toysCategoryCount.visibility = View.GONE
        binding.legoCategoryValue.visibility = View.GONE
        binding.legoCategoryCount.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val allItems = repository.getItems(publicCode)
                val wishlistItems = repository.getWishlist(publicCode)

                val games = allItems.filter { it.type == "GAME" }
                val consoles = allItems.filter { it.type == "CONSOLE" }

                val gamesTotal = games.sumOf { it.estimatedValue }
                val consolesTotal = consoles.sumOf { it.estimatedValue }
                val grandTotal = gamesTotal + consolesTotal
                val totalCount = allItems.size

                binding.grandTotalValue.text = "$${String.format("%.2f", grandTotal)}"
                binding.totalItemCount.text = "$totalCount items in collection"

                binding.gamesCategoryValue.text = "Games  $${String.format("%.2f", gamesTotal)}"
                binding.gamesCategoryCount.text = "${games.size} games"
                binding.consolesCategoryValue.text = "Consoles  $${String.format("%.2f", consolesTotal)}"
                binding.consolesCategoryCount.text = "${consoles.size} consoles"

                binding.wishlistCount.text = "${wishlistItems.size} items on wishlist"

                val recentGames = allItems.sortedByDescending { it.createdAt }.take(5)
                binding.recentGamesText.text = if (recentGames.isEmpty()) {
                    "No items added yet"
                } else {
                    recentGames.joinToString("\n") { item ->
                        "${item.title} (${item.platform}) — $${String.format("%.2f", item.estimatedValue)}"
                    }
                }
            } catch (exception: Exception) {
                binding.grandTotalValue.text = "$0.00"
                binding.totalItemCount.text = "Could not load — check your connection"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
