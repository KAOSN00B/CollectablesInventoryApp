package com.lasallecollegevancouver.gameinventoryapp.ui.collection

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemCollectionEntryBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem

class CollectionEntryAdapter(
    private val onItemClick: (CollectionItem) -> Unit
) : ListAdapter<CollectionItem, CollectionEntryAdapter.EntryViewHolder>(EntryDiffCallback()) {

    inner class EntryViewHolder(val binding: ItemCollectionEntryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemCollectionEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.categoryBadge.text = if (item.type == "GAME") "GAME" else "CON"
        holder.binding.itemTitle.text = item.title
        holder.binding.itemMeta.text = item.platform
        holder.binding.itemCondition.text = "${item.condition}${if (item.forTrade) " · FOR TRADE" else ""}"
        holder.binding.itemValue.text = "$${String.format("%.2f", item.estimatedValue)}"
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    class EntryDiffCallback : DiffUtil.ItemCallback<CollectionItem>() {
        override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem == newItem
    }
}
