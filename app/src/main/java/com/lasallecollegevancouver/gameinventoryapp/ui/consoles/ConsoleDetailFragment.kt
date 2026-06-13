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
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentConsoleDetailBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import kotlinx.coroutines.launch

class ConsoleDetailFragment : Fragment() {

    private var _binding: FragmentConsoleDetailBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private var currentItem: CollectionItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConsoleDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        loadItem()
    }

    private fun setupMenu() {
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

    private fun loadItem() {
        val itemId = arguments?.getInt("itemId", -1) ?: -1
        val publicCode = PrefsHelper.getPublicCode(requireContext())
        if (itemId == -1 || publicCode == null) {
            findNavController().popBackStack()
            return
        }

        lifecycleScope.launch {
            try {
                val items = repository.getItems(publicCode)
                currentItem = items.firstOrNull { it.id == itemId }
                currentItem?.let { displayItem(it) } ?: findNavController().popBackStack()
            } catch (exception: Exception) {
                findNavController().popBackStack()
            }
        }
    }

    private fun displayItem(item: CollectionItem) {
        binding.detailName.text = item.title
        binding.detailBrand.text = "Platform: ${item.platform}"
        binding.detailCondition.text = "Condition: ${item.condition}"
        binding.detailPurchasePrice.text = "Purchase Price: $${String.format("%.2f", item.purchasePrice)}"
        binding.detailEstimatedValue.text = "Estimated Value: $${String.format("%.2f", item.estimatedValue)}"
        binding.detailNotes.text = if (!item.notes.isNullOrBlank()) "Notes: ${item.notes}" else ""
        binding.detailModel.text = if (item.forTrade) "Available for trade" else ""
        binding.detailDateAdded.visibility = View.GONE
        binding.detailLastPriceCheck.visibility = View.GONE
        binding.refreshPriceButton.visibility = View.GONE
    }

    private fun navigateToEdit() {
        val bundle = Bundle().apply { putInt("itemId", currentItem?.id ?: -1) }
        findNavController().navigate(R.id.action_consoleDetail_to_addEditConsole, bundle)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Console")
            .setMessage("Remove \"${currentItem?.title}\" from your collection?")
            .setPositiveButton("Delete") { _, _ -> deleteItem() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem() {
        val item = currentItem ?: return
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                repository.deleteItem(publicCode, item.id)
                Snackbar.make(requireView(), "${item.title} removed", Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            } catch (exception: Exception) {
                Snackbar.make(requireView(), "Could not delete — check your connection", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
