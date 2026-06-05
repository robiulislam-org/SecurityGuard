package com.securityguard.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.securityguard.app.R
import com.securityguard.app.databinding.FragmentDashboardBinding
import java.util.concurrent.TimeUnit

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.allUsage.observe(viewLifecycleOwner) { usageList ->
            if (!usageList.isNullOrEmpty()) {
                val totalMs = usageList.sumOf { it.usageTimeMs }
                binding.tvScreenTimeValue.text = formatDuration(totalMs)

                val topApps = usageList.sortedByDescending { it.usageTimeMs }.take(3)
                val sb = StringBuilder()
                topApps.forEachIndexed { index, app ->
                    sb.append("${index + 1}. ${app.appName} — ${formatDuration(app.usageTimeMs)}\n")
                }
                binding.tvTopAppsValue.text = sb.toString().trim()
            } else {
                binding.tvScreenTimeValue.text = "০ মিনিট"
                binding.tvTopAppsValue.text = "আজকের ডেটা লোড হচ্ছে..."
            }
        }

        viewModel.latestLocation.observe(viewLifecycleOwner) { location ->
            binding.tvLocationValue.text = if (location != null) {
                "অক্ষাংশ: ${"%.4f".format(location.latitude)}\nদ্রাঘিমাংশ: ${"%.4f".format(location.longitude)}"
            } else {
                "লোকেশন পাওয়া যায়নি"
            }
        }

        viewModel.unresolvedAlerts.observe(viewLifecycleOwner) { alerts ->
            val ctx = requireContext()
            if (!alerts.isNullOrEmpty()) {
                binding.tvSecurityStatus.text = "⚠️ ${alerts.size}টি সক্রিয় ঝুঁকি পাওয়া গেছে!"
                binding.tvSecurityStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_red_light))
            } else {
                binding.tvSecurityStatus.text = "✅ কোনো ঝুঁকি পাওয়া যায়নি"
                binding.tvSecurityStatus.setTextColor(ContextCompat.getColor(ctx, android.R.color.holo_green_light))
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> "${hours} ঘণ্টা ${minutes} মিনিট"
            minutes > 0 -> "${minutes} মিনিট"
            else -> "১ মিনিটের কম"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
