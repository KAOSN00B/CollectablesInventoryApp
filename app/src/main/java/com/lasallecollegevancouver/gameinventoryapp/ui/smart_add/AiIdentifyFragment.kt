package com.lasallecollegevancouver.gameinventoryapp.ui.smart_add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentAiIdentifyBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CatalogItem
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.RetrofitClient
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.TcgSearchResult
import com.lasallecollegevancouver.gameinventoryapp.ui.tcg.TcgSearchResultAdapter
import com.lasallecollegevancouver.gameinventoryapp.ui.tcg.TcgSearchUiState
import com.lasallecollegevancouver.gameinventoryapp.ui.tcg.TcgSearchViewModel
import kotlinx.coroutines.launch

class AiIdentifyFragment : Fragment() {

    private var _binding: FragmentAiIdentifyBinding? = null
    private val binding get() = _binding!!

    private val catalogRepository = CollectOsRepository()
    private val tcgViewModel: TcgSearchViewModel by viewModels()

    private lateinit var catalogAdapter: CatalogSearchResultAdapter
    private lateinit var tcgAdapter: TcgSearchResultAdapter

    private var isGamesMode = true
    private var selectedPlatform: String? = null

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
        _binding = FragmentAiIdentifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupModeChips()
        setupPlatformFilter()
        setupTcgGameChips()
        setupSearchButton()
        observeTcgState()
    }

    private fun setupAdapters() {
        catalogAdapter = CatalogSearchResultAdapter(
            rawgRepository = RetrofitClient.rawgRepository,
            lifecycleScope = viewLifecycleOwner.lifecycleScope,
            onItemClick = ::navigateWithCatalogItem
        )
        tcgAdapter = TcgSearchResultAdapter { card -> navigateWithTcgCard(card) }

        binding.resultsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsRecycler.adapter = catalogAdapter
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
            binding.resultsRecycler.adapter = catalogAdapter
        } else {
            binding.platformFilterButton.visibility = View.GONE
            binding.tcgGameFilterScroll.visibility = View.VISIBLE
            binding.resultsRecycler.adapter = tcgAdapter
        }
        // Clear results and status when switching modes
        binding.statusText.visibility = View.GONE
        catalogAdapter.submitList(emptyList())
        tcgAdapter.submitList(emptyList())
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
        binding.identifyTextButton.setOnClickListener {
            val query = binding.descriptionInput.text.toString().trim()
            if (query.isEmpty()) {
                binding.descriptionInput.error = "Enter a name to search"
                return@setOnClickListener
            }
            if (isGamesMode) searchCatalog(query) else tcgViewModel.search(query)
        }
    }

    private fun searchCatalog(query: String) {
        val platformLabel = selectedPlatform?.let { " on $it" } ?: ""
        showCatalogStatus("Searching for \"$query\"$platformLabel…")
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val results = catalogRepository.searchCatalog(query, selectedPlatform)
                    .filter { it.type != "CONSOLE" }
                if (results.isEmpty()) {
                    showCatalogStatus("No results found — try a broader search or change the platform.")
                } else {
                    binding.statusText.text = "${results.size} results"
                    binding.statusText.visibility = View.VISIBLE
                    binding.identifyTextButton.isEnabled = true
                    catalogAdapter.submitList(results)
                }
            } catch (exception: Exception) {
                showCatalogStatus("Could not reach server — check your connection.")
            }
        }
    }

    private fun showCatalogStatus(message: String) {
        binding.statusText.text = message
        binding.statusText.visibility = View.VISIBLE
        binding.identifyTextButton.isEnabled = true
        catalogAdapter.submitList(emptyList())
    }

    private fun observeTcgState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tcgViewModel.uiState.collect { state ->
                    if (isGamesMode) return@collect
                    when (state) {
                        is TcgSearchUiState.Idle -> {
                            binding.statusText.visibility = View.GONE
                            tcgAdapter.submitList(emptyList())
                        }
                        is TcgSearchUiState.Loading -> {
                            binding.statusText.text = "Searching…"
                            binding.statusText.visibility = View.VISIBLE
                            tcgAdapter.submitList(emptyList())
                        }
                        is TcgSearchUiState.Success -> {
                            binding.statusText.text = "${state.results.size} cards found"
                            binding.statusText.visibility = View.VISIBLE
                            tcgAdapter.submitList(state.results)
                        }
                        is TcgSearchUiState.Empty -> {
                            binding.statusText.text = "No cards found — try a different name"
                            binding.statusText.visibility = View.VISIBLE
                            tcgAdapter.submitList(emptyList())
                        }
                        is TcgSearchUiState.Error -> {
                            binding.statusText.text = state.message
                            binding.statusText.visibility = View.VISIBLE
                            tcgAdapter.submitList(emptyList())
                        }
                    }
                }
            }
        }
    }

    private fun navigateWithCatalogItem(item: CatalogItem) {
        val bundle = Bundle().apply {
            putString("prefillTitle", item.title)
            putString("prefillPlatform", item.platform)
            putInt("prefillCatalogItemId", item.id)
            putDouble("prefillLooseValue", item.looseValue)
            putDouble("prefillCibValue", item.cibValue)
            putDouble("prefillNewValue", item.newValue)
        }
        val destination = when (item.type) {
            "CONSOLE"    -> R.id.action_aiIdentify_to_addEditConsole
            "COLLECTIBLE" -> R.id.action_aiIdentify_to_addEditCollectible
            else         -> R.id.action_aiIdentify_to_addEditGame
        }
        findNavController().navigate(destination, bundle)
    }

    private fun navigateWithTcgCard(card: TcgSearchResult) {
        val bundle = Bundle().apply {
            putString("tcgSearchResultJson", Gson().toJson(card))
        }
        findNavController().navigate(R.id.action_aiIdentify_to_tcgCardDetail, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
