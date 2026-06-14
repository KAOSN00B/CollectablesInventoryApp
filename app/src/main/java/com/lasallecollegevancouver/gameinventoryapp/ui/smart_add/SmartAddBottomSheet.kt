package com.lasallecollegevancouver.gameinventoryapp.ui.smart_add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.BottomSheetSmartAddBinding

class SmartAddBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSmartAddBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = SmartAddBottomSheet()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSmartAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionVideoGame.setOnClickListener {
            dismiss()
            VideoGameAddBottomSheet.newInstance().show(parentFragmentManager, "VideoGameAdd")
        }

        binding.optionTradingCard.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.action_global_tcgSearch)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
