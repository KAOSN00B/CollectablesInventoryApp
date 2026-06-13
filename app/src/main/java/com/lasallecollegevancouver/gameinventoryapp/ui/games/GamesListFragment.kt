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
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentGamesListBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
import kotlinx.coroutines.launch

class GamesListFragment : Fragment() {

    private var _binding: FragmentGamesListBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private lateinit var gameAdapter: GameAdapter

    private var fullGameList: List<CollectionItem> = emptyList()
    private var currentSort = "Date Added"
    private var currentFilter = "All Platforms"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentGamesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupRecyclerView()

        binding.fabAddGame.setOnClickListener {
            SmartAddBottomSheet.newInstance(R.id.action_gamesList_to_addEditGame)
                .show(parentFragmentManager, "SmartAdd")
        }
    }

    override fun onResume() {
        super.onResume()
        loadGames()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_list, menu)
            }
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_sort -> { showSortDialog(); true }
                    R.id.action_filter -> { showFilterDialog(); true }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        gameAdapter = GameAdapter { selectedItem ->
            val bundle = Bundle().apply { putInt("itemId", selectedItem.id) }
            findNavController().navigate(R.id.action_gamesList_to_gameDetail, bundle)
        }
        binding.gamesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.gamesRecyclerView.adapter = gameAdapter
    }

    private fun loadGames() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                // Fetch all items and keep only games
                fullGameList = repository.getItems(publicCode).filter { it.type == "GAME" }
                applyCurrentSortAndFilter()
            } catch (exception: Exception) {
                binding.emptyStateText.text = "Could not load games — check your connection"
                binding.emptyStateText.visibility = View.VISIBLE
            }
        }
    }

    private fun applyCurrentSortAndFilter() {
        var filtered = fullGameList
        if (currentFilter != "All Platforms") {
            filtered = filtered.filter { it.platform == currentFilter }
        }

        val sorted = when (currentSort) {
            "Title A–Z"      -> filtered.sortedBy { it.title }
            "Title Z–A"      -> filtered.sortedByDescending { it.title }
            "Platform"       -> filtered.sortedBy { it.platform }
            "Value High–Low" -> filtered.sortedByDescending { it.estimatedValue }
            else             -> filtered.sortedByDescending { it.createdAt }
        }

        gameAdapter.submitList(sorted)
        binding.emptyStateText.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        if (sorted.isEmpty()) binding.emptyStateText.text = "No games yet — tap + to add one"
    }

    private fun showSortDialog() {
        val options = arrayOf("Date Added", "Title A–Z", "Title Z–A", "Platform", "Value High–Low")
        val currentIndex = options.indexOf(currentSort).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle("Sort By")
            .setSingleChoiceItems(options, currentIndex) { dialog, index ->
                currentSort = options[index]
                applyCurrentSortAndFilter()
                dialog.dismiss()
            }
            .show()
    }

    private fun showFilterDialog() {
        val platforms = arrayOf("All Platforms", "SNES", "N64", "Game Boy", "Game Boy Color",
            "GBA", "Virtual Boy", "GameCube", "Switch", "PS1", "PS2", "PS4", "PS5",
            "Genesis", "Saturn", "Dreamcast", "Atari 2600", "Atari 7800", "Jaguar", "Lynx", "Xbox")
        val currentIndex = platforms.indexOf(currentFilter).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Platform")
            .setSingleChoiceItems(platforms, currentIndex) { dialog, index ->
                currentFilter = platforms[index]
                applyCurrentSortAndFilter()
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
