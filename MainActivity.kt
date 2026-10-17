package com.aayu.ai

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val Night = Color(0xFF050B18)
private val Panel = Color(0xFF0B1830)
private val Cyan = Color(0xFF50D8FF)
private val Blue = Color(0xFF3D7BFF)
private data class ChatLine(val who: String, val text: String)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault() }
        setContent { AayuApp(this) }
    }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
    fun speak(text: String) { tts?.speak(text.take(2500), TextToSpeech.QUEUE_FLUSH, null, "aayu-reply") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AayuApp(activity: MainActivity) {
    val prefs = remember { activity.getSharedPreferences("aayu", 0) }
    var apiKey by remember { mutableStateOf(prefs.getString("api_key", "") ?: "") }
    var showSettings by remember { mutableStateOf(apiKey.isBlank()) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var speakReplies by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("SYSTEM ONLINE • READY") }
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<ChatLine>() }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        val saved = prefs.getString("history", "") ?: ""
        if (saved.isNotBlank()) try {
            val arr = JSONArray(saved)
            for (i in 0 until minOf(arr.length(), 80)) { val o = arr.getJSONObject(i); history.add(ChatLine(o.optString("who"), o.optString("text"))) }
        } catch (_: Exception) { }
        if (history.isEmpty()) history.add(ChatLine("aayu", "Hello! I’m AAYU, your personal AI assistant. What can I help you with today?"))
    }
    fun persist() {
        val arr = JSONArray()
        history.takeLast(80).forEach { arr.put(JSONObject().put("who", it.who).put("text", it.text)) }
        prefs.edit().putString("history", arr.toString()).apply()
    }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val words = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!words.isNullOrBlank()) input = words
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // The selected image is kept as a URI for user context; this starter build sends text only.
            input = (input + " [I selected an image. Please tell me how to attach it for image analysis.] ").trim()
            status = "IMAGE SELECTED • IMAGE API HOOKUP NEEDED"
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Cyan, background = Night, surface = Panel)) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF07172C), Night, Color(0xFF02050D))))) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(44.dp).background(Brush.linearGradient(listOf(Cyan, Blue)), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Bolt, null, tint = Night, modifier = Modifier.size(29.dp))
                        }
                        Column {
                            Text("AAYU", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                            Text("PERSONAL INTELLIGENCE", color = Cyan, fontSize = 9.sp, letterSpacing = 2.sp)
                        }
                    }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Settings", tint = Cyan) }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = .28f), RoundedCornerShape(18.dp)).background(Panel.copy(alpha = .72f), RoundedCornerShape(18.dp)).padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(62.dp).border(1.dp, Cyan.copy(alpha = .75f), CircleShape).padding(6.dp).border(1.dp, Blue, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Hub, null, tint = Cyan, modifier = Modifier.size(34.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("NEURAL CORE", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(status, color = Cyan, fontSize = 10.sp, letterSpacing = 1.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { repeat(18) { i -> Box(Modifier.weight(1f).height((5 + (i % 5) * 3).dp).background(if (i < 14) Cyan.copy(alpha = .85f) else Blue.copy(alpha = .25f), RoundedCornerShape(3.dp))) } }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureChip("CHAT", Icons.Default.Forum, Modifier.weight(1f))
                    FeatureChip("VOICE", Icons.Default.Mic, Modifier.weight(1f))
                    FeatureChip("VISION", Icons.Default.Visibility, Modifier.weight(1f))
                    FeatureChip("MEMORY", Icons.Default.Memory, Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Text("CONVERSATION", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                    items(history) { line ->
                        val user = line.who == "you"
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
                            Column(Modifier.widthIn(max = 560.dp).background(if (user) Color(0xFF102B4A) else Panel, RoundedCornerShape(16.dp)).border(1.dp, (if (user) Blue else Cyan).copy(alpha = .28f), RoundedCornerShape(16.dp)).padding(12.dp)) {
                                Text(if (user) "YOU" else "AAYU", color = if (user) Color(0xFF9CB8FF) else Cyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(line.text, color = Color(0xFFEAF5FF), fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                    if (busy) item { Text("AAYU IS THINKING…", color = Cyan, fontSize = 11.sp, letterSpacing = 2.sp) }
                }
                LaunchedEffect(history.size, busy) { if (history.isNotEmpty()) listState.animateScrollToItem(history.lastIndex) }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().border(1.dp, Cyan.copy(alpha = .4f), RoundedCornerShape(18.dp)).background(Color(0xFF091426), RoundedCornerShape(18.dp)).padding(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { imageLauncher.launch("image/*") }) { Icon(Icons.Default.AddPhotoAlternate, "Choose image", tint = Cyan) }
                    BasicTextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f).padding(horizontal = 5.dp), textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp), cursorBrush = androidx.compose.ui.graphics.SolidColor(Cyan), decorationBox = { inner -> Box { if (input.isEmpty()) Text("Ask AAYU anything…", color = Color.Gray, fontSize = 14.sp); inner() } })
                    IconButton(onClick = { val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); try { voiceLauncher.launch(intent) } catch (_: Exception) { status = "VOICE INPUT NOT AVAILABLE" } }) { Icon(Icons.Default.Mic, "Speak", tint = Cyan) }
                    IconButton(enabled = !busy && input.isNotBlank(), onClick = {
                        val prompt = input.trim(); if (prompt.isBlank()) return@IconButton
                        input = ""; history.add(ChatLine("you", prompt)); persist(); busy = true; status = "CONNECTING TO GEMINI • THINKING"
                        scope.launch {
                            val answer = withContext(Dispatchers.IO) { askGemini(apiKey, history.dropLast(0).takeLast(18)) }
                            history.add(ChatLine("aayu", answer)); persist(); busy = false
                            status = if (answer.startsWith("Connection issue:") || answer.startsWith("API error:")) "CHECK CONNECTION / API QUOTA" else "SYSTEM ONLINE • RESPONSE RECEIVED"
                            if (speakReplies) activity.speak(answer)
                        }
                    }) { Icon(Icons.Default.Send, "Send", tint = if (!busy && input.isNotBlank()) Cyan else Color.Gray) }
                }
                Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MODEL  •  GEMINI 3.8 FLASH", color = Cyan.copy(alpha = .7f), fontSize = 9.sp, letterSpacing = 1.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("VOICE REPLY", color = Color.Gray, fontSize = 9.sp); Switch(checked = speakReplies, onCheckedChange = { speakReplies = it }, modifier = Modifier.height(26.dp)) }
                }
            }
            if (showSettings) AlertDialog(onDismissRequest = { showSettings = false }, containerColor = Panel, title = { Text("AAYU CONFIGURATION", color = Cyan) }, text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Text("Gemini API key", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), placeholder = { Text("Paste your API key") })
                    Spacer(Modifier.height(8.dp))
                    Text("Model: gemini-3.8-flash", color = Cyan, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Create a key in Google AI Studio. This starter app stores the key on this device; do not share the APK with your key saved. For public distribution, use a secure backend proxy. Image selection and voice input UI are included, but image analysis needs the image upload integration in a future build.", color = Color.LightGray, fontSize = 12.sp, lineHeight = 17.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { history.clear(); history.add(ChatLine("aayu", "Memory cleared. Ready for a fresh conversation.")); persist() }, modifier = Modifier.fillMaxWidth()) { Text("CLEAR CONVERSATION MEMORY") }
                }
            }, confirmButton = { TextButton(onClick = { prefs.edit().putString("api_key", apiKey.trim()).apply(); showSettings = false; status = if (apiKey.isBlank()) "API KEY NEEDED" else "SYSTEM ONLINE • KEY SAVED" }) { Text("SAVE", color = Cyan) } }, dismissButton = { TextButton(onClick = { showSettings = false }) { Text("CANCEL", color = Color.LightGray) } })
        }
    }
}

@Composable
private fun FeatureChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Row(modifier.background(Color(0xFF0B1A30), RoundedCornerShape(9.dp)).border(1.dp, Cyan.copy(alpha = .2f), RoundedCornerShape(9.dp)).padding(horizontal = 7.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)); Text(label, color = Color(0xFFD9F4FF), fontSize = 8.sp, letterSpacing = .5.sp, fontWeight = FontWeight.Bold)
    }
}

private fun askGemini(apiKey: String, messages: List<ChatLine>): String {
    if (apiKey.isBlank()) return "API key needed. Open Settings, add your Google AI Studio API key, then send your message again."
    var conn: HttpURLConnection? = null
    return try {
        conn = (URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=" + java.net.URLEncoder.encode(apiKey, "UTF-8")).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 60000; doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val contents = JSONArray()
        messages.takeLast(18).forEach { m ->
            contents.put(JSONObject().put("role", if (m.who == "you") "user" else "model").put("parts", JSONArray().put(JSONObject().put("text", m.text)))
        }
        val body = JSONObject().put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", "You are AAYU, a friendly, capable personal AI assistant. Be accurate, practical, clear, and warm. Do not claim to control device features unless explicitly integrated. If uncertain, say so.")))).put("contents", contents).put("generationConfig", JSONObject().put("maxOutputTokens", 4096))
        conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        val code = conn.responseCode
        val responseText = (if (code in 200..299) conn.inputStream else conn.errorStream).bufferedReader().use { it.readText() }
        val obj = JSONObject(responseText)
        if (code !in 200..299) {
            val msg = obj.optJSONObject("error")?.optString("message") ?: "Request failed ($code)"
            return when {
                code == 429 -> "API error: Gemini quota/rate limit reached. Wait a little, check your Google AI Studio project quota and billing, then try again."
                code == 400 || code == 403 -> "API error: $msg. Check that your API key and Gemini API access are valid."
                else -> "API error: $msg"
            }
        }
        val candidates = obj.optJSONArray("candidates")
        val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
        val out = StringBuilder()
        if (parts != null) for (i in 0 until parts.length()) out.append(parts.optJSONObject(i)?.optString("text", "") ?: "")
        out.toString().trim().ifBlank { "I received an empty response. Please try again." }
    } catch (e: java.net.SocketTimeoutException) { "Connection issue: Gemini took too long to respond. Check your internet and try again." }
      catch (e: Exception) { "Connection issue: ${e.message ?: "Unable to connect to Gemini"}. Check your internet and API key." }
    finally { conn?.disconnect() }
}
