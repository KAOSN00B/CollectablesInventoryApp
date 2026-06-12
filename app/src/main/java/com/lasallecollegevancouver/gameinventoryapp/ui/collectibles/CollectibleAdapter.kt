package com.lasallecollegevancouver.gameinventoryapp.ui.collectibles

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.data.Collectible
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemCollectibleBinding

// RecyclerView adapter for the collectibles list — uses DiffUtil for efficient updates
class CollectibleAdapter(
    private val onCollectibleClick: (Collectible) -> Unit
) : ListAdapter<Collectible, CollectibleAdapter.CollectibleViewHolder>(CollectibleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CollectibleViewHolder {
        val binding = ItemCollectibleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CollectibleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CollectibleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CollectibleViewHolder(
        private val binding: ItemCollectibleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(collectible: Collectible) {
            binding.collectibleName.text = collectible.name
            // Show the type badge and a type-specific subtitle (set, card game, issue number, etc.)
            binding.collectibleType.text = collectible.type
            binding.collectibleSubtitle.text = buildSubtitle(collectible)
            binding.collectibleValue.text = "$${String.format("%.2f", collectible.estimatedValue)}"
            binding.root.setOnClickListener { onCollectibleClick(collectible) }
        }

        // Builds a context-appropriate subtitle line based on the collectible type
        private fun buildSubtitle(collectible: Collectible): String {
            return when (collectible.type) {
                "COMIC" -> listOfNotNull(collectible.series, collectible.issueNumber?.let { "#$it" }).joinToString(" ")
                "TCG" -> listOfNotNull(collectible.tcgGame, collectible.cardSet).joinToString(" — ")
                "TOY" -> listOfNotNull(collectible.franchise, collectible.brand).joinToString(" — ")
                "LEGO" -> listOfNotNull(collectible.setNumber, collectible.theme).joinToString(" — ")
                else -> collectible.condition
            }
        }
    }

    class CollectibleDiffCallback : DiffUtil.ItemCallback<Collectible>() {
        override fun areItemsTheSame(oldItem: Collectible, newItem: Collectible) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Collectible, newItem: Collectible) = oldItem == newItem
    }
}
