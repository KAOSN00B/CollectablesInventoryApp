package com.lasallecollegevancouver.gameinventoryapp.ui.smart_add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.BottomSheetVideoGameAddBinding

class VideoGameAddBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetVideoGameAddBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = VideoGameAddBottomSheet()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetVideoGameAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.optionScanBarcode.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.action_global_barcodeScan)
        }

        binding.optionPhotoScan.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.action_global_photoScan)
        }

        binding.optionSearchCatalog.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.action_global_aiIdentify)
        }

        binding.optionManualEntry.setOnClickListener {
            showManualCategoryPicker()
        }
    }

    private fun showManualCategoryPicker() {
        val labels = arrayOf("Game", "Console", "Collectible")
        val actions = intArrayOf(
            R.id.action_global_addGame,
            R.id.action_global_addConsole,
            R.id.action_global_addCollectible
        )
        AlertDialog.Builder(requireContext())
            .setTitle("Choose item type")
            .setItems(labels) { _, which ->
                dismiss()
                findNavController().navigate(actions[which])
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
