package com.luk4stana.echosphere

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class EchoMessage(val text: String, val device: String, val time: String, val timestamp: Long, val isMine: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("splash") }
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}
            LaunchedEffect(Unit) {
                val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
                } else { arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) }
                permissionLauncher.launch(permissions)
                delay(2000)
                currentScreen = "main"
            }
            MaterialTheme { if (currentScreen == "splash") SplashScreen() else EchoScreen() }
        }
    }
}

@Composable
fun SplashScreen() {
    Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), Alignment.Center) {
        Text("ECHO-SPHERE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EchoScreen() {
    val context = LocalContext.current
    val echoManager = remember { EchoManager(context) }
    var isRunning by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }
    var msgCount by remember { mutableIntStateOf(0) }
    var sessionStartTime by remember { mutableLongStateOf(0L) }

    val messageLog = remember { mutableStateListOf<EchoMessage>() }
    val discoveredDevices = remember { mutableStateSetOf<String>() }
    val duplicateCache = remember { mutableStateSetOf<String>() }
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseSize by infiniteTransition.animateFloat(110f, 300f, infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing)), label = "size")
    val pulseAlpha by infiniteTransition.animateFloat(0.5f, 0f, infiniteRepeatable(tween(2000, easing = LinearOutSlowInEasing)), label = "alpha")

    Column(Modifier.fillMaxSize().background(Color(0xFF0A0A0A)), horizontalAlignment = Alignment.CenterHorizontally) {

        // Header con scritta DEVICES IDENTIFIED
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 30.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("github.com/Luk4Stana", color = Color(0xFFFF007A), fontSize = 10.sp, modifier = Modifier.clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Luk4Stana")))
            })
            Surface(color = Color(0xFFFF007A).copy(0.1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFFF007A))) {
                Text("DEVICES IDENTIFIED: ${discoveredDevices.size}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
            }
        }

        // Input
        Row(Modifier.fillMaxWidth(0.9f), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textInput, onValueChange = { textInput = it },
                modifier = Modifier.weight(1f), label = { Text("max 25 characters", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            IconButton(
                onClick = {
                    if(isRunning && textInput.isNotBlank()) {
                        msgCount++
                        val modelTag = Build.MODEL.take(5).uppercase()
                        echoManager.startEcho("$modelTag|$msgCount|$textInput")
                        messageLog.add(0, EchoMessage(textInput, Build.MODEL, timeFormat.format(Date()), System.currentTimeMillis(), true))
                        textInput = ""
                    }
                },
                Modifier.padding(start = 8.dp).size(50.dp).background(Color(0xFFFF007A), CircleShape)
            ) { Icon(Icons.Default.Send, "Send", tint = Color.White) }
        }

        // Radar con scritta START RESEARCH
        Box(Modifier.weight(1f), Alignment.Center) {
            if (isRunning) {
                Box(Modifier.size(pulseSize.dp).border(2.dp, Color(0xFFFF007A).copy(alpha = pulseAlpha), CircleShape))
            }
            Box(Modifier.size(110.dp).clip(CircleShape).background(if(isRunning) Color.White else Color(0xFF1A1A1A))
                .border(2.dp, Color(0xFFFF007A), CircleShape)
                .clickable {
                    if(!isRunning) {
                        sessionStartTime = System.currentTimeMillis()
                        messageLog.clear()
                        discoveredDevices.clear()
                        duplicateCache.clear()
                        echoManager.startScanning { raw ->
                            val parts = raw.split("|", limit = 3)
                            if(parts.size >= 3) {
                                val dev = parts[0]; val id = parts[1]; val txt = parts[2]
                                val signature = "$dev-$id"
                                if (!duplicateCache.contains(signature) && dev != Build.MODEL.take(5).uppercase()) {
                                    discoveredDevices.add(dev)
                                    duplicateCache.add(signature)
                                    messageLog.add(0, EchoMessage(txt, dev, timeFormat.format(Date()), System.currentTimeMillis(), false))
                                }
                            }
                        }
                        isRunning = true
                    } else {
                        echoManager.stopAll()
                        isRunning = false
                    }
                }, Alignment.Center) {
                Text(
                    if(isRunning) "STOP" else "START\nRESEARCH",
                    color = if(isRunning) Color.Black else Color.White,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Chat Feed
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("LIVE CHAT FEED", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
            Card(Modifier.fillMaxWidth().height(250.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A))) {
                val currentMsgs = messageLog.filter { (System.currentTimeMillis() - it.timestamp) < 120000 }
                LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(currentMsgs) { msg ->
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = if(msg.isMine) Alignment.End else Alignment.Start) {
                            Text("${msg.device} • ${msg.time}", color = Color.Gray, fontSize = 10.sp)
                            Surface(
                                color = if(msg.isMine) Color(0xFFFF007A).copy(0.2f) else Color.DarkGray,
                                shape = RoundedCornerShape(12.dp),
                                border = if(msg.isMine) BorderStroke(1.dp, Color(0xFFFF007A)) else null
                            ) {
                                Text(msg.text, color = Color.White, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}