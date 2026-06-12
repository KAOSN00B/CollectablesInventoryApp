package com.lasallecollegevancouver.gameinventoryapp.ui.consoles

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
import com.lasallecollegevancouver.gameinventoryapp.data.Console
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentConsolesListBinding
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
import kotlinx.coroutines.launch

class ConsolesListFragment : Fragment() {

    private var _binding: FragmentConsolesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: AppDatabase
    private lateinit var consoleAdapter: ConsoleAdapter

    // The complete list from Room — never filtered or sorted directly
    private var fullConsoleList: List<Console> = emptyList()

    // The list actually shown in the RecyclerView after sort and filter are applied
    private var displayConsoleList: MutableList<Console> = mutableListOf()

    // Remembers the active sort and filter so they survive a data reload
    private var currentSort = "Date Added"
    private var currentFilter = "All Brands"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsolesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getInstance(requireContext())

        setupMenuItems()
        setupRecyclerView()

        // Open the Smart Add bottom sheet — offers barcode scan, AI identify, or manual entry
        binding.fabAddConsole.setOnClickListener {
            SmartAddBottomSheet.newInstance(R.id.action_consolesList_to_addEditConsole)
                .show(parentFragmentManager, "SmartAdd")
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload every time the user returns here so new or edited consoles appear immediately
        loadConsoles()
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

    // Sets up the RecyclerView with a vertical list layout and the console adapter
    private fun setupRecyclerView() {
        consoleAdapter = ConsoleAdapter { selectedConsole ->
            // Pass the selected console's ID to the detail screen via a Bundle
            val bundle = Bundle().apply { putInt("consoleId", selectedConsole.id) }
            findNavController().navigate(R.id.action_consolesList_to_consoleDetail, bundle)
        }
        binding.consolesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.consolesRecyclerView.adapter = consoleAdapter
    }

    // Fetches all consoles from Room, then re-applies the current sort and filter
    private fun loadConsoles() {
        lifecycleScope.launch {
            fullConsoleList = database.consoleDao().getAllConsoles()
            applyCurrentSortAndFilter()
        }
    }

    // Filters then sorts fullConsoleList and hands the result to the adapter
    private fun applyCurrentSortAndFilter() {
        var filteredList = fullConsoleList

        // Keep only consoles matching the selected brand (skip filter if "All Brands")
        if (currentFilter != "All Brands") {
            filteredList = filteredList.filter { console -> console.brand == currentFilter }
        }

        // Sort the filtered list by the selected order
        val sortedList = when (currentSort) {
            "Name A–Z"       -> filteredList.sortedBy { console -> console.name }
            "Name Z–A"       -> filteredList.sortedByDescending { console -> console.name }
            "Brand"               -> filteredList.sortedBy { console -> console.brand }
            "Value High–Low" -> filteredList.sortedByDescending { console -> console.estimatedValue }
            else                  -> filteredList.sortedByDescending { console -> console.dateAdded }
        }

        displayConsoleList = sortedList.toMutableList()
        consoleAdapter.submitList(displayConsoleList.toList())

        // Show a helpful message when there is nothing to display
        binding.emptyStateText.visibility =
            if (displayConsoleList.isEmpty()) View.VISIBLE else View.GONE
    }

    // Shows a single-choice dialog for picking a sort order
    private fun showSortDialog() {
        val sortOptions = arrayOf("Date Added", "Name A–Z", "Name Z–A", "Brand", "Value High–Low")
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

    // Shows a single-choice dialog for filtering by brand
    private fun showFilterDialog() {
        val filterOptions = arrayOf("All Brands", "Sony", "Microsoft", "Nintendo", "Sega", "Atari", "Other")
        val currentIndex = filterOptions.indexOf(currentFilter).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle("Filter by Brand")
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
