package xyz.brilliant.argpt.service

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log


class BluetoothBackgroundService : Service() {
   /* private lateinit var handler: Handler
    private lateinit var runnable: Runnable*/
    override fun onBind(intent: Intent): IBinder? {
        // Return null since this is not a bound service
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            while (true) {
                Log.e("Service", "Service is running...")
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
        val CHANNELID = "Foreground Service ID"
        val channel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNELID,
                CHANNELID,
                NotificationManager.IMPORTANCE_LOW
            )
        } else {
            TODO("VERSION.SDK_INT < O")
        }

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification: Notification.Builder = Notification.Builder(this, CHANNELID)
            .setContentText("Service is running")
            .setContentTitle("Service enabled")
            .setSmallIcon(R.drawable.sym_def_app_icon)

        startForeground(1001, notification.build())
        return super.onStartCommand(intent, flags, startId)
    }

}