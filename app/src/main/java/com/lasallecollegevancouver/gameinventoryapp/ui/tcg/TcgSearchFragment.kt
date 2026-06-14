package com.lasallecollegevancouver.gameinventoryapp.ui.tcg

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentTcgSearchBinding
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.TcgSearchResult
import kotlinx.coroutines.launch

class TcgSearchFragment : Fragment() {

    private var _binding: FragmentTcgSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TcgSearchViewModel by viewModels()
    private lateinit var resultAdapter: TcgSearchResultAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTcgSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply game selection from nav argument (set before first search)
        val selectedGame = arguments?.getString("selectedGame") ?: "ALL"
        viewModel.selectedGame = selectedGame

        // When a specific game was pre-selected, hide the chip filter row
        if (selectedGame != "ALL") {
            binding.gameFilterScroll.visibility = View.GONE
        }

        setupRecyclerView()
        setupSearchBar()
        setupGameChips()
        observeUiState()
    }

    private fun setupRecyclerView() {
        resultAdapter = TcgSearchResultAdapter { card ->
            navigateToCardDetail(card)
        }
        binding.resultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.resultsRecyclerView.adapter = resultAdapter
    }

    private fun setupSearchBar() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                viewModel.onQueryChanged(editable?.toString() ?: "")
            }
        })

        // Also search when the keyboard search action is tapped
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.searchInput.text?.toString() ?: ""
                if (query.length >= 2) viewModel.search(query)
                true
            } else false
        }
    }

    private fun setupGameChips() {
        binding.gameFilterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.selectedGame = when (checkedIds.firstOrNull()) {
                R.id.chip_mtg     -> "MTG"
                R.id.chip_pokemon -> "POKEMON"
                R.id.chip_yugioh  -> "YUGIOH"
                else              -> "ALL"
            }
            // Re-run the search with the new game filter
            val query = binding.searchInput.text?.toString() ?: ""
            if (query.length >= 2) viewModel.search(query)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is TcgSearchUiState.Idle -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.resultsRecyclerView.visibility = View.GONE
                            binding.statusText.visibility = View.VISIBLE
                            binding.statusText.text = "Search for a card name above"
                        }
                        is TcgSearchUiState.Loading -> {
                            binding.loadingIndicator.visibility = View.VISIBLE
                            binding.resultsRecyclerView.visibility = View.GONE
                            binding.statusText.visibility = View.GONE
                        }
                        is TcgSearchUiState.Success -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.statusText.visibility = View.GONE
                            binding.resultsRecyclerView.visibility = View.VISIBLE
                            resultAdapter.submitList(state.results)
                        }
                        is TcgSearchUiState.Empty -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.resultsRecyclerView.visibility = View.GONE
                            binding.statusText.visibility = View.VISIBLE
                            binding.statusText.text = "No cards found — try a different name"
                        }
                        is TcgSearchUiState.Error -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.resultsRecyclerView.visibility = View.GONE
                            binding.statusText.visibility = View.VISIBLE
                            binding.statusText.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun navigateToCardDetail(card: TcgSearchResult) {
        val bundle = Bundle().apply {
            putString("tcgSearchResultJson", Gson().toJson(card))
        }
        findNavController().navigate(R.id.action_tcgSearch_to_tcgCardDetail, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
