package com.lasallecollegevancouver.gameinventoryapp.ui.consoles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemConsoleBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem

class ConsoleAdapter(
    private val onConsoleClick: (CollectionItem) -> Unit
) : ListAdapter<CollectionItem, ConsoleAdapter.ConsoleViewHolder>(ConsoleDiffCallback()) {

    inner class ConsoleViewHolder(val binding: ItemConsoleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsoleViewHolder {
        val binding = ItemConsoleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ConsoleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConsoleViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.consoleNameText.text = item.title
        holder.binding.consoleBrandText.text = item.platform
        holder.binding.consoleConditionText.text = item.condition
        holder.binding.consoleValueText.text = "$${String.format("%.2f", item.estimatedValue)}"
        // Use model field for trade status
        holder.binding.consoleModelText.text = if (item.forTrade) "FOR TRADE" else ""
        holder.itemView.setOnClickListener { onConsoleClick(item) }
    }

    class ConsoleDiffCallback : DiffUtil.ItemCallback<CollectionItem>() {
        override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem == newItem
    }
}
