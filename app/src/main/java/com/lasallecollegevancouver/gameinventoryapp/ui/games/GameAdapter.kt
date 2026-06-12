package com.lasallecollegevancouver.gameinventoryapp.ui.games

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.data.Game
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemGameBinding

// ListAdapter handles animating row changes automatically using DiffUtil
class GameAdapter(
    private val onGameClick: (Game) -> Unit
) : ListAdapter<Game, GameAdapter.GameViewHolder>(GameDiffCallback()) {

    // ViewHolder holds the binding for one game card row
    inner class GameViewHolder(val binding: ItemGameBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        // Inflate the card layout and wrap it in a ViewHolder
        val binding = ItemGameBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return GameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = getItem(position)

        // Fill each field of the card with the game's data
        holder.binding.gameTitleText.text = game.title
        holder.binding.gamePlatformText.text = game.platform
        holder.binding.gameGenreText.text = game.genre
        holder.binding.gameConditionText.text = game.condition
        holder.binding.gameCompletionText.text = game.completionStatus
        holder.binding.gameValueText.text = "$${String.format("%.2f", game.estimatedValue)}"

        // Pass the tapped game back to the fragment so it can navigate to the detail screen
        holder.itemView.setOnClickListener { onGameClick(game) }
    }

    // DiffUtil compares old and new items so only changed rows are redrawn
    class GameDiffCallback : DiffUtil.ItemCallback<Game>() {
        override fun areItemsTheSame(oldItem: Game, newItem: Game) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Game, newItem: Game) = oldItem == newItem
    }
}
