package com.example.mylibraryapps.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.WorkInfo
import com.example.mylibraryapps.MyLibraryApplication
import com.example.mylibraryapps.databinding.FragmentNotificationSettingsBinding
import com.example.mylibraryapps.notification.NotificationScheduler

class NotificationSettingsFragment : Fragment() {
    
    private var _binding: FragmentNotificationSettingsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var notificationScheduler: NotificationScheduler
    
    // Permission launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(requireContext(), "Izin notifikasi diberikan", Toast.LENGTH_SHORT).show()
            updateNotificationStatus()
        } else {
            Toast.makeText(requireContext(), "Izin notifikasi ditolak", Toast.LENGTH_SHORT).show()
            binding.switchNotifications.isChecked = false
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        notificationScheduler = (requireActivity().application as MyLibraryApplication).notificationScheduler
        
        setupViews()
        updateNotificationStatus()
        observeWorkStatus()
    }
    
    private fun setupViews() {
        // Switch untuk mengaktifkan/menonaktifkan notifikasi
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (checkNotificationPermission()) {
                    enableNotifications()
                } else {
                    requestNotificationPermission()
                }
            } else {
                disableNotifications()
            }
        }
        
        // Button untuk test notifikasi
        binding.btnTestNotification.setOnClickListener {
            if (checkNotificationPermission()) {
                testNotification()
            } else {
                requestNotificationPermission()
            }
        }
        
        // Button untuk cek sekarang
        binding.btnCheckNow.setOnClickListener {
            checkNow()
        }
    }
    
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required for older versions
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    
    private fun enableNotifications() {
        notificationScheduler.scheduleBookReminders()
        updateNotificationStatus()
        Toast.makeText(requireContext(), "Notifikasi pengingat diaktifkan", Toast.LENGTH_SHORT).show()
    }
    
    private fun disableNotifications() {
        notificationScheduler.cancelBookReminders()
        updateNotificationStatus()
        Toast.makeText(requireContext(), "Notifikasi pengingat dinonaktifkan", Toast.LENGTH_SHORT).show()
    }
    
    private fun testNotification() {
        notificationScheduler.scheduleImmediateCheck()
        Toast.makeText(requireContext(), "Test notifikasi dijadwalkan", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkNow() {
        notificationScheduler.scheduleImmediateCheck()
        Toast.makeText(requireContext(), "Pengecekan buku terlambat dimulai...", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateNotificationStatus() {
        val hasPermission = checkNotificationPermission()
        binding.tvPermissionStatus.text = if (hasPermission) {
            "✓ Izin notifikasi diberikan"
        } else {
            "✗ Izin notifikasi diperlukan"
        }
        
        // Update switch state based on permission and work status
        binding.switchNotifications.isChecked = hasPermission
        binding.btnTestNotification.isEnabled = hasPermission
        binding.btnCheckNow.isEnabled = hasPermission
    }
    
    private fun observeWorkStatus() {
        notificationScheduler.getWorkStatus().observe(viewLifecycleOwner) { workInfoList ->
            val isScheduled = workInfoList.any { workInfo ->
                workInfo.state == WorkInfo.State.ENQUEUED || workInfo.state == WorkInfo.State.RUNNING
            }
            
            binding.tvWorkStatus.text = if (isScheduled) {
                "✓ Pengingat buku aktif"
            } else {
                "✗ Pengingat buku tidak aktif"
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}