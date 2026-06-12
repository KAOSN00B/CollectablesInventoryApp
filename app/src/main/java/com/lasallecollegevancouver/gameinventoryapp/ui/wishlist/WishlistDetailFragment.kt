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
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.Game
import com.lasallecollegevancouver.gameinventoryapp.data.WishlistItem
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentWishlistDetailBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WishlistDetailFragment : Fragment() {

    private var _binding: FragmentWishlistDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private var currentItem: WishlistItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())
        setupMenuItems()
        loadItem()

        // Add to Collection — converts the wishlist item into a real Game entry
        binding.addToCollectionButton.setOnClickListener { addToCollection() }

        // Find This Near Me — opens Google Maps with a store search for this specific item
        binding.findNearMeButton.setOnClickListener { openMapsSearch() }
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

    private fun loadItem() {
        val itemId = arguments?.getInt("wishlistItemId", -1) ?: -1
        if (itemId == -1) { findNavController().popBackStack(); return }
        lifecycleScope.launch {
            currentItem = database.wishlistDao().getWishlistItemById(itemId)
            currentItem?.let { displayItem(it) }
        }
    }

    private fun displayItem(item: WishlistItem) {
        binding.detailTitle.text = item.title
        binding.detailType.text = "${item.type} — ${item.platform}"
        binding.detailTargetPrice.text = "My Target Price: $${String.format("%.2f", item.targetPrice)}"
        binding.detailMarketPrice.text = "Current Market Price: $${String.format("%.2f", item.currentMarketPrice)}"
        binding.detailNotes.text = if (item.notes.isNotBlank()) "Notes: ${item.notes}" else ""
        val dateFormatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        binding.detailDateAdded.text = "Added to wishlist: ${dateFormatter.format(Date(item.dateAdded))}"
    }

    // Creates a Game entry from this wishlist item and then deletes the wishlist entry
    private fun addToCollection() {
        val item = currentItem ?: return
        lifecycleScope.launch {
            val newGame = Game(
                title = item.title,
                platform = item.platform,
                genre = "Other",
                condition = "Good",
                completionStatus = "Not Started",
                purchasePrice = item.targetPrice,
                estimatedValue = item.currentMarketPrice,
                notes = item.notes,
                priceChartingId = item.priceChartingId
            )
            database.gameDao().insertGame(newGame)
            database.wishlistDao().delete(item)
            Snackbar.make(requireView(), "${item.title} moved to your Games collection!", Snackbar.LENGTH_LONG).show()
            findNavController().popBackStack()
        }
    }

    // Opens Google Maps with a search for nearby stores that sell this type of item
    private fun openMapsSearch() {
        val item = currentItem ?: return
        val storeType = when (item.type.uppercase()) {
            "COMIC" -> "comic book stores"
            "TCG" -> "trading card game stores"
            "TOY" -> "toy stores"
            "LEGO" -> "LEGO stores"
            else -> "video game stores"
        }
        val searchQuery = Uri.encode("${item.title} $storeType near me")
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$searchQuery"))
        mapsIntent.setPackage("com.google.android.apps.maps")
        if (mapsIntent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(mapsIntent)
        } else {
            // Fallback to browser if Maps isn't installed
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
        val itemToDelete = currentItem ?: return
        lifecycleScope.launch {
            database.wishlistDao().delete(itemToDelete)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
