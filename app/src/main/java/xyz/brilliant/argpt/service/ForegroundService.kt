package xyz.brilliant.argpt.service

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import xyz.brilliant.argpt.MainActivity
import xyz.brilliant.argpt.R

class ForegroundService : Service() {

    private val CHANNEL_ID = "ForegroundServiceChannel"
    private val NOTIFICATION_ID = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification: Notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        // Return null if you don't need to bind the service
        return null
    }

//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                CHANNEL_ID,
//                "Foreground Service Channel",
//                NotificationManager.IMPORTANCE_DEFAULT
//            )
//            channel.lightColor = Color.BLUE
//            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            manager.createNotificationChannel(channel)
//        }
//    }

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

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            // Customize the notification as needed
            // Set the priority to PRIORITY_LOW or PRIORITY_MIN to make it less intrusive
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the category to CATEGORY_SERVICE to indicate that it's a background service
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        return notificationBuilder.build()
    }
}


class ForegroundService2 : Service() {


    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "myServiceChannel",
                "My Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(serviceChannel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "myServiceChannel")
            .setContentTitle("foregroundServiceNotificationTitle")
            .setContentText("input")
            // .setSmallIcon(R.drawable.ic_launcher_nav)
            .setContentIntent(pendingIntent)
            // todo don't wait for 10 seconds
            //.setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        startForeground(1, notification)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

class BaseApplication : Application(), LifecycleEventObserver {

    override fun onCreate() {
        super.onCreate()
      //  ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                Thread.sleep(5000)
                when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                        this.startForegroundService(Intent(this, ForegroundService2::class.java))
                    }
                    else -> {
                        this.startService(Intent(this, ForegroundService2::class.java))
                    }
                }
            }
            else -> {
                // do nothing
            }
        }
    }
}
