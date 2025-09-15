package com.example.realtimeweather.utils

import android.content.Context
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer.createSpeechRecognizer
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.mutableStateOf

class SpeechRecognizerHelper(
    private val context: Context,
    private val onResult: (String) -> Unit,           // Kết quả cuối cùng
    private val onPartialResult: (String) -> Unit,    // Kết quả tạm thời (hiển thị ngay lập tức)
    private val onError: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = mutableStateOf(false)
    private val TAG = "SpeechRecognizerHelper"

    init {
        setupSpeechRecognizer()
    }

    private fun setupSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = createSpeechRecognizer(context).apply {
                    setRecognitionListener(createRecognitionListener())
                }
                Log.d(TAG, "Speech recognizer initialized successfully")
            } else {
                onError("Speech recognition not available on this device")
                Log.e(TAG, "Speech recognition not available")
            }
        } catch (e: Exception) {
            onError("Failed to initialize speech recognizer: ${e.message}")
            Log.e(TAG, "Error initializing speech recognizer", e)
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening.value = true
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech detected")
                onPartialResult("") // Xóa textfield khi bắt đầu nói
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Có thể dùng cho visual feedback về âm lượng
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening.value = false
                Log.d(TAG, "End of speech")
            }

            override fun onError(error: Int) {
                isListening.value = false
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Lỗi ghi âm"
                    SpeechRecognizer.ERROR_CLIENT -> "Lỗi client"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Không đủ quyền"
                    SpeechRecognizer.ERROR_NETWORK -> "Lỗi mạng"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout mạng"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Không nhận diện được giọng nói"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Trình nhận diện đang bận"
                    SpeechRecognizer.ERROR_SERVER -> "Lỗi server"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Không có giọng nói"
                    else -> "Lỗi không xác định: $error"
                }
                Log.e(TAG, "Speech recognition error: $errorMessage")
                onError(errorMessage)
            }

            override fun onResults(results: Bundle?) {
                isListening.value = false
                try {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val bestMatch = matches[0]
                        Log.d(TAG, "Speech recognized: $bestMatch")
                        onResult(bestMatch) // Gửi kết quả cuối cùng
                    } else {
                        Log.d(TAG, "No speech recognized")
                        onError("Không nhận diện được giọng nói")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing results", e)
                    onError("Lỗi xử lý kết quả")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // Hiển thị kết quả tạm thời ngay lập tức
                try {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partialMatch = matches[0]
                        Log.d(TAG, "Partial result: $partialMatch")
                        onPartialResult(partialMatch) // Gửi kết quả tạm thời
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing partial results", e)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        if (isListening.value) {
            Log.w(TAG, "Already listening, ignoring start request")
            return
        }

        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy nói tên thành phố...")
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Bật kết quả tạm thời
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN") // Tiếng Việt
                }

                speechRecognizer?.startListening(intent)
                Log.d(TAG, "Started listening for speech")
            } else {
                onError("Trình nhận diện giọng nói không khả dụng")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for speech recognition", e)
            onError("Cần quyền truy cập micro")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            onError("Lỗi khởi động nhận diện giọng nói: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            if (isListening.value) {
                speechRecognizer?.stopListening()
                isListening.value = false
                Log.d(TAG, "Stopped listening")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }

    fun cancel() {
        try {
            speechRecognizer?.cancel()
            isListening.value = false
            Log.d(TAG, "Cancelled speech recognition")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
    }

    fun destroy() {
        try {
            cancel()
            speechRecognizer?.destroy()
            Log.d(TAG, "Speech recognizer destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        }
    }

    fun isListening(): Boolean = isListening.value
}