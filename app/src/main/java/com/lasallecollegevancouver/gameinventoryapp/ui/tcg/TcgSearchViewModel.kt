package com.lasallecollegevancouver.gameinventoryapp.ui.tcg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lasallecollegevancouver.gameinventoryapp.network.RetrofitClient
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.TcgSearchResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TcgSearchUiState {
    object Idle : TcgSearchUiState()
    object Loading : TcgSearchUiState()
    data class Success(val results: List<TcgSearchResult>) : TcgSearchUiState()
    object Empty : TcgSearchUiState()
    data class Error(val message: String) : TcgSearchUiState()
}

class TcgSearchViewModel : ViewModel() {

    private val repository = RetrofitClient.tcgRepository

    private val _uiState = MutableStateFlow<TcgSearchUiState>(TcgSearchUiState.Idle)
    val uiState: StateFlow<TcgSearchUiState> = _uiState

    private var searchJob: Job? = null
    var selectedGame: String = "ALL"  // "ALL" | "MTG" | "POKEMON" | "YUGIOH"

    // Debounced search — waits 300ms after the last keystroke before hitting the API
    fun onQueryChanged(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _uiState.value = TcgSearchUiState.Idle
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            search(query)
        }
    }

    fun search(query: String) {
        if (query.length < 2) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = TcgSearchUiState.Loading
            try {
                val results = when (selectedGame) {
                    "MTG"     -> repository.searchMtg(query)
                    "POKEMON" -> repository.searchPokemon(query)
                    "YUGIOH"  -> repository.searchYugioh(query)
                    else      -> repository.searchAll(query)
                }
                _uiState.value = if (results.isEmpty()) {
                    TcgSearchUiState.Empty
                } else {
                    TcgSearchUiState.Success(results)
                }
            } catch (exception: Exception) {
                _uiState.value = TcgSearchUiState.Error("Could not search — check your connection")
            }
        }
    }
}
