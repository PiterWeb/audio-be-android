package com.piterdev.audiobe

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
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

    // lateinit var mediaButtonReceiver: MediaButtonReceiver

    var host = "0.0.0.0"
    var port = 8080

    // Instantiate a new DiscoveryListener
    private val discoveryListener = object : NsdManager.DiscoveryListener {

        var lock: WifiManager.MulticastLock? = null
        var nsdManager: NsdManager? = null

        // Called as soon as service discovery begins.
        override fun onDiscoveryStarted(regType: String) {
            lock?.acquire()
            println("Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            // A service was found! Do something with it.
            println("Service discovery success$service")
            when {
                service.serviceType != "_ws._tcp." -> return
                service.serviceName != "AudioBE" -> return
                else -> {
                    nsdManager?.resolveService(service, resolveListener)
                     nsdManager?.stopServiceDiscovery(this)
                }
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            // When the network service is no longer available.
            // Internal bookkeeping code goes here.
            println("service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            lock?.release()
            println("Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            println("Discovery failed: Error code:$errorCode")
            nsdManager?.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            println("Discovery failed: Error code:$errorCode")
            nsdManager?.stopServiceDiscovery(this)
        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
           println("Resolve failed: $errorCode")
        }

        @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            println("Resolve Succeeded. $serviceInfo")
            port = serviceInfo.port
            host = serviceInfo.hostAddresses.firstOrNull()?.hostAddress.orEmpty()

            val intent = Intent(this@MainActivity, PlaybackService::class.java)

            intent.putExtra("host", host)
            intent.putExtra("port", port)

            startForegroundService(intent)

        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val nsdManager = getSystemService(NSD_SERVICE) as NsdManager
        discoveryListener.nsdManager = nsdManager

        val wifi = getSystemService(WIFI_SERVICE) as WifiManager?
        val lock = wifi?.createMulticastLock("audiobe")
        discoveryListener.lock = lock

        nsdManager.discoverServices("_ws._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)

        setContent {

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
                                    musicPrev(host, port)
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
                                    musicPlayPause(host, port)
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
                                    musicNext(host, port)
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