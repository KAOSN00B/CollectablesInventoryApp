package com.lasallecollegevancouver.gameinventoryapp.ui.binders

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentBindersBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BindersFragment : Fragment() {

    private var _binding: FragmentBindersBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.bindersRecycler.layoutManager = LinearLayoutManager(requireContext())

        // Pinned "All Items" card opens the full collection (including items not in any binder).
        binding.allItemsCard.setOnClickListener {
            findNavController().navigate(R.id.action_binders_to_collectionList)
        }

        binding.bindersFab.setOnClickListener { showCreateBinderDialog() }
    }

    override fun onResume() {
        super.onResume()
        loadBinders()
    }

    private fun loadBinders() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        binding.bindersLoading.visibility = View.VISIBLE
        binding.bindersEmptyText.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val binders = repository.getBinders(publicCode)

                if (binders.isEmpty()) {
                    binding.bindersEmptyText.visibility = View.VISIBLE
                    binding.bindersRecycler.visibility = View.GONE
                } else {
                    binding.bindersEmptyText.visibility = View.GONE
                    binding.bindersRecycler.visibility = View.VISIBLE
                    binding.bindersRecycler.adapter = BindersAdapter(binders) { selectedBinder ->
                        val bundle = Bundle().apply { putInt("binderId", selectedBinder.id) }
                        findNavController().navigate(R.id.action_binders_to_binderDetail, bundle)
                    }
                }
            } catch (exception: Exception) {
                binding.bindersEmptyText.text = "Could not load binders — check your connection"
                binding.bindersEmptyText.visibility = View.VISIBLE
            } finally {
                binding.bindersLoading.visibility = View.GONE
            }
        }
    }

    private fun showCreateBinderDialog() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return

        val nameInput = EditText(requireContext()).apply {
            hint = "Binder name (e.g. Base Set Holos)"
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("New Binder")
            .setView(nameInput)
            .setPositiveButton("Create") { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        try {
                            repository.createBinder(publicCode, name)
                            loadBinders()
                        } catch (exception: Exception) {
                            // Silently fail — the list will just not refresh
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
