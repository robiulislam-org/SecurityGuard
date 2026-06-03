package com.securityguard.app.ui

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import com.securityguard.app.databinding.FragmentSettingsBinding
import com.securityguard.app.service.MonitoringService
import com.securityguard.app.service.BackgroundTaskService

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupListeners()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupListeners() {
        binding.switchMonitoring.setOnCheckedChangeListener { _, isChecked ->
            val context = requireContext()
            val monitorIntent = Intent(context, MonitoringService::class.java)
            val scanIntent = Intent(context, BackgroundTaskService::class.java)
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(monitorIntent)
                } else {
                    context.startService(monitorIntent)
                }
                context.startService(scanIntent)
            } else {
                context.stopService(monitorIntent)
                context.stopService(scanIntent)
            }
        }

        binding.btnManagePermissions.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", requireContext().packageName, null)
            }
            startActivity(intent)
        }
    }

    private fun updatePermissionStatus() {
        val context = requireContext()

        val isLocGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (isLocGranted) {
            binding.tvPermLocation.text = "অনুমোদিত"
            binding.tvPermLocation.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            binding.tvPermLocation.text = "অনুমতি নেই"
            binding.tvPermLocation.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }

        val phoneCameraPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
        val isPhoneCamGranted = phoneCameraPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (isPhoneCamGranted) {
            binding.tvPermPhoneCamera.text = "অনুমোদিত"
            binding.tvPermPhoneCamera.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            binding.tvPermPhoneCamera.text = "অনুমতি নেই"
            binding.tvPermPhoneCamera.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        val isUsageGranted = mode == AppOpsManager.MODE_ALLOWED
        if (isUsageGranted) {
            binding.tvPermUsage.text = "অনুমোদিত"
            binding.tvPermUsage.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            binding.tvPermUsage.text = "অনুমতি নেই"
            binding.tvPermUsage.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
