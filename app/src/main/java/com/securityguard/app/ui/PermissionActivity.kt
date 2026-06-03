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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.securityguard.app.MainActivity
import com.securityguard.app.databinding.ActivityPermissionBinding

class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding

    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val phoneCameraPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
    )

    private val LOCATION_REQ_CODE = 2001
    private val PHONE_CAMERA_REQ_CODE = 2002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updatePermissionButtonsState()
    }

    private fun setupListeners() {
        binding.btnGrantLocation.setOnClickListener {
            ActivityCompat.requestPermissions(this, locationPermissions, LOCATION_REQ_CODE)
        }

        binding.btnGrantPhoneCamera.setOnClickListener {
            ActivityCompat.requestPermissions(this, phoneCameraPermissions, PHONE_CAMERA_REQ_CODE)
        }

        binding.btnGrantUsageStats.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
            }
        }

        binding.btnContinue.setOnClickListener {
            if (hasAllPermissions()) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "অনুগ্রহ করে সব পারমিশন মঞ্জুর করুন", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionButtonsState()
    }

    private fun updatePermissionButtonsState() {
        val locGranted = isLocationGranted()
        binding.btnGrantLocation.isEnabled = !locGranted
        binding.btnGrantLocation.text = if (locGranted) "মঞ্জুরকৃত" else "দিন"

        val phoneCamGranted = isPhoneCameraGranted()
        binding.btnGrantPhoneCamera.isEnabled = !phoneCamGranted
        binding.btnGrantPhoneCamera.text = if (phoneCamGranted) "মঞ্জুরকৃত" else "দিন"

        val usageGranted = isUsageStatsGranted()
        binding.btnGrantUsageStats.isEnabled = !usageGranted
        binding.btnGrantUsageStats.text = if (usageGranted) "মঞ্জুরকৃত" else "দিন"
    }

    private fun isLocationGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isPhoneCameraGranted(): Boolean {
        return phoneCameraPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun isUsageStatsGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasAllPermissions(): Boolean {
        return isLocationGranted() && isPhoneCameraGranted() && isUsageStatsGranted()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updatePermissionButtonsState()
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "পারমিশন সফলভাবে মঞ্জুর হয়েছে", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "পারমিশন প্রত্যাখ্যান করা হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }
}
