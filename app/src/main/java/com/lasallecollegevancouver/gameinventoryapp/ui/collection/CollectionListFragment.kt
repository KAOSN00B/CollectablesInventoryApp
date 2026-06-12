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
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentCollectionListBinding
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
import kotlinx.coroutines.launch

class CollectionListFragment : Fragment() {

    private var _binding: FragmentCollectionListBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var adapter: CollectionAdapter
    private var allEntries: List<CollectionEntry> = emptyList()
    private var currentFilter = "ALL"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCollectionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())
        setupRecyclerView()
        setupFilters()
        binding.fabAddItem.setOnClickListener { showSmartAdd() }
        binding.emptyAddButton.setOnClickListener { showSmartAdd() }
    }

    override fun onResume() {
        super.onResume()
        loadCollection()
    }

    private fun setupRecyclerView() {
        adapter = CollectionAdapter { entry -> openEntry(entry) }
        binding.collectionRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.collectionRecyclerView.adapter = adapter
    }

    private fun setupFilters() {
        binding.categoryChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chip_games -> "GAME"
                R.id.chip_consoles -> "CONSOLE"
                R.id.chip_comics -> "COMIC"
                R.id.chip_tcg -> "TCG"
                R.id.chip_toys -> "TOY"
                R.id.chip_lego -> "LEGO"
                else -> "ALL"
            }
            applyFilter()
        }
    }

    private fun loadCollection() {
        lifecycleScope.launch {
            val games = database.gameDao().getAllGames().map { game ->
                CollectionEntry(
                    sourceId = game.id,
                    category = "GAME",
                    title = game.title,
                    meta = game.platform,
                    condition = "${game.condition} • ${game.completionStatus}",
                    estimatedValue = game.estimatedValue,
                    dateAdded = game.dateAdded
                )
            }
            val consoles = database.consoleDao().getAllConsoles().map { console ->
                CollectionEntry(
                    sourceId = console.id,
                    category = "CONSOLE",
                    title = console.name,
                    meta = listOf(console.brand, console.model).filter { it.isNotBlank() }.joinToString(" • "),
                    condition = console.condition,
                    estimatedValue = console.estimatedValue,
                    dateAdded = console.dateAdded
                )
            }
            val collectibles = database.collectibleDao().getAllCollectibles().map { collectible ->
                CollectionEntry(
                    sourceId = collectible.id,
                    category = collectible.type,
                    title = collectible.name,
                    meta = collectibleMeta(collectible.type, collectible.publisher, collectible.tcgGame, collectible.brand, collectible.theme),
                    condition = collectible.condition,
                    estimatedValue = collectible.estimatedValue,
                    dateAdded = collectible.dateAdded
                )
            }
            allEntries = (games + consoles + collectibles).sortedByDescending { it.dateAdded }
            applyFilter()
        }
    }

    private fun collectibleMeta(type: String, publisher: String?, tcgGame: String?, brand: String?, theme: String?): String {
        return when (type) {
            "COMIC" -> publisher ?: "Comic"
            "TCG" -> tcgGame ?: "Trading Card"
            "TOY" -> brand ?: "Toy / Figure"
            "LEGO" -> theme ?: "LEGO"
            else -> type
        }
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == "ALL") allEntries else allEntries.filter { it.category == currentFilter }
        adapter.submitList(filtered)
        binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        val total = filtered.sumOf { it.estimatedValue }
        binding.summaryText.text = "${filtered.size} items • $${String.format("%.2f", total)} estimated value"
    }

    private fun openEntry(entry: CollectionEntry) {
        val bundle = Bundle()
        when (entry.category) {
            "GAME" -> {
                bundle.putInt("gameId", entry.sourceId)
                findNavController().navigate(R.id.action_collectionList_to_gameDetail, bundle)
            }
            "CONSOLE" -> {
                bundle.putInt("consoleId", entry.sourceId)
                findNavController().navigate(R.id.action_collectionList_to_consoleDetail, bundle)
            }
            else -> {
                bundle.putInt("collectibleId", entry.sourceId)
                findNavController().navigate(R.id.action_collectionList_to_collectibleDetail, bundle)
            }
        }
    }

    private fun showSmartAdd() {
        SmartAddBottomSheet.newInstance().show(parentFragmentManager, "SmartAdd")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
