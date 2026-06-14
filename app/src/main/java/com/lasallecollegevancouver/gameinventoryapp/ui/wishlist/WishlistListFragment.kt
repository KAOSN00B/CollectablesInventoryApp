package com.lasallecollegevancouver.gameinventoryapp.ui.wishlist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentWishlistListBinding
import com.lasallecollegevancouver.gameinventoryapp.network.AddWishlistRequest
import com.lasallecollegevancouver.gameinventoryapp.network.CatalogItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.RetrofitClient
import com.lasallecollegevancouver.gameinventoryapp.network.WishlistItem
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.TcgSearchResult
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.CatalogSearchResultAdapter
import com.lasallecollegevancouver.gameinventoryapp.ui.tcg.TcgSearchResultAdapter
import com.lasallecollegevancouver.gameinventoryapp.ui.tcg.TcgSearchUiState
import com.lasallecollegevancouver.gameinventoryapp.ui.tcg.TcgSearchViewModel
import kotlinx.coroutines.launch

class WishlistListFragment : Fragment() {

    private var _binding: FragmentWishlistListBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private val tcgViewModel: TcgSearchViewModel by viewModels()

    private lateinit var wishlistAdapter: WishlistAdapter
    private lateinit var catalogAdapter: CatalogSearchResultAdapter
    private lateinit var tcgAdapter: TcgSearchResultAdapter

    private var isGamesMode = true
    private var selectedPlatform: String? = null

    // Cached wishlist — refreshed in the background after each add
    private var allWishlistItems: List<WishlistItem> = emptyList()

    // WISHLIST = showing saved items; SEARCHING = showing catalog/TCG results to add from
    private enum class ViewState { WISHLIST, SEARCHING }
    private var currentState = ViewState.WISHLIST

    private val platformOptions = arrayOf(
        "Any Platform",
        "NES", "SNES", "N64", "GameCube", "Wii", "Switch",
        "Game Boy", "Game Boy Color", "GBA", "Virtual Boy", "DS", "3DS",
        "PS1", "PS2", "PS3", "PS4", "PS5", "PSP",
        "Xbox", "Xbox 360",
        "Genesis", "Saturn", "Dreamcast",
        "Atari 2600", "Atari 7800", "Jaguar", "Lynx"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWishlistListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupModeChips()
        setupPlatformFilter()
        setupTcgGameChips()
        setupSearchButton()
        setupSearchInputWatcher()
        observeTcgState()

        binding.fabAddWishlistItem.setOnClickListener {
            findNavController().navigate(R.id.action_wishlistList_to_addEditWishlist)
        }
    }

    override fun onResume() {
        super.onResume()
        loadWishlist()
    }

    private fun setupAdapters() {
        wishlistAdapter = WishlistAdapter { selectedItem ->
            val bundle = Bundle().apply { putInt("wishlistItemId", selectedItem.id) }
            findNavController().navigate(R.id.action_wishlistList_to_wishlistDetail, bundle)
        }

        catalogAdapter = CatalogSearchResultAdapter(
            rawgRepository = RetrofitClient.rawgRepository,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onItemClick = ::addCatalogItemToWishlist
        )

        tcgAdapter = TcgSearchResultAdapter { card -> addTcgCardToWishlist(card) }

        binding.resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsRecycler.adapter = wishlistAdapter
    }

    private fun setupModeChips() {
        binding.modeChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            isGamesMode = checkedIds.firstOrNull() == R.id.chip_mode_games
            applyMode()
        }
    }

    private fun applyMode() {
        if (isGamesMode) {
            binding.platformFilterButton.visibility = View.VISIBLE
            binding.tcgGameFilterScroll.visibility = View.GONE
        } else {
            binding.platformFilterButton.visibility = View.GONE
            binding.tcgGameFilterScroll.visibility = View.VISIBLE
        }
        // If currently searching, switch the adapter and clear results
        if (currentState == ViewState.SEARCHING) {
            binding.resultsRecycler.adapter = if (isGamesMode) catalogAdapter else tcgAdapter
            catalogAdapter.submitList(emptyList())
            tcgAdapter.submitList(emptyList())
            showStatus("Type a name and tap Search")
        }
    }

    private fun setupPlatformFilter() {
        binding.platformFilterButton.setOnClickListener {
            val currentIndex = if (selectedPlatform == null) 0
                else platformOptions.indexOf(selectedPlatform).coerceAtLeast(0)

            AlertDialog.Builder(requireContext())
                .setTitle("Filter by Platform")
                .setSingleChoiceItems(platformOptions, currentIndex) { dialog, index ->
                    selectedPlatform = if (index == 0) null else platformOptions[index]
                    binding.platformFilterButton.text = selectedPlatform ?: "Any Platform"
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun setupTcgGameChips() {
        binding.tcgGameChips.setOnCheckedStateChangeListener { _, checkedIds ->
            tcgViewModel.selectedGame = when (checkedIds.firstOrNull()) {
                R.id.chip_tcg_mtg     -> "MTG"
                R.id.chip_tcg_pokemon -> "POKEMON"
                R.id.chip_tcg_yugioh  -> "YUGIOH"
                else                  -> "ALL"
            }
        }
    }

    private fun setupSearchButton() {
        binding.searchButton.setOnClickListener {
            val query = binding.searchInput.text?.toString()?.trim() ?: return@setOnClickListener
            if (query.isEmpty()) {
                binding.searchInput.error = "Enter a name to search"
                return@setOnClickListener
            }
            if (isGamesMode) searchCatalog(query) else tcgViewModel.search(query)
        }
    }

    // Clearing the search input returns the user to their saved wishlist
    private fun setupSearchInputWatcher() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                if (editable.isNullOrBlank() && currentState == ViewState.SEARCHING) {
                    showWishlist()
                }
            }
        })
    }

    private fun loadWishlist() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        if (currentState == ViewState.WISHLIST) {
            binding.loadingIndicator.visibility = View.VISIBLE
        }
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                allWishlistItems = repository.getWishlist(publicCode)
            } catch (exception: Exception) {
                // Keep whatever was cached; don't crash the UI
            } finally {
                binding.loadingIndicator.visibility = View.GONE
                if (currentState == ViewState.WISHLIST) {
                    showWishlist()
                }
            }
        }
    }

    // Silently refreshes the wishlist cache without switching away from search results
    private fun refreshWishlistInBackground() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                allWishlistItems = repository.getWishlist(publicCode)
                if (currentState == ViewState.WISHLIST) showWishlist()
            } catch (exception: Exception) { /* silent */ }
        }
    }

    private fun showWishlist() {
        currentState = ViewState.WISHLIST
        binding.resultsRecycler.adapter = wishlistAdapter
        wishlistAdapter.submitList(allWishlistItems)

        if (allWishlistItems.isEmpty()) {
            showStatus("Your wishlist is empty — search above to add items")
        } else {
            showStatus("YOUR WISHLIST · ${allWishlistItems.size} items")
            wishlistAdapter.submitList(allWishlistItems)
        }
    }

    private fun searchCatalog(query: String) {
        currentState = ViewState.SEARCHING
        binding.resultsRecycler.adapter = catalogAdapter
        catalogAdapter.submitList(emptyList())
        showLoadingStatus("Searching for \"$query\"…")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val results = repository.searchCatalog(query, selectedPlatform)
                    .filter { it.type != "CONSOLE" }
                if (results.isEmpty()) {
                    showStatus("No results — try a different name or platform")
                } else {
                    showStatus("${results.size} results · tap to add to wishlist")
                    catalogAdapter.submitList(results)
                }
            } catch (exception: Exception) {
                showStatus("Could not reach server — check your connection")
            }
        }
    }

    private fun observeTcgState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tcgViewModel.uiState.collect { state ->
                    if (isGamesMode) return@collect
                    when (state) {
                        is TcgSearchUiState.Idle -> { /* do nothing — search not triggered yet */ }
                        is TcgSearchUiState.Loading -> {
                            currentState = ViewState.SEARCHING
                            binding.resultsRecycler.adapter = tcgAdapter
                            tcgAdapter.submitList(emptyList())
                            showLoadingStatus("Searching…")
                        }
                        is TcgSearchUiState.Success -> {
                            showStatus("${state.results.size} cards · tap to add to wishlist")
                            tcgAdapter.submitList(state.results)
                        }
                        is TcgSearchUiState.Empty -> {
                            showStatus("No cards found — try a different name")
                            tcgAdapter.submitList(emptyList())
                        }
                        is TcgSearchUiState.Error -> {
                            showStatus(state.message)
                            tcgAdapter.submitList(emptyList())
                        }
                    }
                }
            }
        }
    }

    private fun addCatalogItemToWishlist(item: CatalogItem) {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = AddWishlistRequest(
                    catalogItemId = item.id,
                    title = item.title,
                    platform = item.platform,
                    targetPrice = item.looseValue,
                    currentEstimatedValue = item.looseValue,
                    notes = null,
                    isGrail = false
                )
                repository.addWishlistItem(publicCode, request)
                Toast.makeText(requireContext(), "${item.title} added to wishlist", Toast.LENGTH_SHORT).show()
                // Refresh cache silently — user can keep browsing search results
                refreshWishlistInBackground()
            } catch (exception: Exception) {
                Toast.makeText(requireContext(), "Could not add to wishlist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addTcgCardToWishlist(card: TcgSearchResult) {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val platform = "${card.tcgGame}: ${card.setName}"
                val price = card.priceRegular ?: 0.0
                val request = AddWishlistRequest(
                    catalogItemId = null,
                    title = card.name,
                    platform = platform,
                    targetPrice = price,
                    currentEstimatedValue = price,
                    notes = card.rarity.takeIf { it.isNotBlank() },
                    isGrail = false
                )
                repository.addWishlistItem(publicCode, request)
                Toast.makeText(requireContext(), "${card.name} added to wishlist", Toast.LENGTH_SHORT).show()
                refreshWishlistInBackground()
            } catch (exception: Exception) {
                Toast.makeText(requireContext(), "Could not add to wishlist", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showStatus(message: String) {
        binding.loadingIndicator.visibility = View.GONE
        binding.statusText.text = message
    }

    private fun showLoadingStatus(message: String) {
        binding.loadingIndicator.visibility = View.VISIBLE
        binding.statusText.text = message
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
