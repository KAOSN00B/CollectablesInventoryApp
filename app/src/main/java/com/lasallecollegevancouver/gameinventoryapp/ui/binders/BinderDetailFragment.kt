package com.lasallecollegevancouver.gameinventoryapp.ui.binders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentBinderDetailBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.ui.collection.CollectionEntryAdapter
import com.lasallecollegevancouver.gameinventoryapp.ui.tcg.OwnedTcgDetailBottomSheet
import kotlinx.coroutines.launch

class BinderDetailFragment : Fragment() {

    private var _binding: FragmentBinderDetailBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private var binderId = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBinderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binderId = arguments?.getInt("binderId", -1) ?: -1
        if (binderId == -1) {
            findNavController().popBackStack()
            return
        }

        binding.binderDetailRecycler.layoutManager = LinearLayoutManager(requireContext())

        binding.deleteBinderButton.setOnClickListener { confirmDeleteBinder() }

        // Refresh the binder contents when a TCG card is removed from its detail sheet.
        parentFragmentManager.setFragmentResultListener(
            OwnedTcgDetailBottomSheet.RESULT_KEY_CHANGED, viewLifecycleOwner
        ) { _, _ -> loadBinderDetail() }

        loadBinderDetail()
    }

    override fun onResume() {
        super.onResume()
        loadBinderDetail()
    }

    private fun loadBinderDetail() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        binding.binderDetailLoading.visibility = View.VISIBLE
        binding.binderDetailEmptyText.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val detail = repository.getBinderDetail(publicCode, binderId)

                // Update the action bar title to the binder name
                activity?.title = detail.name

                val totalValue = detail.items.sumOf { item ->
                    item.estimatedValue * (item.quantity ?: 1)
                }

                binding.binderDetailItemCount.text = "${detail.items.size} items"
                binding.binderDetailTotalValue.text = "$${String.format("%.2f", totalValue)}"

                if (detail.items.isEmpty()) {
                    binding.binderDetailEmptyText.visibility = View.VISIBLE
                    binding.binderDetailRecycler.visibility = View.GONE
                } else {
                    binding.binderDetailEmptyText.visibility = View.GONE
                    binding.binderDetailRecycler.visibility = View.VISIBLE
                    val adapter = CollectionEntryAdapter { selectedItem ->
                        val bundle = Bundle().apply { putInt("itemId", selectedItem.id) }
                        when (selectedItem.type) {
                            "GAME"    -> findNavController().navigate(R.id.action_global_gameDetail, bundle)
                            "CONSOLE" -> findNavController().navigate(R.id.action_global_consoleDetail, bundle)
                            "TCG"     -> OwnedTcgDetailBottomSheet.newInstance(selectedItem.id)
                                .show(parentFragmentManager, "OwnedTcgDetail")
                            else      -> { /* collectible detail still coming later */ }
                        }
                    }
                    binding.binderDetailRecycler.adapter = adapter
                    adapter.submitList(detail.items)
                }
            } catch (exception: Exception) {
                binding.binderDetailEmptyText.text = "Could not load — check your connection"
                binding.binderDetailEmptyText.visibility = View.VISIBLE
            } finally {
                binding.binderDetailLoading.visibility = View.GONE
            }
        }
    }

    private fun confirmDeleteBinder() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Binder")
            .setMessage("This removes the binder only. Your items will stay in your collection.")
            .setPositiveButton("Delete") { _, _ -> deleteBinder() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteBinder() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                repository.deleteBinder(publicCode, binderId)
                findNavController().popBackStack()
            } catch (exception: Exception) {
                // Silently fail
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
