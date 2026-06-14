package com.lasallecollegevancouver.gameinventoryapp.ui.tcg

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemTcgSearchResultBinding
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.TcgSearchResult

class TcgSearchResultAdapter(
    private val onCardClick: (TcgSearchResult) -> Unit
) : ListAdapter<TcgSearchResult, TcgSearchResultAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(val binding: ItemTcgSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTcgSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val card = getItem(position)

        holder.binding.cardName.text = card.name
        holder.binding.cardSetInfo.text = "${card.setName}  ·  ${card.cardNumber}"
        holder.binding.cardRarity.text = card.rarity

        // Price display: show regular price, foil if available
        holder.binding.cardPrice.text = when {
            card.isFoilVariant && card.priceRegular != null ->
                "Foil: $${String.format("%.2f", card.priceRegular)}"
            card.priceRegular != null ->
                "$${String.format("%.2f", card.priceRegular)}"
            else -> "Price N/A"
        }

        // Color-coded game badge
        holder.binding.gameBadge.text = when (card.tcgGame) {
            "MTG"     -> "MTG"
            "POKEMON" -> "PKM"
            "YUGIOH"  -> "YGO"
            else      -> card.tcgGame
        }
        val badgeColor = when (card.tcgGame) {
            "MTG"     -> 0xFF7B2FBE.toInt()  // purple
            "POKEMON" -> 0xFFE3B000.toInt()  // yellow/gold
            "YUGIOH"  -> 0xFF1565C0.toInt()  // dark blue
            else      -> 0xFF666666.toInt()
        }
        holder.binding.gameBadge.setBackgroundColor(badgeColor)

        // Foil indicator
        holder.binding.foilBadge.visibility =
            if (card.isFoilVariant) android.view.View.VISIBLE else android.view.View.GONE

        // Card image via Glide
        Glide.with(holder.binding.cardImage.context)
            .load(card.imageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.binding.cardImage)

        holder.binding.root.setOnClickListener { onCardClick(card) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TcgSearchResult>() {
            override fun areItemsTheSame(oldItem: TcgSearchResult, newItem: TcgSearchResult) =
                oldItem.externalId == newItem.externalId
            override fun areContentsTheSame(oldItem: TcgSearchResult, newItem: TcgSearchResult) =
                oldItem == newItem
        }
    }
}
