package com.piterdev.audiobe

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.piterdev.audiobe.ui.theme.AudioBETheme
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.sleep
import java.net.ConnectException
import java.net.Socket
import kotlin.concurrent.thread


class MainActivity : ComponentActivity() {

    lateinit var mediaButtonReceiver: MediaButtonReceiver

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val intent = Intent(this@MainActivity, PlaybackService::class.java)
        startForegroundService(intent)

        setContent {

            val host = remember { mutableStateOf("192.168.1.36") }
            val port = 8080

            AudioBETheme {
                Scaffold(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.background)
                        .fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text("AudioBE", color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 2.dp))
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    musicPrev(host.value, port)
                                },
                                colors = IconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.Blue,
                                    disabledContentColor = Color.Black
                                ),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp).padding(0.dp)
                            ) {
                                Icon(Icons.Default.SkipPrevious, "Skip previous", Modifier.size(32.dp))
                            }
                            IconButton(
                                onClick = {
                                    musicPlayPause(host.value, port)
                                },
                                colors = IconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.Blue,
                                    disabledContentColor = Color.Black
                                ),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp).padding(0.dp)
                            ) {
                                Icon(Icons.Default.PlayCircleOutline, "Play/pause", Modifier.size(32.dp))
                            }
                            IconButton(
                                onClick = {
                                    musicNext(host.value, port)
                                },
                                colors = IconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.Blue,
                                    disabledContentColor = Color.Black
                                ),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp).padding(0.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, "Skip next", Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this@MainActivity, PlaybackService::class.java)
        stopService(intent)
    }

//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        println("onKeydown")
//        when (keyCode) {
//            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
//                musicPrev("192.168.1.36", 8080)
//                return true
//            }
//            KeyEvent.KEYCODE_MEDIA_NEXT -> {
//                musicNext("192.168.1.36", 8080)
//                return true
//            }
//            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
//                musicPlayPause("192.168.1.36", 8080)
//                return true
//            }
//        }
//        return super.onKeyDown(keyCode, event)
//    }

}
fun getActionIO(host: String, port: Int): Pair<InputStream, OutputStream> {
    try {
        val socket = Socket(host, port)
        val outputStream = socket.getOutputStream()

        outputStream.write(1)

        return Pair(socket.getInputStream(), outputStream)
    } catch (_: ConnectException) {
        sleep(1000)
        return getActionIO(host,port)
    }
}

fun musicPrev(host: String, port: Int) {
    Thread {
        val actionIO = getActionIO(host, port)
        actionIO.second.use {
            it.write(0)
        }
    }.start()
}


fun musicPlayPause(host: String, port: Int) {
    Thread {
        val actionIO = getActionIO(host, port)
        actionIO.second.use {
            it.write(1)
        }
    }.start()
}


fun musicNext(host: String, port: Int) {
    Thread {
        val actionIO = getActionIO(host, port)
        actionIO.second.use {
            it.write(2)
        }
    }.start()
}