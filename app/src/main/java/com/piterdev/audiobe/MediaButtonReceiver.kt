package com.piterdev.audiobe

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        println("OnReceive")
        when (intent.action) {
            Intent.ACTION_MEDIA_BUTTON -> {
                println("Media Button")
            }
        }
    }
}