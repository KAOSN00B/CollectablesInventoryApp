package com.lasallecollegevancouver.gameinventoryapp.ui.consoles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.data.Console
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemConsoleBinding

// ListAdapter handles animating row changes automatically using DiffUtil
class ConsoleAdapter(
    private val onConsoleClick: (Console) -> Unit
) : ListAdapter<Console, ConsoleAdapter.ConsoleViewHolder>(ConsoleDiffCallback()) {

    // ViewHolder holds the binding for one console card row
    inner class ConsoleViewHolder(val binding: ItemConsoleBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConsoleViewHolder {
        // Inflate the card layout and wrap it in a ViewHolder
        val binding = ItemConsoleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ConsoleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConsoleViewHolder, position: Int) {
        val console = getItem(position)

        // Fill each field of the card with the console's data
        holder.binding.consoleNameText.text = console.name
        holder.binding.consoleBrandText.text = console.brand
        holder.binding.consoleModelText.text = console.model
        holder.binding.consoleConditionText.text = console.condition
        holder.binding.consoleValueText.text = "$${String.format("%.2f", console.estimatedValue)}"

        // Pass the tapped console back to the fragment so it can navigate to the detail screen
        holder.itemView.setOnClickListener { onConsoleClick(console) }
    }

    // DiffUtil compares old and new items so only changed rows are redrawn
    class ConsoleDiffCallback : DiffUtil.ItemCallback<Console>() {
        override fun areItemsTheSame(oldItem: Console, newItem: Console) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Console, newItem: Console) = oldItem == newItem
    }
}
