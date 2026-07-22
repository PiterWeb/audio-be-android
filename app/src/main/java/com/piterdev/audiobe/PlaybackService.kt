package com.piterdev.audiobe

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import java.lang.Thread.sleep
import java.net.ConnectException
import java.net.Socket

class PlaybackService : Service() {

    val CHANNEL_ID = "AudioBE"

    lateinit var audioThread: Thread

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
        audioThread.interrupt()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        try {

            val address = intent.getStringExtra("host").orEmpty()
            val port = intent.getIntExtra("port", 8080)

            audioThread = Thread {
                startPlayback(address, port)
            }
            audioThread.start()

            createNotificationChannel(this)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("AudioBE").setContentText("Playing music in background").setPriority(
                NotificationCompat.PRIORITY_DEFAULT).build()
            ServiceCompat.startForeground(
                this,
                100,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY_COMPATIBILITY
    }

    fun createNotificationChannel(context: Context) {
        val name = "AudioBE"
        val descriptionText = "Playing music in background"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system.
        val notificationManager: NotificationManager =
            context.getSystemService(NotificationManager::class.java) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    @RequiresApi(Build.VERSION_CODES.S)
    fun startPlayback(host: String, port: Int) {
        try {

            val socket = Socket(host, port)

            socket.getOutputStream().use { outputStream ->
                outputStream.write(0)

                val inputStream = socket.getInputStream()

                val bits = inputStream.read()

                val audioFormat = if (bits == 16) AudioFormat.ENCODING_PCM_16BIT else AudioFormat.ENCODING_PCM_32BIT

                val bufSize = AudioTrack.getMinBufferSize(
                    44100,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    audioFormat
                )

                val audio = AudioTrack(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build(),
                    AudioFormat.Builder()
                        .setSampleRate(44100)
                        .setEncoding(audioFormat)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build(),  // 24-bit
                    bufSize,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE
                )

                audio.play()

                inputStream.use { audioInputStream ->
                    while (true) {
                        val buf = ByteArray(bufSize)
                        val n = audioInputStream.read(buf)

                        if (n == -1) {
                            break
                        }

                        audio.write(buf, 0, n)
                    }
                }
            }

            sleep(1000)
            startPlayback(host, port)

        } catch (_: ConnectException) {
            sleep(1000)
            startPlayback(host, port)
        }
    }

}
