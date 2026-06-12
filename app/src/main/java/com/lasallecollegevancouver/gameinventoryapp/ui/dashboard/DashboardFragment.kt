package com.lasallecollegevancouver.gameinventoryapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentDashboardBinding
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
import kotlinx.coroutines.launch

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())
        binding.buttonScan.setOnClickListener { findNavController().navigate(R.id.action_global_barcodeScan) }
        binding.buttonSearch.setOnClickListener { findNavController().navigate(R.id.action_global_search) }
        binding.buttonAdd.setOnClickListener {
            SmartAddBottomSheet.newInstance().show(parentFragmentManager, "SmartAdd")
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            val gamesTotal = database.gameDao().getTotalValue()
            val gamesCount = database.gameDao().getCount()
            val consolesTotal = database.consoleDao().getTotalValue()
            val consolesCount = database.consoleDao().getCount()
            val comicsTotal = database.collectibleDao().getTotalValueByType("COMIC")
            val comicsCount = database.collectibleDao().getCountByType("COMIC")
            val tcgTotal = database.collectibleDao().getTotalValueByType("TCG")
            val tcgCount = database.collectibleDao().getCountByType("TCG")
            val toysTotal = database.collectibleDao().getTotalValueByType("TOY")
            val toysCount = database.collectibleDao().getCountByType("TOY")
            val legoTotal = database.collectibleDao().getTotalValueByType("LEGO")
            val legoCount = database.collectibleDao().getCountByType("LEGO")
            val recentGames = database.gameDao().getRecentGames()
            val wishlistCount = database.wishlistDao().getAllWishlistItems().size

            val grandTotal = gamesTotal + consolesTotal + comicsTotal + tcgTotal + toysTotal + legoTotal
            val totalCount = gamesCount + consolesCount + comicsCount + tcgCount + toysCount + legoCount

            binding.grandTotalValue.text = "$${String.format("%.2f", grandTotal)}"
            binding.totalItemCount.text = "$totalCount items in collection"

            binding.gamesCategoryValue.text = "Games  $${String.format("%.2f", gamesTotal)}"
            binding.gamesCategoryCount.text = "$gamesCount games"
            binding.consolesCategoryValue.text = "Consoles  $${String.format("%.2f", consolesTotal)}"
            binding.consolesCategoryCount.text = "$consolesCount consoles"
            binding.comicsCategoryValue.text = "Comics  $${String.format("%.2f", comicsTotal)}"
            binding.comicsCategoryCount.text = "$comicsCount comics"
            binding.tcgCategoryValue.text = "Trading Cards  $${String.format("%.2f", tcgTotal)}"
            binding.tcgCategoryCount.text = "$tcgCount cards"
            binding.toysCategoryValue.text = "Toys / Figures  $${String.format("%.2f", toysTotal)}"
            binding.toysCategoryCount.text = "$toysCount toys"
            binding.legoCategoryValue.text = "LEGO  $${String.format("%.2f", legoTotal)}"
            binding.legoCategoryCount.text = "$legoCount sets"
            binding.wishlistCount.text = "$wishlistCount items on wishlist"

            binding.recentGamesText.text = if (recentGames.isEmpty()) {
                "No games added yet"
            } else {
                recentGames.joinToString("\n") { game ->
                    "${game.title} (${game.platform}) - $${String.format("%.2f", game.estimatedValue)}"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
