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
import com.lasallecollegevancouver.gameinventoryapp.data.AppDatabase
import com.lasallecollegevancouver.gameinventoryapp.data.Game
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentGamesListBinding
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
import kotlinx.coroutines.launch

class GamesListFragment : Fragment() {

    private var _binding: FragmentGamesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var gameAdapter: GameAdapter

    // The complete list from Room — never filtered or sorted directly
    private var fullGameList: List<Game> = emptyList()

    // The list actually shown in the RecyclerView after sort and filter are applied
    private var displayGameList: MutableList<Game> = mutableListOf()

    // Remembers the active sort and filter so they survive a data reload
    private var currentSort = "Date Added"
    private var currentFilter = "All Platforms"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGamesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())

        setupMenuItems()
        setupRecyclerView()

        // Open the Smart Add bottom sheet — offers barcode scan, AI identify, or manual entry
        binding.fabAddGame.setOnClickListener {
            SmartAddBottomSheet.newInstance(R.id.action_gamesList_to_addEditGame)
                .show(parentFragmentManager, "SmartAdd")
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload every time the user returns here so new or edited games appear immediately
        loadGames()
    }

    // Registers the Sort and Filter menu items via the modern MenuProvider API
    private fun setupMenuItems() {
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

    // Sets up the RecyclerView with a vertical list layout and the game adapter
    private fun setupRecyclerView() {
        gameAdapter = GameAdapter { selectedGame ->
            // Pass the selected game's ID to the detail screen via a Bundle
            val bundle = Bundle().apply { putInt("gameId", selectedGame.id) }
            findNavController().navigate(R.id.action_gamesList_to_gameDetail, bundle)
        }
        binding.gamesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.gamesRecyclerView.adapter = gameAdapter
    }

    // Fetches all games from Room, then re-applies the current sort and filter
    private fun loadGames() {
        lifecycleScope.launch {
            fullGameList = database.gameDao().getAllGames()
            applyCurrentSortAndFilter()
        }
    }

    // Filters then sorts fullGameList and hands the result to the adapter
    private fun applyCurrentSortAndFilter() {
        var filteredList = fullGameList

        // Keep only games matching the selected platform (skip filter if "All Platforms")
        if (currentFilter != "All Platforms") {
            filteredList = filteredList.filter { game -> game.platform == currentFilter }
        }

        // Sort the filtered list by the selected order
        val sortedList = when (currentSort) {
            "Title A–Z"      -> filteredList.sortedBy { game -> game.title }
            "Title Z–A"      -> filteredList.sortedByDescending { game -> game.title }
            "Platform"            -> filteredList.sortedBy { game -> game.platform }
            "Value High–Low" -> filteredList.sortedByDescending { game -> game.estimatedValue }
            else                  -> filteredList.sortedByDescending { game -> game.dateAdded }
        }

        displayGameList = sortedList.toMutableList()
        gameAdapter.submitList(displayGameList.toList())

        // Show a helpful message when there is nothing to display
        binding.emptyStateText.visibility =
            if (displayGameList.isEmpty()) View.VISIBLE else View.GONE
    }

    // Shows a single-choice dialog for picking a sort order
    private fun showSortDialog() {
        val sortOptions = arrayOf("Date Added", "Title A–Z", "Title Z–A", "Platform", "Value High–Low")
        val currentIndex = sortOptions.indexOf(currentSort).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle("Sort By")
            .setSingleChoiceItems(sortOptions, currentIndex) { dialog, selectedIndex ->
                currentSort = sortOptions[selectedIndex]
                applyCurrentSortAndFilter()
                dialog.dismiss()
            }
            .show()
    }

    // Shows a single-choice dialog for filtering by platform
    private fun showFilterDialog() {
        val filterOptions = arrayOf("All Platforms", "PlayStation 5", "Xbox Series X", "Nintendo Switch", "PC", "Other")
        val currentIndex = filterOptions.indexOf(currentFilter).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Platform")
            .setSingleChoiceItems(filterOptions, currentIndex) { dialog, selectedIndex ->
                currentFilter = filterOptions[selectedIndex]
                applyCurrentSortAndFilter()
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Null the binding to avoid memory leaks when the fragment view is destroyed
        _binding = null
    }
}
