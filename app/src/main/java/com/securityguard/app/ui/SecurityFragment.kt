package com.securityguard.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.securityguard.app.databinding.FragmentSecurityBinding

class SecurityFragment : Fragment() {
    private var _binding: FragmentSecurityBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SecurityViewModel
    private lateinit var adapter: SecurityAlertAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(SecurityViewModel::class.java)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = SecurityAlertAdapter { alert ->
            viewModel.resolveAlert(alert.id)
        }
        binding.rvSecurityAlerts.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.securityAlerts.observe(viewLifecycleOwner) { alerts ->
            if (!alerts.isNullOrEmpty()) {
                binding.rvSecurityAlerts.visibility = View.VISIBLE
                binding.tvEmptyState.visibility = View.GONE
                adapter.submitList(alerts)
            } else {
                binding.rvSecurityAlerts.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
