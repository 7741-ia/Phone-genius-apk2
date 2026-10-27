package com.example.phonegenius.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceAssistant(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    var isTtsReady = false
        private set

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.FRENCH)
            isTtsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
        }
    }

    fun hasOfflineSpeechRecognizer(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Moteur de reconnaissance vocale indisponible sur cet appareil.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.FRENCH.toString())
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Aucun mot détecté. Répétez plus près du micro."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Délai d'attente dépassé."
                        SpeechRecognizer.ERROR_AUDIO -> "Erreur d'enregistrement audio."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission micro requise."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Moteur vocal occupé."
                        SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Pack de reconnaissance vocale hors ligne requis. Installez le français dans Paramètres › Langues."
                        else -> "Erreur vocale ($error). Écrivez votre demande ci-dessous."
                    }
                    onError(msg)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    if (text.isNotBlank()) {
                        onResult(text)
                    } else {
                        onError("Je n'ai rien entendu.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            onError("Impossible de démarrer l'écoute : ${e.localizedMessage}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
    }

    fun speak(text: String, rate: Float = 1.0f, volume: Float = 1.0f): Boolean {
        if (!isTtsReady || tts == null) return false
        tts?.setSpeechRate(rate)
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "pg_tts_${System.currentTimeMillis()}")
        return true
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    fun shutdown() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    enum class VoiceCommand {
        HELP, SILENCE, UNSILENCE, REPEAT, DIAGNOSE
    }

    companion object {
        fun parseCommand(text: String): VoiceCommand {
            val t = text.lowercase()
            return when {
                "aide|que sais-tu|commandes".toRegex().containsMatchIn(t) -> VoiceCommand.HELP
                "réactive|reactive|parle à nouveau".toRegex().containsMatchIn(t) -> VoiceCommand.UNSILENCE
                "silence|tais-toi|muet".toRegex().containsMatchIn(t) -> VoiceCommand.SILENCE
                "répète|repete".toRegex().containsMatchIn(t) -> VoiceCommand.REPEAT
                else -> VoiceCommand.DIAGNOSE
            }
        }
    }
}
