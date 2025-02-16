// Copyright (C) 2024 by Ubaldo Porcheddu <ubaldo@eja.it>
// Copyright (C) 2025 by Takase

package it.eja.ntfyrelay

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

private const val CHANNEL_ID = "NFTY_RELAY"
private const val NOTIFICATION_ID = 100

class NotificationListenerService : NotificationListenerService() {

    private val sender = NotificationSender(this)
    private val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ ->
            createNotification(sharedPreferences.getBoolean("ACTIVE", false))
        }

    override fun onListenerConnected() {
        super.onListenerConnected()

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        createNotification(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        createNotification(false)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE, "").toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT, "").toString()
        Log.d("NotificationListenerService", "Title: $title, Text: $text")

        val sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val url = sharedPreferences.getString("URL", "") ?: ""
        val active: Boolean = sharedPreferences.getBoolean("ACTIVE", false)
        val authType = NotificationServerConfig.AuthType.valueOf(
            sharedPreferences.getString("AUTH_TYPE", null)
                ?: NotificationServerConfig.AuthType.HTTP_NONE.toString()
        )
        val username = sharedPreferences.getString("USERNAME", null)
        val password = sharedPreferences.getString("PASSWORD", null)
        val serverConfig = NotificationServerConfig(url, authType, username, password)

        if (active && url != "") {
            try {
                sender.sendNotification(serverConfig, title, text, notification.smallIcon)
            } catch (e: Exception) {
                Log.e("NotificationListenerService", "Error sending notification: ${e.message}")
            }
        }
    }

    private fun createNotification(running: Boolean) {
        val description: String
        val icon: Int
        val button: String
        val action: NotificationControlService.ActionType

        if (running) {
            description = getString(R.string.desc_running)
            icon = R.drawable.baseline_pause_24
            button = getString(R.string.button_stop)
            action = NotificationControlService.ActionType.STOP
        } else {
            description = getString(R.string.desc_stopped)
            icon = R.drawable.baseline_play_arrow_24
            button = getString(R.string.button_start)
            action = NotificationControlService.ActionType.START
        }

        val startIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val startPendingIntent =
            PendingIntent.getActivity(this, 0, startIntent, PendingIntent.FLAG_IMMUTABLE)
        val toggleIntent = Intent(this, NotificationControlService::class.java).also {
            it.action = action.toString()
        }
        val togglePendingIntent =
            PendingIntent.getBroadcast(this, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(description)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(startPendingIntent)
            .addAction(icon, button, togglePendingIntent)

        with(NotificationManagerCompat.from(this)) {
            if (ActivityCompat.checkSelfPermission(
                    this@NotificationListenerService,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: request permission here
                return@with
            }
            notify(NOTIFICATION_ID, builder.build())
        }
    }

}