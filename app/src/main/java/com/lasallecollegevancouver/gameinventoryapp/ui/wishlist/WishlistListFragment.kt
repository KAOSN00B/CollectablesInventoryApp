package com.lasallecollegevancouver.gameinventoryapp.ui.wishlist

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
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentWishlistListBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import kotlinx.coroutines.launch

class WishlistListFragment : Fragment() {

    private var _binding: FragmentWishlistListBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()
    private lateinit var wishlistAdapter: WishlistAdapter

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
    }

    override fun onResume() {
        super.onResume()
        loadWishlist()
    }

    private fun loadWishlist() {
        val publicCode = PrefsHelper.getPublicCode(requireContext()) ?: return
        lifecycleScope.launch {
            try {
                val items = repository.getWishlist(publicCode)
                wishlistAdapter.submitList(items)
                binding.emptyStateText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            } catch (exception: Exception) {
                binding.emptyStateText.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
