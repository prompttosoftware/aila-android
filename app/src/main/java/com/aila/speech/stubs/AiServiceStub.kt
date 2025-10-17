package com.aila.speech.stubs

import com.aila.speech.processor.ASRProcessor
import com.aila.speech.tutor.TutorService
import com.aila.speech.tts.TTSClient

/**
 * Stub implementation of AI services that provides predictable dummy responses
 * for use in development and unit testing.
 * Implements all core AI interfaces: ASR, tutoring logic, and TTS.
 */
class AiServiceStub : ASRProcessor, TutorService, TTSClient {

    /**
     * Simulates speech-to-text transcription.
     * Returns a dummy transcription if audio is "long enough", otherwise null
     * to simulate low confidence or silence.
     *
     * @param audio the audio data in bytes
     * @return "test utterance" if audio size > 1000, else null
     */
    override fun transcribe(audio: ByteArray): String? {
        return if (audio.size > 1000) "test utterance" else null
    }

    /**
     * Simulates a tutoring response based on conversation context.
     * Returns a fallback message if context contains "fail", otherwise a positive response.
     *
     * @param context the conversation or input context
     * @return a static response based on context
     */
    override fun respond(context: String): String {
        return if (context.contains("fail", ignoreCase = true))
            "I didn't understand. Let's try again."
        else
            "That's great!"
    }

    /**
     * Simulates text-to-speech synthesis by returning a dummy byte array.
     * The returned array represents silent PCM audio data (size 1024).
     *
     * @param text the text to synthesize
     * @return a dummy byte array representing silent audio
     */
    override fun generateSpeech(text: String): ByteArray {
        return ByteArray(1024) { 0 }
    }
}
