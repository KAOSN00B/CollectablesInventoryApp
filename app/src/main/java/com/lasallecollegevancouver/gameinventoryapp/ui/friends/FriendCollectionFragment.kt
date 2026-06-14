package com.lasallecollegevancouver.gameinventoryapp.ui.friends

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lasallecollegevancouver.gameinventoryapp.databinding.FragmentFriendCollectionBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectOsRepository
import kotlinx.coroutines.launch

// Read-only view of a friend's collection. The user's own code in PrefsHelper is never touched.
class FriendCollectionFragment : Fragment() {

    private var _binding: FragmentFriendCollectionBinding? = null
    private val binding get() = _binding!!

    private val repository = CollectOsRepository()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFriendCollectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // friendCode is passed as a nav argument from SettingsFragment
        val friendCode = arguments?.getString("friendCode") ?: run {
            showError("No collection code provided")
            return
        }

        loadFriendCollection(friendCode)
    }

    private fun loadFriendCollection(friendCode: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Fetch collection info, all items, and binders in parallel
                val collection = repository.getCollection(friendCode)
                val allItems = repository.getItems(friendCode)
                val binders = repository.getBinders(friendCode)

                // Header
                binding.friendNameText.text = collection.displayName ?: "Friend's Collection"
                binding.friendItemCountText.text = "${allItems.size} items in collection"

                // For Trade — items the friend has marked as available for trade
                val forTradeItems = allItems.filter { it.forTrade }
                binding.forTradeText.text = if (forTradeItems.isEmpty()) {
                    "Nothing listed for trade right now"
                } else {
                    forTradeItems.joinToString("\n") { item ->
                        val value = if (item.estimatedValue > 0) {
                            " — \$${String.format("%.2f", item.estimatedValue)}"
                        } else ""
                        "${item.title} (${item.platform}) · ${item.condition}$value"
                    }
                }

                // Binders
                binding.bindersText.text = if (binders.isEmpty()) {
                    "No binders yet"
                } else {
                    binders.joinToString("\n") { binder ->
                        val itemLabel = if (binder.itemCount == 1) "1 card" else "${binder.itemCount} cards"
                        "• ${binder.name}  ($itemLabel)"
                    }
                }
            } catch (exception: Exception) {
                showError("Could not load collection — check your connection")
            } finally {
                binding.loadingIndicator.visibility = View.GONE
            }
        }
    }

    private fun showError(message: String) {
        binding.friendNameText.text = message
        binding.friendItemCountText.text = ""
        binding.forTradeText.text = "—"
        binding.bindersText.text = "—"
        binding.loadingIndicator.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
