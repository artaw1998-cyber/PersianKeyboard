package com.example.persiankeyboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import java.util.*

class VoiceTypingActivity : Activity() {
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        
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
            Toast.makeText(this, "برای تایپ صوتی، دسترسی میکروفون الزامی است", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "سیستم تشخیص صدای گوگل روی گوشی شما نصب یا فعال نیست.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        Toast.makeText(this, "صحبت کنید...", Toast.LENGTH_SHORT).show()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fa-IR") 
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "خطای ضبط صدا (میکروفون مشکل دارد)"
                    SpeechRecognizer.ERROR_CLIENT -> "خطای کلاینت"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "دسترسی میکروفون داده نشده است"
                    SpeechRecognizer.ERROR_NETWORK -> "خطای شبکه (اینترنت را چک کنید)"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "زمان اینترنت به پایان رسید"
                    SpeechRecognizer.ERROR_NO_MATCH -> "صدایی تشخیص داده نشد، دوباره تلاش کنید"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "سیستم تشخیص صدا مشغول است"
                    SpeechRecognizer.ERROR_SERVER -> "خطای سرور گوگل"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "صحبتی نشنیدم"
                    else -> "خطای ناشناخته ($error)"
                }
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    finish() // بستن صفحه ضبط صدا
                    
                    // 🌟 صبر می‌کنیم تا صفحه بسته بشه و فوکوس به کیبورد برگرده، بعد متن رو می‌فرستیم
                    Handler(Looper.getMainLooper()).postDelayed({
                        PersianKeyboardService.voiceResultListener?.invoke(recognizedText)
                    }, 500) // نیم ثانیه تاخیر
                } else {
                    finish()
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        
        speechRecognizer?.startListening(intent)
    }
    
    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
