package com.lasallecollegevancouver.gameinventoryapp.ui.wishlist

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.lasallecollegevancouver.gameinventoryapp.R
import com.lasallecollegevancouver.gameinventoryapp.config.PrefsHelper
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentWishlistListBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import com.lasallecollegevancouver.gameinventoryapp.network.WishlistItem
import kotlinx.coroutines.launch

class WishlistListFragment : Fragment() {

    private var _binding: FragmentWishlistListBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private lateinit var wishlistAdapter: WishlistAdapter
    private var allWishlistItems: List<WishlistItem> = emptyList()
    private var searchQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWishlistListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        wishlistAdapter = WishlistAdapter { selectedItem ->
            val bundle = Bundle().apply { putInt("wishlistItemId", selectedItem.id) }
            findNavController().navigate(R.id.action_wishlistList_to_wishlistDetail, bundle)
        }
        binding.wishlistRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.wishlistRecyclerView.adapter = wishlistAdapter

        binding.fabAddWishlistItem.setOnClickListener {
            findNavController().navigate(R.id.action_wishlistList_to_addEditWishlist)
        }

        // Filter as the user types — searches title and platform
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                searchQuery = editable?.toString() ?: ""
                applyFilter()
            }
        })

        binding.swipeRefresh.setOnRefreshListener { loadWishlist() }
    }

    override fun onResume() {
        super.onResume()
        loadWishlist()
    }

    private fun loadWishlist() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        binding.loadingIndicator.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                allWishlistItems = repository.getWishlist(publicCode)
                applyFilter()
            } catch (exception: Exception) {
                binding.emptyStateText.visibility = View.VISIBLE
            } finally {
                binding.loadingIndicator.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun applyFilter() {
        val filtered = if (searchQuery.isBlank()) allWishlistItems else {
            allWishlistItems.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.platform.contains(searchQuery, ignoreCase = true)
            }
        }
        wishlistAdapter.submitList(filtered)
        binding.emptyStateText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        if (filtered.isEmpty()) {
            binding.emptyStateText.text = if (allWishlistItems.isEmpty()) {
                "Your wishlist is empty — tap + to add an item"
            } else {
                "No results for \"$searchQuery\""
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
