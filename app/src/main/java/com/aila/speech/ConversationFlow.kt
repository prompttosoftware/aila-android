package com.aila.speech

import androidx.work.*
import kotlinx.coroutines.*
import javax.inject.Inject
import java.util.concurrent.TimeUnit

class ConversationFlow @Inject constructor(
    private val asr: ASRProcessor,
    private val tutor: TutorService,
    private val tts: TTSClient,
    private val vocab: VocabularyTracker,
    private val settings: AppSettings,
    private val workManager: WorkManager
) {
    private var consecutiveFailures = 0
    private val history = mutableListOf<String>()
    private var currentContact: Contact? = null
    private var fallbackActive = false
    private val contextWindowSize = 10 // number of recent exchanges to keep in context

    suspend fun startConversation(contact: Contact) {
        currentContact = contact
        // Load contact-specific prompt (personality + life history)
        val initialPrompt = loadContactPrompt(contact)
        history.clear()
        history.add("System: $initialPrompt")
        // Start listening for user speech
        // In practice, this would hook into a microphone stream or callback
    }

    suspend fun handleUserSpeech(audio: ByteArray) {
        val text = asr.transcribe(audio) ?: run {
            handleFailure()
            return
        }

        // Process the utterance for vocabulary tracking
        val vocabResult = vocab.processUtterance(text, settings.currentLanguage)
        // Build context with conversation history and vocabulary insights
        val context = buildContext(history, vocabResult)

        // Generate response using appropriate mode based on failure count
        val response = if (consecutiveFailures >= 2) {
            fallbackActive = true
            val fallbackPrompt = generateFallbackPrompt(text, consecutiveFailures)
            tutor.respondInNative(context + "\n$ fallbackPrompt", settings.nativeLanguage)
        } else {
            fallbackActive = false
            tutor.respond(context)
        }

        // Update history
        history.add("User: $text")
        history.add("AI: $response")

        // Trim history to prevent memory growth
        if (history.size > contextWindowSize * 2) {
            history.subList(0, history.size - contextWindowSize * 2).clear()
        }

        // Speak the response
        if (response.isNotBlank()) {
            tts.speak(response)
            consecutiveFailures = 0 // Reset on successful response
        } else {
            handleFailure()
        }

        // Schedule vocabulary review jobs based on SRS intervals
        launchVocabularyReviewJobs()
    }

    private fun handleFailure() {
        consecutiveFailures++
        when (consecutiveFailures) {
            1 -> {
                // "I didn't understand. Did you mean X?"
                val correction = asr.generateCorrectionSuggestions()?.firstOrNull()
                val clarification = if (correction != null) {
                    "I didn't understand. Did you mean '$correction'?"
                } else {
                    "I didn't catch that. Could you rephrase?"
                }
                history.add("AI: $clarification")
                runBlocking { tts.speak(clarification) }
            }
            2 -> {
                // Repeat with more clarification
                val clarification = "Let's try again. Please speak clearly. " +
                        "Use simple ${settings.currentLanguage} phrases."
                history.add("AI: $clarification")
                runBlocking { tts.speak(clarification) }
            }
            3 -> {
                // Fallback to native language
                val fallbackMessage = "Let me switch to ${settings.nativeLanguage} to help. " +
                        "How would you say the phrase in your own words?"
                history.add("AI: $fallbackMessage")
                runBlocking { tts.speakInLanguage(fallbackMessage, settings.nativeLanguage) }
            }
        }
    }

    private fun loadContactPrompt(contact: Contact): String {
        return "You are ${contact.name}, a ${contact.personality} person from ${contact.hometown}. " +
                "You are interested in ${contact.interests.joinToString(", ")}. " +
                "Engage the user in natural conversation to practice ${settings.currentLanguage}."
    }

    private fun buildContext(history: List<String>, vocabInsight: String): String {
        val context = history.takeLast(contextWindowSize * 2).joinToString("\n")
        return if (vocabInsight.isNotBlank()) {
            "$context\n\nVocabularyContext: $vocabInsight"
        } else {
            context
        }
    }

    private fun generateFallbackPrompt(userInput: String, failures: Int): String {
        return when (failures) {
            1 -> "Clarify: I heard '$userInput'. What did you intend to say?"
            2 -> "Rephrase in ${settings.currentLanguage}: Use simple words to express your thought."
            3 -> "Now responding in ${settings.nativeLanguage} for clarity. Please try to answer in ${settings.currentLanguage} when ready."
            else -> "Let's simplify. Can you repeat using basic vocabulary?"
        }
    }

    private fun launchVocabularyReviewJobs() {
        // Schedule review jobs based on SRS intervals (e.g., 1 day, 3 days, 1 week)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        vocab.getScheduledReviews().forEach { word ->
            val workRequest = OneTimeWorkRequestBuilder<VocabularyReviewWorker>()
                .setConstraints(constraints)
                .setInitialDelay(word.nextReviewIn, TimeUnit.SECONDS)
                .setInputData(workDataOf("WORD" to word.text, "LANGUAGE" to settings.currentLanguage))
                .build()

            workManager.enqueueUniqueWork(
                "review_${word.text}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }
}
