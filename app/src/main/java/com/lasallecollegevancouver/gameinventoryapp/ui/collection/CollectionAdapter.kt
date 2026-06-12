package com.lasallecollegevancouver.gameinventoryapp.ui.collection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemCollectionEntryBinding

class CollectionAdapter(
    private val onItemClick: (CollectionEntry) -> Unit
) : ListAdapter<CollectionEntry, CollectionAdapter.CollectionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectionViewHolder {
        val binding = ItemCollectionEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CollectionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CollectionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CollectionViewHolder(
        private val binding: ItemCollectionEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(entry: CollectionEntry) {
            binding.categoryBadge.text = entry.category.take(4)
            binding.itemTitle.text = entry.title
            binding.itemMeta.text = entry.meta
            binding.itemCondition.text = entry.condition
            binding.itemValue.text = "$${String.format("%.2f", entry.estimatedValue)}"
            binding.root.setOnClickListener { onItemClick(entry) }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CollectionEntry>() {
        override fun areItemsTheSame(oldItem: CollectionEntry, newItem: CollectionEntry): Boolean {
            return oldItem.category == newItem.category && oldItem.sourceId == newItem.sourceId
        }

        override fun areContentsTheSame(oldItem: CollectionEntry, newItem: CollectionEntry): Boolean {
            return oldItem == newItem
        }
    }
}
