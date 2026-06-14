package com.lasallecollegevancouver.gameinventoryapp.ui.smart_add

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemCatalogSearchResultBinding
import com.lasallecollegevancouver.gameinventoryapp.network.CatalogItem
import com.lasallecollegevancouver.gameinventoryapp.network.rawg.RawgRepository
import kotlinx.coroutines.launch

class CatalogSearchResultAdapter(
    private val rawgRepository: RawgRepository,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onItemClick: (CatalogItem) -> Unit
) : ListAdapter<CatalogItem, CatalogSearchResultAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(val binding: ItemCatalogSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCatalogSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val binding = holder.binding

        binding.gameTitle.text = item.title
        binding.platformBadge.text = item.platform
        binding.gameMeta.text = item.platform
        binding.gameGenreYear.text = listOfNotNull(item.genre, item.releaseYear?.toString())
            .joinToString(" - ")
            .ifBlank { "Catalog item" }
        binding.gameValue.text = "$${String.format("%.2f", item.cibValue)}"

        val directImageUrl = item.imageUrl?.takeIf { it.isNotBlank() }
        Glide.with(binding.gameCoverImage.context)
            .load(directImageUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(binding.gameCoverImage)

        if (directImageUrl == null && item.type == "GAME") {
            lifecycleScope.launch {
                val rawgImageUrl = rawgRepository.getCoverUrl("${item.title} ${item.platform}")
                    ?: rawgRepository.getCoverUrl(item.title)
                if (rawgImageUrl != null && holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    Glide.with(binding.gameCoverImage.context)
                        .load(rawgImageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(binding.gameCoverImage)
                }
            }
        }

        binding.root.setOnClickListener { onItemClick(item) }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<CatalogItem>() {
            override fun areItemsTheSame(oldItem: CatalogItem, newItem: CatalogItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CatalogItem, newItem: CatalogItem) =
                oldItem == newItem
        }
    }
}
