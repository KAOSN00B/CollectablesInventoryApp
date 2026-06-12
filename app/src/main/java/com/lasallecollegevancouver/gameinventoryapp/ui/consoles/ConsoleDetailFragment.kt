package com.lasallecollegevancouver.gameinventoryapp.ui.consoles

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
import com.lasallecollegevancouver.gameinventoryapp.data.Console
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentConsoleDetailBinding
import com.lasallecollegevancouver.gameinventoryapp.network.PriceChartingRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsoleDetailFragment : Fragment() {

    private var _binding: FragmentConsoleDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private val priceChartingRepository = PriceChartingRepository()

    // Holds the loaded console so Edit, Delete, and Refresh actions can reference it
    private var currentConsole: Console? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsoleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())
        setupMenuItems()
        loadConsole()

        // Refresh Market Price — only shown and active when we have a PriceCharting ID
        binding.refreshPriceButton.setOnClickListener { refreshMarketPrice() }
    }

    // Registers Edit and Delete toolbar menu items
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

    // Reads the consoleId from the Bundle and fetches the matching console from Room
    private fun loadConsole() {
        val consoleId = arguments?.getInt("consoleId", -1) ?: -1
        if (consoleId == -1) {
            findNavController().popBackStack()
            return
        }
        lifecycleScope.launch {
            currentConsole = database.consoleDao().getConsoleById(consoleId)
            currentConsole?.let { console -> displayConsole(console) }
        }
    }

    // Fills every TextView with the console's stored data
    private fun displayConsole(console: Console) {
        binding.detailName.text = console.name
        binding.detailBrand.text = "Brand: ${console.brand}"
        binding.detailModel.text = "Model: ${console.model}"
        binding.detailCondition.text = "Condition: ${console.condition}"
        binding.detailPurchasePrice.text = "Purchase Price: $${String.format("%.2f", console.purchasePrice)}"
        binding.detailEstimatedValue.text = "Estimated Value: $${String.format("%.2f", console.estimatedValue)}"
        binding.detailNotes.text = if (console.notes.isNotBlank()) "Notes: ${console.notes}" else ""

        // Convert stored epoch milliseconds to a human-readable date string
        val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.detailDateAdded.text = "Added: ${dateFormatter.format(Date(console.dateAdded))}"

        // Show the last price check date if available
        if (console.lastPriceCheck != null) {
            binding.detailLastPriceCheck.text = "Price last updated: ${dateFormatter.format(Date(console.lastPriceCheck))}"
            binding.detailLastPriceCheck.visibility = View.VISIBLE
        } else {
            binding.detailLastPriceCheck.visibility = View.GONE
        }

        // Only show the refresh button if this item has a PriceCharting ID to look up
        binding.refreshPriceButton.visibility =
            if (console.priceChartingId != null) View.VISIBLE else View.GONE
    }

    // Calls PriceCharting, updates estimatedValue in Room, and shows a Snackbar with the change
    private fun refreshMarketPrice() {
        val console = currentConsole ?: return
        val priceChartingId = console.priceChartingId ?: return

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

            val oldValue = console.estimatedValue
            val newValue = priceData.bestPrice()

            // Save the updated price and timestamp back to Room
            val updatedConsole = console.copy(
                estimatedValue = newValue,
                lastPriceCheck = System.currentTimeMillis()
            )
            database.consoleDao().updateConsole(updatedConsole)
            currentConsole = updatedConsole
            displayConsole(updatedConsole)

            val changeText = when {
                newValue > oldValue -> "+$${String.format("%.2f", newValue - oldValue)}"
                newValue < oldValue -> "-$${String.format("%.2f", oldValue - newValue)}"
                else -> "no change"
            }
            Snackbar.make(
                requireView(),
                "Market price updated to $${String.format("%.2f", newValue)} ($changeText)",
                Snackbar.LENGTH_LONG
            ).show()

            binding.refreshPriceButton.isEnabled = true
            binding.refreshPriceButton.text = "Refresh Market Price"
        }
    }

    // Navigates to AddEditConsoleFragment in edit mode, passing the current console's id
    private fun navigateToEdit() {
        val bundle = Bundle().apply { putInt("consoleId", currentConsole?.id ?: -1) }
        findNavController().navigate(R.id.action_consoleDetail_to_addEditConsole, bundle)
    }

    // Asks the user to confirm before permanently deleting the console
    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Console")
            .setMessage("Delete \"${currentConsole?.name}\" from your collection?")
            .setPositiveButton("Delete") { _, _ -> deleteConsole() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteConsole() {
        val consoleToDelete = currentConsole ?: return
        lifecycleScope.launch {
            database.consoleDao().deleteConsole(consoleToDelete)
            Snackbar.make(requireView(), "${consoleToDelete.name} deleted", Snackbar.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
