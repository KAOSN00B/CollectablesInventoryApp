package com.lasallecollegevancouver.gameinventoryapp.ui.games

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemDisplayCaseCardBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CollectionItem
import com.lasallecollegevancouver.gameinventoryapp.network.rawg.RawgRepository
import com.lasallecollegevancouver.gameinventoryapp.ui.common.DisplayCaseBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Grid adapter that renders video games as premium "Display Case" cards, reusing the exact same
 * card layout as the TCG grid (item_display_case_card.xml).
 *
 * Games differ from trading cards in two ways, both handled here:
 *   1. Box art is a wide hero image, so we use a 3:4 portrait frame (per the UI guide) and crop
 *      the artwork to fill it instead of letterboxing.
 *   2. A [CollectionItem] of type GAME carries no image URL, so we resolve box art on the fly via
 *      [RawgRepository.getCoverUrl] - the same path the game detail screen already uses. RawgRepository
 *      caches results, so re-binding while scrolling does not re-hit the network.
 *
 * @param coverArtScope a lifecycle-bound scope (e.g. viewLifecycleOwner.lifecycleScope) used to
 *        launch the per-item box-art lookups so they are cancelled when the screen goes away.
 */
class GameDisplayCaseAdapter(
    private val coverArtScope: CoroutineScope,
    private val rawgRepository: RawgRepository,
    private val onGameClick: (CollectionItem) -> Unit
) : ListAdapter<CollectionItem, GameDisplayCaseAdapter.GameViewHolder>(DIFF_CALLBACK) {

    inner class GameViewHolder(val binding: ItemDisplayCaseCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val binding = ItemDisplayCaseCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        // Switch the image frame from the default 2:3 (cards) to 3:4 (games) one time per holder.
        val imageParams = binding.itemImage.layoutParams as ConstraintLayout.LayoutParams
        imageParams.dimensionRatio = "H,3:4"
        binding.itemImage.layoutParams = imageParams

        // Crop wide RAWG hero art to fill the portrait frame rather than leaving empty bars.
        binding.itemImage.scaleType = ImageView.ScaleType.CENTER_CROP

        return GameViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        val game = getItem(position)

        holder.binding.itemTitle.text = game.title
        holder.binding.itemSet.text = game.platform
        holder.binding.itemPrice.text = "$${String.format("%.2f", game.estimatedValue)}"

        // Floating badge: flag tradeable copies in green; otherwise show nothing.
        if (game.forTrade) {
            DisplayCaseBinder.showCustomBadge(holder.binding.badgeLabel, "TRADE", COLOR_TRADE)
        } else {
            DisplayCaseBinder.hideBadge(holder.binding.badgeLabel)
        }

        // Resolve box art asynchronously. We tag the ImageView with this game's id and re-check it
        // once the URL arrives, so a slow response for a card that has since been recycled to show a
        // different game never paints the wrong cover.
        holder.binding.itemImage.setImageDrawable(null)
        holder.binding.itemImage.tag = game.id
        coverArtScope.launch {
            val coverUrl = rawgRepository.getCoverUrl(game.title)
            if (holder.binding.itemImage.tag == game.id) {
                DisplayCaseBinder.loadArtwork(holder.binding.itemImage, coverUrl, centerCrop = true)
            }
        }

        holder.binding.root.setOnClickListener { onGameClick(game) }
    }

    companion object {
        // Forest green for the "TRADE" badge — high contrast against the white badge text.
        private const val COLOR_TRADE = 0xFF2E7D32.toInt()

        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CollectionItem>() {
            override fun areItemsTheSame(oldItem: CollectionItem, newItem: CollectionItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CollectionItem, newItem: CollectionItem) =
                oldItem == newItem
        }
    }
}
