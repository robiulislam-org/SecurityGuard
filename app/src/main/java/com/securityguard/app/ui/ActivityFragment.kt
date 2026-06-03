package com.securityguard.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.securityguard.app.databinding.FragmentActivityBinding

class ActivityFragment : Fragment() {
    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ActivityViewModel
    private lateinit var adapter: AppUsageAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(ActivityViewModel::class.java)
        
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = AppUsageAdapter()
        binding.rvAppUsage.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.appUsageList.observe(viewLifecycleOwner) { usageList ->
            if (!usageList.isNullOrEmpty()) {
                binding.rvAppUsage.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
                adapter.submitList(usageList)
            } else {
                binding.rvAppUsage.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
