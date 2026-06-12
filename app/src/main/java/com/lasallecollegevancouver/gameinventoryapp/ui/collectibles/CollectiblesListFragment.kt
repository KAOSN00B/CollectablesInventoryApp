package com.lasallecollegevancouver.gameinventoryapp.ui.collectibles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.Collectible
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentCollectiblesListBinding
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
import kotlinx.coroutines.launch

// Shows all collectibles with a chip row filter: All | Comics | TCG | Toys | LEGO
class CollectiblesListFragment : Fragment() {

    private var _binding: FragmentCollectiblesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var collectibleAdapter: CollectibleAdapter

    // Full unfiltered list from Room
    private var fullCollectibleList: List<Collectible> = emptyList()

    // "ALL" means no filter — otherwise matches the type string stored in Room ("COMIC", "TCG", etc.)
    private var currentTypeFilter = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCollectiblesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())

        setupRecyclerView()
        setupChipFilters()

        // Open Smart Add bottom sheet when the + FAB is tapped
        binding.fabAddCollectible.setOnClickListener {
            SmartAddBottomSheet.newInstance(R.id.action_collectiblesList_to_addEditCollectible)
                .show(parentFragmentManager, "SmartAdd")
        }
    }

    override fun onResume() {
        super.onResume()
        loadCollectibles()
    }

    private fun setupRecyclerView() {
        collectibleAdapter = CollectibleAdapter { selectedCollectible ->
            val bundle = Bundle().apply { putInt("collectibleId", selectedCollectible.id) }
            findNavController().navigate(R.id.action_collectiblesList_to_collectibleDetail, bundle)
        }
        binding.collectiblesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.collectiblesRecyclerView.adapter = collectibleAdapter
    }

    // Wires up the type filter chips
    private fun setupChipFilters() {
        binding.chipAll.setOnClickListener { setTypeFilter("ALL") }
        binding.chipComics.setOnClickListener { setTypeFilter("COMIC") }
        binding.chipTcg.setOnClickListener { setTypeFilter("TCG") }
        binding.chipToys.setOnClickListener { setTypeFilter("TOY") }
        binding.chipLego.setOnClickListener { setTypeFilter("LEGO") }
    }

    private fun setTypeFilter(type: String) {
        currentTypeFilter = type
        applyFilter()
    }

    // Loads all collectibles from Room then applies the current chip filter
    private fun loadCollectibles() {
        lifecycleScope.launch {
            fullCollectibleList = database.collectibleDao().getAllCollectibles()
            applyFilter()
        }
    }

    private fun applyFilter() {
        val filteredList = if (currentTypeFilter == "ALL") {
            fullCollectibleList
        } else {
            fullCollectibleList.filter { collectible -> collectible.type == currentTypeFilter }
        }

        collectibleAdapter.submitList(filteredList)
        binding.emptyStateText.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
