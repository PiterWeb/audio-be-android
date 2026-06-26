package com.piterdev.audiobe

import android.content.ComponentName
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

import com.piterdev.audiobe.ui.theme.AudioBETheme
import java.io.InputStream
import java.io.OutputStream
import java.lang.Thread.sleep
import java.net.ConnectException
import java.net.Socket


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            val context = LocalContext.current

            val host = remember { mutableStateOf("192.168.1.36") }
            val port = 8080

            val mediaController = remember { mutableStateOf<MediaController?>(null)}

            val currentPlaybackState = remember { mutableIntStateOf(Player.STATE_BUFFERING) }
            val isPlaying = remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                Thread {
                    startPlayback(host.value, port)
                }.start()
            }

            LaunchedEffect(Unit) {
                buildMediaController(context = context, {
                    mediaController.value = it
                    mediaController.value?.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(playing: Boolean) {
                            isPlaying.value = playing
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            super.onPlaybackStateChanged(playbackState)
                            currentPlaybackState.intValue = playbackState
                        }
                    })
                })
            }

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
                                    Thread {
                                        val actionIO = getActionIO(host.value, port)
                                        actionIO.second.use {
                                            it.write(0)
                                        }
                                    }.start()
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
                                    Thread {
                                        val actionIO = getActionIO(host.value, port)
                                        actionIO.second.use {
                                            it.write(1)
                                        }
                                    }.start()
                                    isPlaying.value = !isPlaying.value
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
                                    Thread {
                                        val actionIO = getActionIO(host.value, port)
                                        actionIO.second.use {
                                            it.write(2)
                                        }
                                    }.start()
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

    fun buildMediaController(context: Context, onControllerCreated: (MediaController) -> Unit) {

        val mediaItem =
            MediaItem.Builder()
                .setMediaId("audiobe")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("AudioBE")
                        .build()
                )
                .build()


        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture.addListener({
            val controller = controllerFuture.get()
            onControllerCreated(controller)
            controller.setMediaItem(mediaItem)
            controller.prepare()
            controller.play()
        }, ContextCompat.getMainExecutor(context))
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

@RequiresApi(Build.VERSION_CODES.S)
fun startPlayback(host: String, port: Int) {
    try {

        val bufSize = AudioTrack.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_32BIT
        )

        val audio = AudioTrack(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(44100)
                .setEncoding(AudioFormat.ENCODING_PCM_32BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .build(),  // 24-bit
            bufSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audio.play()

        val socket = Socket(host, port)

        socket.getOutputStream().use { outputStream ->
            outputStream.write(0)
            socket.getInputStream().use { audioInputStream ->
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
        startPlayback(host,port)

    } catch (_: ConnectException) {
        sleep(1000)
        startPlayback(host,port)
    }
}


//@Composable
//fun Greeting(name: String, modifier: Modifier = Modifier) {
//    Text(
//        text = "Hello $name!",
//        modifier = modifier
//    )
//}

//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    AudioBETheme {
//        Greeting("Android")
//    }
//}