// Copyright (C) 2025 by Takase

package it.eja.ntfyrelay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService.MODE_PRIVATE
import androidx.core.content.edit

class NotificationControlService : BroadcastReceiver() {
    enum class ActionType {
        START,
        STOP
    }

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val sharedPreferences = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        when (ActionType.valueOf(intent.action ?: "STOP")) {
            ActionType.START -> sharedPreferences.edit { putBoolean("ACTIVE", true) }
            ActionType.STOP -> sharedPreferences.edit { putBoolean("ACTIVE", false) }
        }
    }
}