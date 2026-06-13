package com.lasallecollegevancouver.gameinventoryapp.ui.wishlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAddEditWishlistBinding
import com.lasallecollegevancouver.gameinventoryapp.network.AddWishlistRequest
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.UpdateWishlistRequest
import com.lasallecollegevancouver.gameinventoryapp.network.WishlistItem
import kotlinx.coroutines.launch

class AddEditWishlistFragment : Fragment() {

    private var _binding: FragmentAddEditWishlistBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private var existingItem: WishlistItem? = null
    private var prefillCatalogItemId: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEditWishlistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // The "type" input (game/comic/tcg etc.) is not used in CollectOS — hide the TextInputLayout wrapper
        (binding.typeInput.parent?.parent as? ViewGroup)?.visibility = View.GONE

        val itemId = arguments?.getInt("wishlistItemId", -1) ?: -1
        if (itemId != -1) {
            loadExistingItem(itemId)
        } else {
            applyPrefill()
        }

        binding.saveButton.setOnClickListener { saveItem() }
    }

    private fun applyPrefill() {
        val args = arguments ?: return
        val prefillTitle = args.getString("prefillTitle") ?: return
        binding.titleInput.setText(prefillTitle)
        binding.platformInput.setText(args.getString("prefillPlatform") ?: "")
        val marketPrice = args.getDouble("prefillEstimatedValue", 0.0)
        if (marketPrice > 0.0) {
            binding.currentMarketPriceInput.setText(String.format("%.2f", marketPrice))
        }
        val catalogId = args.getInt("prefillCatalogItemId", -1)
        if (catalogId != -1) prefillCatalogItemId = catalogId
    }

    private fun loadExistingItem(itemId: Int) {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val items = repository.getWishlist(publicCode)
                existingItem = items.firstOrNull { it.id == itemId }
                existingItem?.let { item ->
                    binding.titleInput.setText(item.title)
                    binding.platformInput.setText(item.platform)
                    binding.targetPriceInput.setText(item.targetPrice.toString())
                    binding.currentMarketPriceInput.setText(item.currentEstimatedValue.toString())
                    binding.notesInput.setText(item.notes ?: "")
                }
            } catch (exception: Exception) {
                findNavController().popBackStack()
            }
        }
    }

    private fun saveItem() {
        val title = binding.titleInput.text.toString().trim()
        if (title.isEmpty()) { binding.titleInput.error = "Title is required"; return }

        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        val platform = binding.platformInput.text.toString().trim()
        val targetPrice = binding.targetPriceInput.text.toString().toDoubleOrNull() ?: 0.0
        val currentEstimatedValue = binding.currentMarketPriceInput.text.toString().toDoubleOrNull() ?: 0.0
        val notes = binding.notesInput.text.toString().trim().ifEmpty { null }

        binding.saveButton.isEnabled = false
        lifecycleScope.launch {
            try {
                if (existingItem == null) {
                    repository.addWishlistItem(
                        publicCode,
                        AddWishlistRequest(
                            catalogItemId = prefillCatalogItemId,
                            title = title,
                            platform = platform,
                            targetPrice = targetPrice,
                            currentEstimatedValue = currentEstimatedValue,
                            notes = notes,
                            isGrail = false
                        )
                    )
                } else {
                    repository.updateWishlistItem(
                        publicCode,
                        existingItem!!.id,
                        UpdateWishlistRequest(
                            targetPrice = targetPrice,
                            currentEstimatedValue = currentEstimatedValue,
                            notes = notes,
                            isGrail = existingItem!!.isGrail
                        )
                    )
                }
                findNavController().popBackStack()
            } catch (exception: Exception) {
                binding.saveButton.isEnabled = true
                binding.titleInput.error = "Could not save — check your connection"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
