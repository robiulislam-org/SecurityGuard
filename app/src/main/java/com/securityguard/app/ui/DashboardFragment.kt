package com.securityguard.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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
        viewModel = ViewModelProvider(this).get(DashboardViewModel::class.java)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.allUsage.observe(viewLifecycleOwner) { usageList ->
            if (!usageList.isNullOrEmpty()) {
                val totalMs = usageList.sumOf { it.usageTimeMs }
                binding.tvScreenTimeValue.text = formatDuration(totalMs)

                val topApps = usageList.sortedByDescending { it.usageTimeMs }.take(3)
                val sb = StringBuilder()
                for ((index, app) in topApps.withIndex()) {
                    sb.append("${index + 1}. ${app.appName} — ${formatDuration(app.usageTimeMs)}\n")
                }
                binding.tvTopAppsValue.text = sb.toString().trim()
            } else {
                binding.tvScreenTimeValue.text = "0 মিনিট"
                binding.tvTopAppsValue.text = "কোনো ডেটা পাওয়া যায়নি"
            }
        }

        viewModel.latestLocation.observe(viewLifecycleOwner) { location ->
            if (location != null) {
                binding.tvLocationValue.text = "অক্ষাংশ: ${"%.4f".format(location.latitude)}, দ্রাঘিমাংশ: ${"%.4f".format(location.longitude)}"
            } else {
                binding.tvLocationValue.text = "লোকেশন পাওয়া যায়নি"
            }
        }

        viewModel.unresolvedAlerts.observe(viewLifecycleOwner) { alerts ->
            if (!alerts.isNullOrEmpty()) {
                binding.tvSecurityStatus.text = "${alerts.size}টি ঝুঁকি সনাক্ত করা হয়েছে!"
                binding.tvSecurityStatus.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
            } else {
                binding.tvSecurityStatus.text = "কোনো ঝুঁকি পাওয়া যায়নি"
                binding.tvSecurityStatus.setTextColor(resources.getColor(android.R.color.white, null))
            }
        }
    }

    private fun formatDuration(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return when {
            hours > 0 -> "${hours} ঘণ্টা ${minutes} মিনিট"
            else -> "${minutes} মিনিট"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
