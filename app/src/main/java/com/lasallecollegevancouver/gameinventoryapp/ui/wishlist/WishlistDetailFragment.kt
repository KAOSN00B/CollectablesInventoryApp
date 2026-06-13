package com.lasallecollegevancouver.gameinventoryapp.ui.wishlist

import android.content.Intent
import android.net.Uri
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
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentWishlistDetailBinding
import com.lasallecollegevancouver.gameinventoryapp.network.AddItemRequest
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.WishlistItem
import kotlinx.coroutines.launch

class WishlistDetailFragment : Fragment() {

    private var _binding: FragmentWishlistDetailBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private var currentItem: WishlistItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWishlistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        loadItem()

        // Move the wishlist item into the main collection as a GAME entry
        binding.addToCollectionButton.setOnClickListener { addToCollection() }

        // Search for nearby game stores that might carry this title
        binding.findNearMeButton.setOnClickListener { openMapsSearch() }
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
        val itemId = arguments?.getInt("wishlistItemId", -1) ?: -1
        val publicCode = PrefsHelper.getPublicCode(requireContext())
        if (itemId == -1 || publicCode == null) { findNavController().popBackStack(); return }

        lifecycleScope.launch {
            try {
                val items = repository.getWishlist(publicCode)
                currentItem = items.firstOrNull { it.id == itemId }
                currentItem?.let { displayItem(it) } ?: findNavController().popBackStack()
            } catch (exception: Exception) {
                findNavController().popBackStack()
            }
        }
    }

    private fun displayItem(item: WishlistItem) {
        binding.detailTitle.text = "${if (item.isGrail) "★ " else ""}${item.title}"
        binding.detailType.text = item.platform
        binding.detailTargetPrice.text = "My Target Price: $${String.format("%.2f", item.targetPrice)}"
        binding.detailMarketPrice.text = "Catalog Value: $${String.format("%.2f", item.currentEstimatedValue)}"
        binding.detailNotes.text = if (!item.notes.isNullOrBlank()) "Notes: ${item.notes}" else ""
        binding.detailDateAdded.visibility = View.GONE
    }

    // Adds the wishlist item to the real collection, then removes it from the wishlist
    private fun addToCollection() {
        val item = currentItem ?: return
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                repository.addItem(
                    publicCode,
                    AddItemRequest(
                        catalogItemId = item.catalogItemId,
                        type = "GAME",
                        title = item.title,
                        platform = item.platform,
                        condition = "LOOSE",
                        purchasePrice = item.targetPrice,
                        estimatedValue = item.currentEstimatedValue,
                        notes = item.notes,
                        forTrade = false
                    )
                )
                repository.deleteWishlistItem(publicCode, item.id)
                Snackbar.make(requireView(), "${item.title} moved to your collection!", Snackbar.LENGTH_LONG).show()
                findNavController().popBackStack()
            } catch (exception: Exception) {
                Snackbar.make(requireView(), "Could not move item — check your connection", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    // Opens Google Maps with a search for nearby video game stores
    private fun openMapsSearch() {
        val item = currentItem ?: return
        val searchQuery = Uri.encode("${item.title} video game stores near me")
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$searchQuery"))
        mapsIntent.setPackage("com.google.android.apps.maps")
        if (mapsIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapsIntent)
        } else {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=$searchQuery"))
            startActivity(browserIntent)
        }
    }

    private fun navigateToEdit() {
        val bundle = Bundle().apply { putInt("wishlistItemId", currentItem?.id ?: -1) }
        findNavController().navigate(R.id.action_wishlistDetail_to_addEditWishlist, bundle)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove from Wishlist")
            .setMessage("Remove \"${currentItem?.title}\" from your wishlist?")
            .setPositiveButton("Remove") { _, _ -> deleteItem() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteItem() {
        val item = currentItem ?: return
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                repository.deleteWishlistItem(publicCode, item.id)
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
