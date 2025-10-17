package com.aila.domain

import com.aila.data.local.VocabularyProficientDao
import com.aila.data.local.VocabularyStrugglingDao
import com.aila.model.Word
import com.aila.model.UtteranceResult
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class VocabularyTracker @Inject constructor(
    private val proficientDao: VocabularyProficientDao,
    private val strugglingDao: VocabularyStrugglingDao
) {
    suspend fun processUtterance(text: String, targetLanguage: String): UtteranceResult {
        return withContext(Dispatchers.IO) {
            val words = tokenize(text)
            val result = UtteranceResult()

            for (word in words) {
                val normalizedWord = word.lowercase().trim()
                var strugglingWord = strugglingDao.find(normalizedWord, targetLanguage)

                if (strugglingWord != null) {
                    if (isUsedCorrectly(strugglingWord, text)) {
                        // Apply success logic: decrement severity
                        strugglingWord.severity = (strugglingWord.severity - 1).coerceAtLeast(0)

                        val now = Instant.now()
                        val lastReviewAgo = ChronoUnit.HOURS.between(strugglingWord.lastReviewed, now)

                        // If severity is zero and used correctly within 24h, promote to proficient
                        if (strugglingWord.severity == 0 && lastReviewAgo <= 24) {
                            strugglingDao.delete(strugglingWord)
                            proficientDao.insert(
                                Word(
                                    word = normalizedWord,
                                    language = targetLanguage,
                                    masteredAt = now
                                )
                            )
                        } else {
                            // Otherwise, update interval and ease factor using SM-2
                            val successFactor = 1.3 + (0.1 * (5 - strugglingWord.easeFactor)) // Adjusted based on current ease
                            strugglingWord.easeFactor = (strugglingWord.easeFactor + 0.1).coerceAtMost(2.5)
                            strugglingWord.currentInterval = (strugglingWord.currentInterval * strugglingWord.easeFactor).toInt()
                            strugglingWord.nextInterval = now.plusSeconds((strugglingWord.currentInterval * 3600).toLong())
                            strugglingWord.lastReviewed = now

                            strugglingDao.update(strugglingWord)
                        }
                        result.addCorrectedWord(normalizedWord, severity = strugglingWord.severity)
                    } else {
                        // Incorrect usage: increase severity, retries, and reset interval
                        strugglingWord.severity = (strugglingWord.severity + 1).coerceAtMost(3)
                        strugglingWord.retriesNeeded++
                        strugglingWord.easeFactor = (strugglingWord.easeFactor * 0.8).coerceAtLeast(1.3)
                        strugglingWord.currentInterval = 1 // Reset to 1 hour
                        strugglingWord.nextInterval = Instant.now().plusSeconds(3600)
                        strugglingWord.lastReviewed = Instant.now()

                        strugglingDao.update(strugglingWord)
                        result.addStrugglingWord(normalizedWord, strugglingWord.severity)
                    }
                } else {
                    // Word is not in struggling list
                    // Optionally check if in proficient list and recently used?
                    // For now, just ignore or log exposure?
                }
            }

            result
        }
    }

    suspend fun getReviewWords(): List<Word> {
        return withContext(Dispatchers.IO) {
            strugglingDao.getDueForReview(Instant.now())
                .map { Word(word = it.word, language = it.language) }
        }
    }

    private fun tokenize(text: String): List<String> {
        return text.split(Regex("\\s+|[,.;:!?\"'()\\[\\]{}]+")).filter { it.isNotBlank() }
    }

    private fun isUsedCorrectly(word: StrugglingWord, context: String): Boolean {
        // Simplified correctness check – in practice, integrate with grammar checker or usage model
        // For now, assume presence in utterance is "correct"
        return context.lowercase().contains(word.word.lowercase())
    }
}
