package com.lasallecollegevancouver.gameinventoryapp.ui.collection

import android.content.res.ColorStateList
import android.graphics.Color
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

        if (item.type == "TCG") {
            bindTcgItem(holder.binding, item)
        } else {
            bindGameOrConsoleItem(holder.binding, item)
        }

        // Color the badge according to platform/game so items are visually scannable at a glance
        holder.binding.categoryBadge.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor(badgeColor(item)))

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    // Display logic for standard game and console items
    private fun bindGameOrConsoleItem(binding: ItemCollectionEntryBinding, item: CollectionItem) {
        binding.categoryBadge.text = if (item.type == "GAME") "GAME" else "CON"
        binding.itemTitle.text = item.title
        binding.itemMeta.text = item.platform
        binding.itemCondition.text = "${item.condition}${if (item.forTrade) " · FOR TRADE" else ""}"
        binding.itemValue.text = "$${String.format("%.2f", item.estimatedValue)}"
    }

    // Display logic for TCG cards — shows game abbreviation badge, set+number, and foil/qty indicators
    private fun bindTcgItem(binding: ItemCollectionEntryBinding, item: CollectionItem) {
        binding.categoryBadge.text = when (item.tcgGame) {
            "MTG"     -> "MTG"
            "POKEMON" -> "PKM"
            "YUGIOH"  -> "YGO"
            else      -> "TCG"
        }

        binding.itemTitle.text = item.title

        binding.itemMeta.text = buildString {
            append(item.tcgSet ?: item.platform)
            val number = item.tcgCardNumber
            if (!number.isNullOrBlank()) append("  #$number")
        }

        binding.itemCondition.text = buildString {
            append(item.condition)
            if (item.tcgIsFoil == true) append(" · FOIL")
            val quantity = item.quantity
            if (quantity != null && quantity > 1) append(" · ×$quantity")
            if (item.forTrade) append(" · FOR TRADE")
        }

        binding.itemValue.text = "$${String.format("%.2f", item.estimatedValue)}"
    }

    // Returns a hex color string for the badge based on the item's platform or TCG game.
    // Chosen to be readable with white text and distinctive enough to recognize at a glance.
    private fun badgeColor(item: CollectionItem): String {
        if (item.type == "TCG") {
            return when (item.tcgGame) {
                "MTG"     -> "#6B3FA0"   // Magic purple
                "POKEMON" -> "#3B4CCA"   // Pokémon blue (works better than yellow with white text)
                "YUGIOH"  -> "#8B6914"   // YGO gold-brown
                else      -> "#607D8B"   // Generic slate
            }
        }

        return when (item.platform) {
            // Nintendo
            "NES", "SNES", "N64", "GameCube", "Wii", "Switch",
            "Game Boy", "Game Boy Color", "GBA", "Virtual Boy", "DS", "3DS" -> "#D01223"

            // Sony
            "PS1", "PS2", "PS3", "PS4", "PS5", "PSP" -> "#0070D1"

            // Microsoft
            "Xbox", "Xbox 360" -> "#107C10"

            // Sega
            "Genesis", "Saturn", "Dreamcast" -> "#1752A2"

            // Atari
            "Atari 2600", "Atari 7800", "Jaguar", "Lynx" -> "#FF5000"

            // Collectible (non-TCG)
            else -> if (item.type == "COLLECTIBLE") "#FF6D00" else "#455A64"
        }
    }

    class EntryDiffCallback : DiffUtil.ItemCallback<CollectionItem>() {
        override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem) = oldItem == newItem
    }
}
