package com.lasallecollegevancouver.gameinventoryapp.ui.games

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
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentGameDetailBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.RetrofitClient
import kotlinx.coroutines.launch

class GameDetailFragment : Fragment() {

    private var _binding: FragmentGameDetailBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private val rawgRepository = RetrofitClient.rawgRepository
    private var currentItem: CollectionItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGameDetailBinding.inflate(inflater, container, false)
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
                currentItem?.let {
                    displayItem(it)
                    loadCoverArt(it.title)
                } ?: findNavController().popBackStack()

                // Load community stats if this item came from the catalog
                currentItem?.catalogItemId?.let { catalogId ->
                    val stats = repository.getCommunityStats(catalogId)
                    stats?.let {
                        binding.detailLastPriceCheck.text =
                            "${it.ownedByCollectors} collectors own this · ${it.availableForTrade} for trade"
                        binding.detailLastPriceCheck.visibility = View.VISIBLE
                    }
                }
            } catch (exception: Exception) {
                findNavController().popBackStack()
            }
        }
    }

    private fun loadCoverArt(title: String) {
        lifecycleScope.launch {
            val url = rawgRepository.getCoverUrl(title) ?: return@launch
            if (_binding == null) return@launch
            binding.detailCoverArt.visibility = View.VISIBLE
            Glide.with(this@GameDetailFragment)
                .load(url)
                .centerCrop()
                .into(binding.detailCoverArt)
        }
    }

    private fun displayItem(item: CollectionItem) {
        binding.detailTitle.text = item.title
        binding.detailPlatform.text = "Platform: ${item.platform}"
        binding.detailCondition.text = "Condition: ${item.condition}"
        binding.detailPurchasePrice.text = "Purchase Price: $${String.format("%.2f", item.purchasePrice)}"
        binding.detailEstimatedValue.text = "Estimated Value: $${String.format("%.2f", item.estimatedValue)}"
        binding.detailNotes.text = if (!item.notes.isNullOrBlank()) "Notes: ${item.notes}" else ""
        binding.detailCompletion.text = if (item.forTrade) "Available for trade" else ""
        binding.detailCompletion.visibility = if (item.forTrade) View.VISIBLE else View.GONE

        // Hide the fields that no longer apply (PriceCharting era)
        binding.detailGenre.visibility = View.GONE
        binding.detailDateAdded.visibility = View.GONE
        binding.detailLastPriceCheck.visibility = View.GONE
        binding.refreshPriceButton.visibility = View.GONE
    }

    private fun navigateToEdit() {
        val bundle = Bundle().apply {
            putInt("itemId", currentItem?.id ?: -1)
        }
        findNavController().navigate(R.id.action_gameDetail_to_addEditGame, bundle)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Game")
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
