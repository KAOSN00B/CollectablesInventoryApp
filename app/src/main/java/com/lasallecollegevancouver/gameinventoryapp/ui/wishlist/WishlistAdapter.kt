package com.lasallecollegevancouver.gameinventoryapp.ui.wishlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemWishlistBinding
import com.lasallecollegevancouver.gameinventoryapp.network.WishlistItem

class WishlistAdapter(
    private val onItemClick: (WishlistItem) -> Unit
) : ListAdapter<WishlistItem, WishlistAdapter.WishlistViewHolder>(WishlistDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WishlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WishlistViewHolder(
        private val binding: ItemWishlistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WishlistItem) {
            binding.wishlistItemTitle.text = if (item.isGrail) "★ ${item.title}" else item.title
            binding.wishlistItemType.text = item.platform
            binding.wishlistItemTargetPrice.text = "Target: $${String.format("%.2f", item.targetPrice)}"
            binding.wishlistItemMarketPrice.text = "Value: $${String.format("%.2f", item.currentEstimatedValue)}"
            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    class WishlistDiffCallback : DiffUtil.ItemCallback<WishlistItem>() {
        override fun areItemsTheSame(oldItem: WishlistItem, newItem: WishlistItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: WishlistItem, newItem: WishlistItem) = oldItem == newItem
    }
}
