package com.example.persiankeyboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.*

class VoiceTypingActivity : Activity() {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // شفاف کردن صفحه
        window.setBackgroundDrawableResource(android.R.color.transparent)
        
        // چک کردن دسترسی میکروفون
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        } else {
            startListening()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            Toast.makeText(this, "برای تایپ صوتی، دسترسی میکروفون لازم است", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startListening() {
        Toast.makeText(this, "صحبت کنید... (آنلاین/آفلاین)", Toast.LENGTH_SHORT).show()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR") // زبان فارسی
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // اولویت با حالت آفلاین
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            
            override fun onEndOfSpeech() {
                if (!isListening) finish()
            }

            override fun onError(error: Int) {
                Toast.makeText(applicationContext, "صدایی تشخیص داده نشد یا اینترنت/زبان چک شود", Toast.LENGTH_SHORT).show()
                finish()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    // فرستادن متن به کیبورد
                    PersianKeyboardService.voiceResultListener?.invoke(matches[0])
                }
                finish()
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        isListening = true
        speechRecognizer?.startListening(intent)
    }
    
    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
