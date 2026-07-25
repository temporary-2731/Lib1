package com.yourname.vf.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourname.vf.databinding.ItemHistoryBinding
import com.yourname.vf.db.ConversionHistory

class HistoryAdapter : ListAdapter<ConversionHistory, HistoryAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ConversionHistory) {
            binding.tvFilename.text = item.inputFileName
            binding.tvMethod.text = "Method: ${item.method}"
            binding.tvStatus.text = "Status: ${item.status} | ${item.durationSec}s"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<ConversionHistory>() {
        override fun areItemsTheSame(oldItem: ConversionHistory, newItem: ConversionHistory) =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ConversionHistory, newItem: ConversionHistory) =
            oldItem == newItem
    }
}
