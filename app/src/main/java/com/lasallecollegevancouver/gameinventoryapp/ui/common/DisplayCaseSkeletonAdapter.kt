package com.lasallecollegevancouver.gameinventoryapp.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemDisplayCaseSkeletonBinding

/**
 * A tiny fixed-size adapter that fills a grid with shimmering skeleton cards while real data
 * loads from the API. It has no real data of its own - it just renders [placeholderCount]
 * identical skeleton tiles and runs the breathing shimmer animation on each.
 *
 * Show this adapter's RecyclerView during a Loading state, then swap to the real
 * [DisplayCaseCardAdapter] once results arrive.
 */
class DisplayCaseSkeletonAdapter(
    private val placeholderCount: Int = 6
) : RecyclerView.Adapter<DisplayCaseSkeletonAdapter.SkeletonViewHolder>() {

    inner class SkeletonViewHolder(
        val binding: ItemDisplayCaseSkeletonBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkeletonViewHolder {
        val binding = ItemDisplayCaseSkeletonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SkeletonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SkeletonViewHolder, position: Int) {
        // Begin the gentle pulse as soon as the placeholder is shown.
        ShimmerHelper.start(holder.binding.skeletonRoot)
    }

    override fun onViewRecycled(holder: SkeletonViewHolder) {
        super.onViewRecycled(holder)
        // Release the animator when the placeholder scrolls off / is recycled.
        ShimmerHelper.stop(holder.binding.skeletonRoot)
    }

    override fun getItemCount(): Int = placeholderCount
}
