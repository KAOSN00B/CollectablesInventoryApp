package com.lasallecollegevancouver.gameinventoryapp.ui.tcg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.BottomSheetOwnedTcgDetailBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.network.RetrofitClient
import com.lasallecollegevancouver.gameinventoryapp.ui.common.DisplayCaseBinder
import kotlinx.coroutines.launch

/**
 * Detail / viewer for a TCG card the user already owns.
 *
 * This is what now opens when a TCG item is tapped in the collection (previously that tap only
 * showed a "coming next" toast). It presents the card as a premium "slab on a backlit shelf"
 * (guide section 2B) and lets the user remove it from their collection.
 *
 * A saved [CollectionItem] of type TCG has no stored image URL, so we re-resolve the artwork by
 * searching the matching TCG API for the card's name and picking the printing whose externalId
 * matches the one we saved (falling back to the first result). RetrofitClient exposes a shared,
 * cached TcgRepository so this is cheap on repeat opens.
 */
class OwnedTcgDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetOwnedTcgDetailBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private val tcgRepository = RetrofitClient.tcgRepository

    companion object {
        private const val ARG_ITEM_ID = "itemId"

        // Other screens listen for this result key to refresh themselves after a delete.
        const val RESULT_KEY_CHANGED = "owned_tcg_changed"

        fun newInstance(itemId: Int): OwnedTcgDetailBottomSheet {
            return OwnedTcgDetailBottomSheet().apply {
                arguments = Bundle().apply { putInt(ARG_ITEM_ID, itemId) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetOwnedTcgDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemId = arguments?.getInt(ARG_ITEM_ID, -1) ?: -1
        val publicCode = PrefsHelper.getPublicCode(requireContext())
        if (itemId == -1 || publicCode == null) {
            dismiss()
            return
        }

        binding.closeButton.setOnClickListener { dismiss() }

        loadItem(publicCode, itemId)
    }

    // Fetch the owned item, fill in its text fields, then resolve and load its hero artwork.
    private fun loadItem(publicCode: String, itemId: Int) {
        lifecycleScope.launch {
            val item = try {
                repository.getItems(publicCode).firstOrNull { it.id == itemId }
            } catch (exception: Exception) {
                null
            }

            // Guard against the sheet being closed while the network call was in flight.
            if (_binding == null) return@launch

            if (item == null) {
                Snackbar.make(binding.root, "Could not load card — check your connection", Snackbar.LENGTH_LONG).show()
                return@launch
            }

            populateText(item)
            setupDeleteButton(publicCode, item)
            loadHeroArtwork(item)
        }
    }

    private fun populateText(item: CollectionItem) {
        binding.detailName.text = item.title

        // Set line: "<set> · #<number>" — fall back to the platform field which stores the set name.
        val setName = item.tcgSet ?: item.platform
        binding.detailSet.text = buildString {
            append(setName)
            if (!item.tcgCardNumber.isNullOrBlank()) append("  ·  #${item.tcgCardNumber}")
        }

        binding.detailRarity.text = item.tcgRarity.orEmpty()
        binding.detailRarity.visibility = if (item.tcgRarity.isNullOrBlank()) View.GONE else View.VISIBLE

        // One-line summary built only from the parts that actually apply to this copy.
        val metaParts = mutableListOf(item.condition)
        item.quantity?.let { if (it > 1) metaParts.add("Qty $it") }
        if (item.tcgIsFoil == true) metaParts.add("Foil")
        if (item.forTrade) metaParts.add("For Trade")
        binding.detailMeta.text = metaParts.joinToString("  ·  ")

        binding.detailValue.text = "$${String.format("%.2f", item.estimatedValue)}"

        if (!item.notes.isNullOrBlank()) {
            binding.detailNotes.text = "Notes: ${item.notes}"
            binding.detailNotes.visibility = View.VISIBLE
        } else {
            binding.detailNotes.visibility = View.GONE
        }
    }

    // Re-search the card's API by name and match the saved printing to recover its image URL.
    private fun loadHeroArtwork(item: CollectionItem) {
        lifecycleScope.launch {
            val results = try {
                when (item.tcgGame) {
                    "MTG"     -> tcgRepository.searchMtg(item.title)
                    "POKEMON" -> tcgRepository.searchPokemon(item.title)
                    "YUGIOH"  -> tcgRepository.searchYugioh(item.title)
                    else      -> tcgRepository.searchAll(item.title)
                }
            } catch (exception: Exception) {
                emptyList()
            }

            if (_binding == null) return@launch

            // Prefer the exact printing we saved; otherwise use the best (first) match.
            val match = results.firstOrNull { it.externalId == item.tcgExternalId }
                ?: results.firstOrNull()

            DisplayCaseBinder.loadArtwork(binding.heroImage, match?.imageUrl)
        }
    }

    private fun setupDeleteButton(publicCode: String, item: CollectionItem) {
        binding.deleteButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Remove Card")
                .setMessage("Remove \"${item.title}\" from your collection?")
                .setPositiveButton("Remove") { _, _ -> deleteItem(publicCode, item) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteItem(publicCode: String, item: CollectionItem) {
        lifecycleScope.launch {
            try {
                repository.deleteItem(publicCode, item.id)
                // Tell the host screen to refresh its list, then close the sheet.
                parentFragmentManager.setFragmentResult(RESULT_KEY_CHANGED, Bundle())
                dismiss()
            } catch (exception: Exception) {
                _binding?.let {
                    Snackbar.make(it.root, "Could not remove — check your connection", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
