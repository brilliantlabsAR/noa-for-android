package xyz.brilliant.argpt.service

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import xyz.brilliant.argpt.R


class ForegroundService : Service() {

    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = createNotification(true)
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        unregisterReceiver(connectionStatusReceiver);
        super.onDestroy()
    }
    private fun triggerScan() {
        val intent = Intent("ACTION_START_SCAN")
        sendBroadcast(intent)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val filter = IntentFilter("ACTION_CONNECTION_STATUS")
        registerReceiver(connectionStatusReceiver, filter)
        Thread {
            while (true) {
                triggerScan()
                try {
                    Thread.sleep(5000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
        return super.onStartCommand(intent, flags, startId)
    }
    private val connectionStatusReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            println("working")
            if (intent?.action == "ACTION_CONNECTION_STATUS") {
                val isConnected = intent.getBooleanExtra("EXTRA_CONNECTION_STATUS", false)
                val notification: Notification = createNotification(isConnected)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        // Return null if you don't need to bind the service
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.setSound(null, null) // Remove sound for the notification
            channel.enableVibration(false) // Disable vibration for the notification
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(isConnected:Boolean): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Noa Service")
            .setContentText(if (isConnected) "Listening.." else "Disconnected! Trying to connect.")
            .setSmallIcon(R.drawable.logo_button)
//            .setColor("#E82E87".toInt())

            // Customize the notification as needed
            // Set the priority to PRIORITY_LOW or PRIORITY_MIN to make it less intrusive
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the category to CATEGORY_SERVICE to indicate that it's a background service
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        return notificationBuilder.build()
    }
}
