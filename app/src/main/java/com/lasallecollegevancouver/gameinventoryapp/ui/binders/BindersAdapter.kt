package com.lasallecollegevancouver.gameinventoryapp.ui.binders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lasallecollegevancouver.gameinventoryapp.databinding.ItemBinderBinding
import com.lasallecollegevancouver.gameinventoryapp.network.Binder

class BindersAdapter(
    private val binders: List<Binder>,
    private val onBinderClick: (Binder) -> Unit
) : RecyclerView.Adapter<BindersAdapter.BinderViewHolder>() {

    inner class BinderViewHolder(val binding: ItemBinderBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BinderViewHolder {
        val binding = ItemBinderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BinderViewHolder(binding)
    }

    override fun getItemCount() = binders.size

    override fun onBindViewHolder(holder: BinderViewHolder, position: Int) {
        val binder = binders[position]
        holder.binding.binderName.text = binder.name
        holder.binding.binderSubtitle.text = "${binder.itemCount} items"
        holder.binding.binderValue.text = "$${String.format("%.2f", binder.totalValue)}"
        holder.itemView.setOnClickListener { onBinderClick(binder) }
    }
}
