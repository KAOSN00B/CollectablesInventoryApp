package com.lasallecollegevancouver.gameinventoryapp.ui.smart_add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.databinding.BottomSheetSmartAddBinding

class SmartAddBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSmartAddBinding? = null
    private val binding get() = _binding!!
    private var manualAddActionId: Int = -1

    companion object {
        const val ARG_MANUAL_ACTION_ID = "manualAddActionId"

        fun newInstance(manualAddActionId: Int = -1): SmartAddBottomSheet {
            return SmartAddBottomSheet().apply {
                arguments = Bundle().apply { putInt(ARG_MANUAL_ACTION_ID, manualAddActionId) }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetSmartAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        manualAddActionId = arguments?.getInt(ARG_MANUAL_ACTION_ID, -1) ?: -1

        binding.optionScanBarcode.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.action_global_barcodeScan)
        }

        binding.optionAiIdentify.setOnClickListener {
            dismiss()
            findNavController().navigate(R.id.action_global_aiIdentify)
        }

        binding.optionManualEntry.setOnClickListener {
            if (manualAddActionId != -1) {
                dismiss()
                findNavController().navigate(manualAddActionId)
            } else {
                showManualCategoryPicker()
            }
        }
    }

    private fun showManualCategoryPicker() {
        val labels = arrayOf("Game", "Console", "Comic / TCG / Toy / LEGO")
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
