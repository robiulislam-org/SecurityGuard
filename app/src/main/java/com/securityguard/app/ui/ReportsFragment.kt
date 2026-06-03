package com.securityguard.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.securityguard.app.databinding.FragmentReportsBinding

class ReportsFragment : Fragment() {
    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
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
                val sortedList = usageList.sortedByDescending { it.usageTimeMs }.take(5)
                setupChart(sortedList)
            } else {
                binding.barChart.clear()
            }
        }

        viewModel.unresolvedAlerts.observe(viewLifecycleOwner) { alerts ->
            val totalAlertsCount = alerts?.size ?: 0
            if (totalAlertsCount > 0) {
                binding.tvThreatsSummary.text = "ডিভাইসে $totalAlertsCount টি অমিমাংসিত নিরাপত্তা ঝুঁকি সনাক্ত করা হয়েছে। অনুগ্রহ করে নিরাপত্তা ট্যাবে গিয়ে সমাধান করুন।"
                binding.tvThreatsSummary.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            } else {
                binding.tvThreatsSummary.text = "অভিনন্দন! ডিভাইসে বর্তমানে কোনো নিরাপত্তা ঝুঁকি পাওয়া যায়নি। আপনার ডিভাইস সম্পূর্ণ সুরক্ষিত।"
                binding.tvThreatsSummary.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            }
        }
    }

    private fun setupChart(topApps: List<com.securityguard.app.data.AppUsageEntity>) {
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        for ((index, app) in topApps.withIndex()) {
            val minutes = app.usageTimeMs.toFloat() / (1000 * 60)
            entries.add(BarEntry(index.toFloat(), minutes))
            labels.add(app.appName)
        }

        val dataSet = BarDataSet(entries, "অ্যাপ ব্যবহারের সময় (মিনিট)").apply {
            colors = ColorTemplate.COLORFUL_COLORS.toList()
            valueTextSize = 12f
        }

        val barData = BarData(dataSet)
        binding.barChart.apply {
            data = barData
            description.isEnabled = false
            legend.isEnabled = true
            
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
                textSize = 10f
            }
            
            axisLeft.apply {
                axisMinimum = 0f
                textSize = 10f
            }
            axisRight.isEnabled = false
            
            animateY(1000)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
