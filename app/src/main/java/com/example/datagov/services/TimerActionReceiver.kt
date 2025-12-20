package com.example.datagov.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TimerActionReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "TimerActionReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Action received: ${intent.action}")
        
        val serviceIntent = Intent(context, TimerService::class.java).apply {
            action = intent.action
        }
        
        when (intent.action) {
            TimerService.ACTION_PAUSE -> {
                Log.d(TAG, "Forwarding PAUSE action to service")
                context.startService(serviceIntent)
            }
            TimerService.ACTION_RESUME -> {
                Log.d(TAG, "Forwarding RESUME action to service")
                context.startService(serviceIntent)
            }
            TimerService.ACTION_STOP -> {
                Log.d(TAG, "Forwarding STOP action to service")
                context.startService(serviceIntent)
            }
        }
    }
}
