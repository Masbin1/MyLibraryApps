package com.example.mylibraryapps.ui.test

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mylibraryapps.R
import com.example.mylibraryapps.utils.NotificationTestHelper
import com.example.mylibraryapps.service.NotificationForegroundService
import com.example.mylibraryapps.utils.AlarmScheduler
import kotlinx.coroutines.launch

class NotificationTestActivity : AppCompatActivity() {
    
    private lateinit var testHelper: NotificationTestHelper
    private var testTransactionId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_test)
        
        testHelper = NotificationTestHelper(this)
        
        setupButtons()
    }
    
    private fun setupButtons() {
        findViewById<Button>(R.id.btnTestImmediate).setOnClickListener {
            testHelper.testImmediateNotificationCheck()
            Toast.makeText(this, "Immediate notification check started", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.btnTestSystemNotification).setOnClickListener {
            testHelper.testSystemNotificationDirect()
            Toast.makeText(this, "System notification sent to Android bar!", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.btnCreate3DayTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(3)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (3 days)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreate2DayTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(2)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (2 days)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreate1DayTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(1)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (1 day)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreateTodayTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(0)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (today)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnCreateOverdueTest).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId = testHelper.createTestTransaction(-1)
                Toast.makeText(this@NotificationTestActivity, "Test transaction created (overdue)", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnShowActiveTransactions).setOnClickListener {
            lifecycleScope.launch {
                testHelper.showActiveTransactions()
                Toast.makeText(this@NotificationTestActivity, "Check logcat for active transactions", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnClearNotificationRecords).setOnClickListener {
            lifecycleScope.launch {
                testHelper.clearNotificationSentRecords()
                Toast.makeText(this@NotificationTestActivity, "Notification records cleared", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnShowNotificationRecords).setOnClickListener {
            lifecycleScope.launch {
                testHelper.showNotificationSentRecords()
                Toast.makeText(this@NotificationTestActivity, "Check logcat for notification records", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnUpdateFCMToken).setOnClickListener {
            lifecycleScope.launch {
                testHelper.updateFCMToken()
                Toast.makeText(this@NotificationTestActivity, "FCM token updated", Toast.LENGTH_SHORT).show()
            }
        }
        
        findViewById<Button>(R.id.btnDeleteTestTransaction).setOnClickListener {
            lifecycleScope.launch {
                testTransactionId?.let { id ->
                    testHelper.deleteTestTransaction(id)
                    testTransactionId = null
                    Toast.makeText(this@NotificationTestActivity, "Test transaction deleted", Toast.LENGTH_SHORT).show()
                } ?: run {
                    Toast.makeText(this@NotificationTestActivity, "No test transaction to delete", Toast.LENGTH_SHORT).show()
                }
            }
        }
        
        findViewById<Button>(R.id.btnStartBackgroundService).setOnClickListener {
            NotificationForegroundService.startService(this)
            Toast.makeText(this, "Background service started", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.btnStopBackgroundService).setOnClickListener {
            NotificationForegroundService.stopService(this)
            Toast.makeText(this, "Background service stopped", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<Button>(R.id.btnScheduleAlarm).setOnClickListener {
            val alarmScheduler = AlarmScheduler(this)
            alarmScheduler.scheduleNotificationAlarm()
            Toast.makeText(this, "Alarm scheduled", Toast.LENGTH_SHORT).show()
        }
    }
}