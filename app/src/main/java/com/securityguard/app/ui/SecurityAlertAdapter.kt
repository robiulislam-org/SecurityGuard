package com.securityguard.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.securityguard.app.data.SecurityAlertEntity
import com.securityguard.app.databinding.ItemSecurityAlertBinding

class SecurityAlertAdapter(
    private val onResolveClick: (SecurityAlertEntity) -> Unit
) : ListAdapter<SecurityAlertEntity, SecurityAlertAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(private val binding: ItemSecurityAlertBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SecurityAlertEntity, onResolveClick: (SecurityAlertEntity) -> Unit) {
            binding.alertTitle.text = item.title
            binding.alertDescription.text = item.description

            if (item.isResolved) {
                binding.alertStatus.text = "সমাধান করা হয়েছে"
                binding.alertStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_green_dark))
                binding.alertIcon.setImageResource(android.R.drawable.checkbox_on_background)
                binding.alertIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                    binding.root.context.getColor(android.R.color.holo_green_dark)
                )
                binding.btnResolve.visibility = View.GONE
            } else {
                binding.alertStatus.text = "সক্রিয় ঝুঁকি"
                binding.alertStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
                binding.alertIcon.setImageResource(android.R.drawable.stat_sys_warning)
                binding.alertIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                    binding.root.context.getColor(android.R.color.holo_red_dark)
                )
                binding.btnResolve.visibility = View.VISIBLE
                binding.btnResolve.setOnClickListener { onResolveClick(item) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSecurityAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), onResolveClick)
    }

    object DiffCallback : DiffUtil.ItemCallback<SecurityAlertEntity>() {
        override fun areItemsTheSame(oldItem: SecurityAlertEntity, newItem: SecurityAlertEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SecurityAlertEntity, newItem: SecurityAlertEntity): Boolean {
            return oldItem == newItem
        }
    }
}
