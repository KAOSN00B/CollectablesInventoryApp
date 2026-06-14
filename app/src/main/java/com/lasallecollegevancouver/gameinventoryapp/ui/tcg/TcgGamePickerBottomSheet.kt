package com.lasallecollegevancouver.gameinventoryapp.ui.tcg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.BottomSheetTcgGamePickerBinding

class TcgGamePickerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTcgGamePickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetTcgGamePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionAllGames.setOnClickListener { navigateToSearch("ALL") }
        binding.optionMtg.setOnClickListener      { navigateToSearch("MTG") }
        binding.optionPokemon.setOnClickListener  { navigateToSearch("POKEMON") }
        binding.optionYugioh.setOnClickListener   { navigateToSearch("YUGIOH") }
    }

    private fun navigateToSearch(selectedGame: String) {
        dismiss()
        findNavController().navigate(
            R.id.action_global_tcgSearch,
            bundleOf("selectedGame" to selectedGame)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = TcgGamePickerBottomSheet()
    }
}
