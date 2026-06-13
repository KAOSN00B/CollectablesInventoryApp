package com.lasallecollegevancouver.gameinventoryapp.ui.games

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemGameBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem

class GameAdapter(
    private val onGameClick: (CollectionItem) -> Unit
) : ListAdapter<CollectionItem, GameAdapter.GameViewHolder>(GameDiffCallback()) {

    inner class GameViewHolder(val binding: ItemGameBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemGameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.gameTitleText.text = item.title
        holder.binding.gamePlatformText.text = item.platform
        holder.binding.gameConditionText.text = item.condition
        holder.binding.gameValueText.text = "$${String.format("%.2f", item.estimatedValue)}"
        if (item.forTrade) {
            holder.binding.gameCompletionText.text = "FOR TRADE"
        } else {
            holder.binding.gameCompletionText.text = ""
        }
        holder.itemView.setOnClickListener { onGameClick(item) }
    }

    class GameDiffCallback : DiffUtil.ItemCallback<CollectionItem>() {
        override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem == newItem
    }
}
