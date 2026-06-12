package com.lasallecollegevancouver.gameinventoryapp.ui.collectibles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.Collectible
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentCollectibleDetailBinding
import com.lasallecollegevancouver.gameinventoryapp.network.PriceChartingRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CollectibleDetailFragment : Fragment() {

    private var _binding: FragmentCollectibleDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private val priceChartingRepository = PriceChartingRepository()
    private var currentCollectible: Collectible? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectibleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())
        setupMenuItems()
        loadCollectible()
        binding.refreshPriceButton.setOnClickListener { refreshMarketPrice() }
    }

    private fun setupMenuItems() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_detail, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_edit -> { navigateToEdit(); true }
                    R.id.action_delete -> { showDeleteConfirmation(); true }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun loadCollectible() {
        val collectibleId = arguments?.getInt("collectibleId", -1) ?: -1
        if (collectibleId == -1) { findNavController().popBackStack(); return }
        lifecycleScope.launch {
            currentCollectible = database.collectibleDao().getById(collectibleId)
            currentCollectible?.let { displayCollectible(it) }
        }
    }

    private fun displayCollectible(collectible: Collectible) {
        binding.detailName.text = collectible.name
        binding.detailType.text = collectible.type
        binding.detailCondition.text = "Condition: ${collectible.condition}"
        binding.detailPurchasePrice.text = "Purchase Price: $${String.format("%.2f", collectible.purchasePrice)}"
        binding.detailEstimatedValue.text = "Estimated Value: $${String.format("%.2f", collectible.estimatedValue)}"
        binding.detailNotes.text = if (collectible.notes.isNotBlank()) "Notes: ${collectible.notes}" else ""

        val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.detailDateAdded.text = "Added: ${dateFormatter.format(Date(collectible.dateAdded))}"

        // Build a type-specific detail block showing the unique fields for this category
        binding.detailTypeSpecific.text = buildTypeSpecificDetails(collectible)

        if (collectible.lastPriceCheck != null) {
            binding.detailLastPriceCheck.text = "Price last updated: ${dateFormatter.format(Date(collectible.lastPriceCheck))}"
            binding.detailLastPriceCheck.visibility = View.VISIBLE
        } else {
            binding.detailLastPriceCheck.visibility = View.GONE
        }

        binding.refreshPriceButton.visibility =
            if (collectible.priceChartingId != null) View.VISIBLE else View.GONE
    }

    // Returns a formatted string of all type-specific fields for this collectible
    private fun buildTypeSpecificDetails(collectible: Collectible): String {
        return when (collectible.type) {
            "COMIC" -> listOfNotNull(
                collectible.publisher?.let { "Publisher: $it" },
                collectible.series?.let { "Series: $it" },
                collectible.issueNumber?.let { "Issue: #$it" },
                collectible.grade?.let { "Grade: $it" }
            ).joinToString("\n")

            "TCG" -> listOfNotNull(
                collectible.tcgGame?.let { "Game: $it" },
                collectible.cardSet?.let { "Set: $it" },
                collectible.rarity?.let { "Rarity: $it" },
                collectible.quantity?.let { "Quantity: $it" }
            ).joinToString("\n")

            "TOY" -> listOfNotNull(
                collectible.franchise?.let { "Franchise: $it" },
                collectible.brand?.let { "Brand: $it" },
                collectible.isSealed?.let { "Sealed: ${if (it) "Yes" else "No"}" }
            ).joinToString("\n")

            "LEGO" -> listOfNotNull(
                collectible.setNumber?.let { "Set #: $it" },
                collectible.theme?.let { "Theme: $it" },
                collectible.hasBox?.let { "Has Box: ${if (it) "Yes" else "No"}" },
                collectible.hasInstructions?.let { "Has Instructions: ${if (it) "Yes" else "No"}" },
                collectible.isComplete?.let { "Complete: ${if (it) "Yes" else "No"}" }
            ).joinToString("\n")

            else -> ""
        }
    }

    private fun refreshMarketPrice() {
        val collectible = currentCollectible ?: return
        val priceChartingId = collectible.priceChartingId ?: return

        binding.refreshPriceButton.isEnabled = false
        binding.refreshPriceButton.text = "Refreshing..."

        lifecycleScope.launch {
            val priceData = priceChartingRepository.refreshPrice(priceChartingId)
            if (priceData == null) {
                Snackbar.make(requireView(), "Price refresh failed — check your connection", Snackbar.LENGTH_SHORT).show()
                binding.refreshPriceButton.isEnabled = true
                binding.refreshPriceButton.text = "Refresh Market Price"
                return@launch
            }

            val oldValue = collectible.estimatedValue
            val newValue = priceData.bestPrice()
            val updatedCollectible = collectible.copy(estimatedValue = newValue, lastPriceCheck = System.currentTimeMillis())
            database.collectibleDao().update(updatedCollectible)
            currentCollectible = updatedCollectible
            displayCollectible(updatedCollectible)

            val changeText = when {
                newValue > oldValue -> "+$${String.format("%.2f", newValue - oldValue)}"
                newValue < oldValue -> "-$${String.format("%.2f", oldValue - newValue)}"
                else -> "no change"
            }
            Snackbar.make(requireView(), "Market price updated to $${String.format("%.2f", newValue)} ($changeText)", Snackbar.LENGTH_LONG).show()
            binding.refreshPriceButton.isEnabled = true
            binding.refreshPriceButton.text = "Refresh Market Price"
        }
    }

    private fun navigateToEdit() {
        val bundle = Bundle().apply { putInt("collectibleId", currentCollectible?.id ?: -1) }
        findNavController().navigate(R.id.action_collectibleDetail_to_addEditCollectible, bundle)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Collectible")
            .setMessage("Delete \"${currentCollectible?.name}\" from your collection?")
            .setPositiveButton("Delete") { _, _ -> deleteCollectible() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCollectible() {
        val collectibleToDelete = currentCollectible ?: return
        lifecycleScope.launch {
            database.collectibleDao().delete(collectibleToDelete)
            Snackbar.make(requireView(), "${collectibleToDelete.name} deleted", Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
