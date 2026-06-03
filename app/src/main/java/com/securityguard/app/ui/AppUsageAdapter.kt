package com.securityguard.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.securityguard.app.data.AppUsageEntity
import com.securityguard.app.databinding.ItemAppUsageBinding
import java.util.concurrent.TimeUnit

class AppUsageAdapter : ListAdapter<AppUsageEntity, AppUsageAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemAppUsageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppUsageEntity) {
            binding.appName.text = item.appName
            binding.packageName.text = item.packageName
            binding.usageTime.text = formatDuration(item.usageTimeMs)
        }

        private fun formatDuration(millis: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(millis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    object DiffCallback : DiffUtil.ItemCallback<AppUsageEntity>() {
        override fun areItemsTheSame(oldItem: AppUsageEntity, newItem: AppUsageEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AppUsageEntity, newItem: AppUsageEntity): Boolean {
            return oldItem == newItem
        }
    }
}
