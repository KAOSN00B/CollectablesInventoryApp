package com.lasallecollegevancouver.gameinventoryapp.ui.collection

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentCollectionListBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
import kotlinx.coroutines.launch

// Sort options available from the sort picker dialog
enum class SortOrder { RECENT, VALUE_DESC, A_TO_Z, PLATFORM }

class CollectionListFragment : Fragment() {

    private var _binding: FragmentCollectionListBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private lateinit var collectionAdapter: CollectionEntryAdapter
    private var allItems: List<CollectionItem> = emptyList()
    private var currentSortOrder = SortOrder.RECENT
    private var searchQuery = ""

    companion object {
        // Survives configuration changes; acts as offline cache when network is unavailable
        private var itemCache: List<CollectionItem> = emptyList()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCollectionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectionAdapter = CollectionEntryAdapter { selectedItem ->
            val bundle = Bundle().apply { putInt("itemId", selectedItem.id) }
            when (selectedItem.type) {
                "GAME"    -> findNavController().navigate(R.id.action_collectionList_to_gameDetail, bundle)
                "CONSOLE" -> findNavController().navigate(R.id.action_collectionList_to_consoleDetail, bundle)
                else -> Toast.makeText(requireContext(), "${selectedItem.type} detail view is coming next", Toast.LENGTH_SHORT).show()
            }
        }
        binding.collectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.collectionRecyclerView.adapter = collectionAdapter

        // Comics, Toys, LEGO not implemented yet — hide those chips
        binding.chipComics.visibility = View.GONE
        binding.chipToys.visibility = View.GONE
        binding.chipLego.visibility = View.GONE

        binding.categoryChipGroup.setOnCheckedStateChangeListener { _, _ -> applyFilter() }

        binding.sortButton.setOnClickListener { showSortPicker() }

        // Filter list as the user types — searches title and platform
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                searchQuery = editable?.toString() ?: ""
                applyFilter()
            }
        })

        binding.fabAddItem.setOnClickListener {
            SmartAddBottomSheet.newInstance().show(parentFragmentManager, "SmartAdd")
        }
        binding.emptyAddButton.setOnClickListener {
            SmartAddBottomSheet.newInstance().show(parentFragmentManager, "SmartAdd")
        }

        binding.swipeRefresh.setOnRefreshListener { loadItems() }
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun loadItems() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        binding.loadingIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val freshItems = repository.getItems(publicCode)
                allItems = freshItems
                itemCache = freshItems
                binding.cachedDataBanner.visibility = View.GONE
                applyFilter()
            } catch (exception: Exception) {
                // Show cached data rather than an empty screen when the network is down
                if (itemCache.isNotEmpty()) {
                    allItems = itemCache
                    binding.cachedDataBanner.visibility = View.VISIBLE
                    applyFilter()
                } else {
                    binding.emptyState.visibility = View.VISIBLE
                }
            } finally {
                binding.loadingIndicator.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun applyFilter() {
        // Step 1 — filter by category chip
        val byType = when (binding.categoryChipGroup.checkedChipId) {
            R.id.chip_games    -> allItems.filter { it.type == "GAME" }
            R.id.chip_consoles -> allItems.filter { it.type == "CONSOLE" }
            R.id.chip_tcg      -> allItems.filter { it.type == "TCG" }
            else               -> allItems
        }

        // Step 2 — filter by search query (title or platform)
        val filtered = if (searchQuery.isBlank()) byType else {
            byType.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.platform.contains(searchQuery, ignoreCase = true)
            }
        }

        // Step 3 — sort
        val sorted = when (currentSortOrder) {
            SortOrder.RECENT     -> filtered.sortedByDescending { it.id }
            SortOrder.VALUE_DESC -> filtered.sortedByDescending { it.estimatedValue }
            SortOrder.A_TO_Z     -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.PLATFORM   -> filtered.sortedWith(
                compareBy({ it.platform.lowercase() }, { it.title.lowercase() })
            )
        }

        collectionAdapter.submitList(sorted)
        val total = filtered.sumOf { it.estimatedValue }
        binding.summaryText.text = "${filtered.size} items · $${String.format("%.2f", total)} estimated value"
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showSortPicker() {
        val options = arrayOf("Recent (newest first)", "Value: High to Low", "Title: A to Z", "Platform")
        AlertDialog.Builder(requireContext())
            .setTitle("Sort by")
            .setSingleChoiceItems(options, currentSortOrder.ordinal) { dialog, which ->
                currentSortOrder = SortOrder.entries[which]
                updateSortButtonLabel()
                applyFilter()
                dialog.dismiss()
            }
            .show()
    }

    private fun updateSortButtonLabel() {
        binding.sortButton.text = when (currentSortOrder) {
            SortOrder.RECENT     -> "Sort: Recent"
            SortOrder.VALUE_DESC -> "Sort: Value ↓"
            SortOrder.A_TO_Z     -> "Sort: A–Z"
            SortOrder.PLATFORM   -> "Sort: Platform"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
