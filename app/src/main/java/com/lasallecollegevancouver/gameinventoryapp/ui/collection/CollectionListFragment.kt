package com.lasallecollegevancouver.gameinventoryapp.ui.collection

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

class CollectionListFragment : Fragment() {

    private var _binding: FragmentCollectionListBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private lateinit var collectionAdapter: CollectionEntryAdapter
    private var allItems: List<CollectionItem> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCollectionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        collectionAdapter = CollectionEntryAdapter { selectedItem ->
            val bundle = Bundle().apply { putInt("itemId", selectedItem.id) }
            when (selectedItem.type) {
                "GAME" -> findNavController().navigate(R.id.action_collectionList_to_gameDetail, bundle)
                "CONSOLE" -> findNavController().navigate(R.id.action_collectionList_to_consoleDetail, bundle)
            }
        }
        binding.collectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.collectionRecyclerView.adapter = collectionAdapter

        // CollectOS only has GAME and CONSOLE types — hide unused filter chips
        binding.chipComics.visibility = View.GONE
        binding.chipTcg.visibility = View.GONE
        binding.chipToys.visibility = View.GONE
        binding.chipLego.visibility = View.GONE

        binding.categoryChipGroup.setOnCheckedStateChangeListener { _, _ -> applyFilter() }

        binding.fabAddItem.setOnClickListener {
            SmartAddBottomSheet.newInstance().show(parentFragmentManager, "SmartAdd")
        }
        binding.emptyAddButton.setOnClickListener {
            SmartAddBottomSheet.newInstance().show(parentFragmentManager, "SmartAdd")
        }
    }

    override fun onResume() {
        super.onResume()
        loadItems()
    }

    private fun loadItems() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                allItems = repository.getItems(publicCode)
                applyFilter()
            } catch (exception: Exception) {
                binding.emptyState.visibility = View.VISIBLE
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (binding.categoryChipGroup.checkedChipId) {
            R.id.chip_games -> allItems.filter { it.type == "GAME" }
            R.id.chip_consoles -> allItems.filter { it.type == "CONSOLE" }
            else -> allItems
        }
        collectionAdapter.submitList(filtered)
        val total = filtered.sumOf { it.estimatedValue }
        binding.summaryText.text = "${filtered.size} items · $${String.format("%.2f", total)} estimated value"
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
