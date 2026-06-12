package com.lasallecollegevancouver.gameinventoryapp.ui.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.WishlistItem
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAddEditWishlistBinding
import kotlinx.coroutines.launch

class AddEditWishlistFragment : Fragment() {

    private var _binding: FragmentAddEditWishlistBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private var existingItem: WishlistItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())

        val itemId = arguments?.getInt("wishlistItemId", -1) ?: -1
        if (itemId != -1) loadExistingItem(itemId)

        // Apply prefill from Price Check screen if present
        arguments?.getString("prefillTitle")?.let { title ->
            binding.titleInput.setText(title)
            binding.platformInput.setText(arguments?.getString("prefillPlatform") ?: "")
            val marketPrice = arguments?.getDouble("prefillEstimatedValue", 0.0) ?: 0.0
            if (marketPrice > 0.0) binding.currentMarketPriceInput.setText(String.format("%.2f", marketPrice))
        }

        binding.saveButton.setOnClickListener { saveItem() }
    }

    private fun loadExistingItem(itemId: Int) {
        lifecycleScope.launch {
            existingItem = database.wishlistDao().getWishlistItemById(itemId)
            existingItem?.let { item ->
                binding.titleInput.setText(item.title)
                binding.typeInput.setText(item.type)
                binding.platformInput.setText(item.platform)
                binding.targetPriceInput.setText(item.targetPrice.toString())
                binding.currentMarketPriceInput.setText(item.currentMarketPrice.toString())
                binding.notesInput.setText(item.notes)
            }
        }
    }

    private fun saveItem() {
        val title = binding.titleInput.text.toString().trim()
        if (title.isEmpty()) { binding.titleInput.error = "Title is required"; return }

        val wishlistItem = WishlistItem(
            id = existingItem?.id ?: 0,
            title = title,
            type = binding.typeInput.text.toString().trim().ifEmpty { "game" },
            platform = binding.platformInput.text.toString().trim(),
            targetPrice = binding.targetPriceInput.text.toString().toDoubleOrNull() ?: 0.0,
            currentMarketPrice = binding.currentMarketPriceInput.text.toString().toDoubleOrNull() ?: 0.0,
            notes = binding.notesInput.text.toString().trim(),
            dateAdded = existingItem?.dateAdded ?: System.currentTimeMillis(),
            priceChartingId = existingItem?.priceChartingId
        )

        lifecycleScope.launch {
            if (existingItem == null) database.wishlistDao().insert(wishlistItem)
            else database.wishlistDao().update(wishlistItem)
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
