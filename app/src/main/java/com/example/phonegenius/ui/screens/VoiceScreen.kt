package com.example.phonegenius.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phonegenius.data.engine.RulesEngine
import com.example.phonegenius.data.model.DeviceSnapshot
import com.example.phonegenius.data.model.Thresholds
import com.example.phonegenius.data.model.VoiceExchange
import com.example.phonegenius.service.VoiceAssistant
import com.example.phonegenius.ui.components.AppCard

@Composable
fun VoiceScreen(
    snapshot: DeviceSnapshot?,
    thresholds: Thresholds,
    voiceAssistant: VoiceAssistant?,
    modifier: Modifier = Modifier
) {
    var isListening by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isSilent by remember { mutableStateOf(false) }
    var speechRate by remember { mutableFloatStateOf(1.0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val conversations = remember {
        mutableStateListOf(
            VoiceExchange(
                "init",
                System.currentTimeMillis(),
                "Aide",
                "Bonjour ! Je suis Voice Genius, votre assistant 100 % hors ligne. Dites par exemple : « Pourquoi mon téléphone chauffe ? », « Batterie faible », ou « Analyse tout »."
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    fun handleInput(text: String) {
        if (text.isBlank()) return
        val cmd = VoiceAssistant.parseCommand(text)
        val reply = when (cmd) {
            VoiceAssistant.VoiceCommand.HELP -> "Dites : pourquoi mon téléphone chauffe, batterie faible, téléphone lent, ou analyse complète."
            VoiceAssistant.VoiceCommand.SILENCE -> {
                isSilent = true
                voiceAssistant?.stopSpeaking()
                "Mode silencieux activé. Je répondrai par écrit."
            }
            VoiceAssistant.VoiceCommand.UNSILENCE -> {
                isSilent = false
                "Voix réactivée."
            }
            VoiceAssistant.VoiceCommand.REPEAT -> conversations.firstOrNull()?.reply ?: "Je n'ai encore rien dit."
            VoiceAssistant.VoiceCommand.DIAGNOSE -> {
                if (snapshot == null) "Je n'ai pas encore lu les mesures de l'appareil."
                else {
                    val rep = RulesEngine.runDiagnostic(snapshot, thresholds, text)
                    "${rep.summary} ${rep.findings.firstOrNull()?.solutions?.firstOrNull() ?: ""}"
                }
            }
        }
        conversations.add(0, VoiceExchange(System.currentTimeMillis().toString(), System.currentTimeMillis(), text, reply))
        if (!isSilent && cmd != VoiceAssistant.VoiceCommand.SILENCE) {
            voiceAssistant?.speak(reply, rate = speechRate)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Voice Genius",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Assistant vocal 100 % hors ligne. Aucun enregistrement n'est transmis à un serveur.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Hero Mic Card
        item {
            AppCard(modifier = Modifier.testTag("card_mic_hero")) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            )
                        }
                        FilledIconButton(
                            onClick = {
                                if (isListening) {
                                    isListening = false
                                    voiceAssistant?.stopListening()
                                } else {
                                    isListening = true
                                    statusMessage = null
                                    voiceAssistant?.startListening(
                                        onResult = { recognized ->
                                            isListening = false
                                            handleInput(recognized)
                                        },
                                        onError = { err ->
                                            isListening = false
                                            statusMessage = err
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .testTag("btn_mic_toggle"),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = if (isListening) "Arrêter l'écoute" else "Démarrer l'écoute",
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isListening) "Écoute en cours..." else "Touchez le micro pour parler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (statusMessage != null) {
                        Text(
                            text = statusMessage ?: "",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = true,
                            onClick = {},
                            label = { Text("Hors ligne garanti", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = !isSilent,
                            onClick = {
                                isSilent = !isSilent
                                if (isSilent) voiceAssistant?.stopSpeaking()
                            },
                            label = { Text(if (isSilent) "Muet" else "Voix active", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    if (isSilent) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.testTag("chip_voice_silent")
                        )
                    }
                }
            }
        }

        // Text input for fallback
        item {
            AppCard {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Posez votre question par écrit...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_voice_text"),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val t = inputText
                                inputText = ""
                                handleInput(t)
                            },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier.testTag("btn_send_voice")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Envoyer")
                        }
                    }
                )
            }
        }

        // History
        item {
            Text("Historique des échanges", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(conversations) { exchange ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(14.dp, 14.dp, 2.dp, 14.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = exchange.user,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(14.dp, 14.dp, 14.dp, 2.dp),
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(
                        text = exchange.reply,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
