package com.lasallecollegevancouver.gameinventoryapp.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemDisplayCaseCardBinding
import com.lasallecollegevancouver.gameinventoryapp.network.tcg.TcgSearchResult

/**
 * Grid adapter that renders TCG search results as premium "Display Case" cards.
 *
 * This is the showcase wiring for the new card component: it takes the unified [TcgSearchResult]
 * (already normalised from Scryfall / Pokémon TCG / YGOProDeck by TcgRepository) and maps each
 * field onto the reusable card layout. Pair it with a GridLayoutManager to get the digital
 * display-case grid look.
 */
class DisplayCaseCardAdapter(
    private val onCardClick: (TcgSearchResult) -> Unit
) : ListAdapter<TcgSearchResult, DisplayCaseCardAdapter.DisplayCaseViewHolder>(DIFF_CALLBACK) {

    inner class DisplayCaseViewHolder(
        val binding: ItemDisplayCaseCardBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DisplayCaseViewHolder {
        val binding = ItemDisplayCaseCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return DisplayCaseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DisplayCaseViewHolder, position: Int) {
        val card = getItem(position)

        // Title and set/number metadata line.
        holder.binding.itemTitle.text = card.name
        holder.binding.itemSet.text = "${card.setName} · ${card.cardNumber}"

        // Market price: prefer the foil price when this result is the foil variant, otherwise
        // the regular price. Fall back to a clear "N/A" so a card never shows a blank value.
        val priceToShow = if (card.isFoilVariant && card.priceFoil != null) card.priceFoil else card.priceRegular
        holder.binding.itemPrice.text =
            if (priceToShow != null) "$${String.format("%.2f", priceToShow)}" else "N/A"

        // Floating badge: raw search results are not graded, so we show the colored game tag
        // (MTG / PKM / YGO) in the corner to demonstrate the floating-badge slot with real data.
        DisplayCaseBinder.showCategoryBadge(holder.binding.badgeLabel, card.tcgGame)

        // Artwork from the API, faded in by Glide over the recessed background.
        DisplayCaseBinder.loadArtwork(holder.binding.itemImage, card.imageUrl)

        holder.binding.root.setOnClickListener { onCardClick(card) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TcgSearchResult>() {
            override fun areItemsTheSame(oldItem: TcgSearchResult, newItem: TcgSearchResult) =
                oldItem.externalId == newItem.externalId && oldItem.isFoilVariant == newItem.isFoilVariant

            override fun areContentsTheSame(oldItem: TcgSearchResult, newItem: TcgSearchResult) =
                oldItem == newItem
        }
    }
}
