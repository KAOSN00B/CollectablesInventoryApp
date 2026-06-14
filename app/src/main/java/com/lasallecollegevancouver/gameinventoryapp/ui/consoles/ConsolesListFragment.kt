package com.lasallecollegevancouver.gameinventoryapp.ui.consoles

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentConsolesListBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.ui.smart_add.SmartAddBottomSheet
import kotlinx.coroutines.launch

class ConsolesListFragment : Fragment() {

    private var _binding: FragmentConsolesListBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private lateinit var consoleAdapter: ConsoleAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentConsolesListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        consoleAdapter = ConsoleAdapter { selectedItem ->
            val bundle = Bundle().apply { putInt("itemId", selectedItem.id) }
            findNavController().navigate(R.id.action_consolesList_to_consoleDetail, bundle)
        }
        binding.consolesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.consolesRecyclerView.adapter = consoleAdapter

        binding.fabAddConsole.setOnClickListener {
            SmartAddBottomSheet.newInstance().show(parentFragmentManager, "SmartAdd")
        }

        binding.swipeRefresh.setOnRefreshListener { loadConsoles() }
    }

    override fun onResume() {
        super.onResume()
        loadConsoles()
    }

    private fun loadConsoles() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        binding.loadingIndicator.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val consoles = repository.getItems(publicCode).filter { it.type == "CONSOLE" }
                consoleAdapter.submitList(consoles)
                binding.emptyStateText.visibility = if (consoles.isEmpty()) View.VISIBLE else View.GONE
            } catch (exception: Exception) {
                binding.emptyStateText.visibility = View.VISIBLE
            } finally {
                binding.loadingIndicator.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
