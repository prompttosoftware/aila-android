package com.aila.domain

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.aila.data.local.ContactDao
import com.aila.data.local.ContactEntity
import com.aila.ai.AiService
import dagger.hilt.android.scopes.ViewModelScoped
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

/**
 * Manager for handling contact lifecycle operations and generating dynamic ringing narratives
 * using on-device mT5-small model via AiService.
 */
@ViewModelScoped
class ContactManager @Inject constructor(
    private val contactDao: ContactDao,
    private val aiService: AiService
) {
    companion object {
        private const val TAG = "ContactManager"
        private val BIRTHDAY_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    /**
     * Creates a new contact after validating inputs and persists it to the database.
     *
     * @param name The contact's name (non-empty)
     * @param birthday The contact's birthday in ISO 8601 format (e.g., "1990-05-20")
     * @param personality A textual description of the contact's personality
     * @param voice Voice profile identifier
     * @param language Preferred language code (e.g., "en", "es")
     * @return The created Contact domain object
     * @throws IllegalArgumentException if validation fails
     */
    suspend fun createContact(
        name: String,
        birthday: String,
        personality: String,
        voice: String,
        language: String
    ): Contact {
        validateName(name)
        validateBirthday(birthday)
        validatePersonality(personality)
        validateLanguage(language)

        val entity = ContactEntity(
            id = 0, // Room will auto-generate
            name = name.trim(),
            birthday = birthday,
            personality = personality.trim(),
            voice = voice,
            language = language,
            lastCallTime = Instant.EPOCH.toString() // Default value
        )

        val id = contactDao.insert(entity)
        return entity.copy(id = id).toDomain()
    }

    /**
     * Generates a realistic narrative for what happened to the contact since their last call.
     * Uses mT5-small model through AiService with contextual prompt including time delta.
     *
     * @param contact The contact to generate a narrative for
     * @return A generated narrative string
     */
    suspend fun generateRingingNarrative(contact: Contact): String {
        val lastCallInstant = Instant.parse(contact.lastCallTime)
        val now = Instant.now()
        val deltaHours = Duration.between(lastCallInstant, now).toHours()

        val prompt = buildString {
            append("Generate a realistic, concise life event for ${contact.name} ")
            append("since the last interaction on ${contact.lastCallTime}. ")
            append("Current context: ${contact.personality}. ")
            append("Time elapsed: $deltaHours hours. ")
            append("Response should be 1-2 sentences, natural and grounded in reality.")
        }

        return try {
            aiService.generateText(prompt)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate AI narrative", e)
            getDefaultNarrative(contact, deltaHours)
        }
    }

    /**
     * Updates the last call timestamp for a contact after a call ends.
     *
     * @param contactId The ID of the contact
     */
    suspend fun updateLastCallTime(contactId: Long) {
        val contact = contactDao.getById(contactId) ?: return
        val updated = contact.copy(lastCallTime = Instant.now().toString())
        contactDao.update(updated)
    }

    // Validation helpers
    private fun validateName(name: String) {
        if (name.trim().isEmpty()) {
            throw IllegalArgumentException("Name cannot be empty")
        }
    }

    private fun validateBirthday(birthday: String) {
        try {
            LocalDate.parse(birthday, BIRTHDAY_FORMATTER)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("Birthday must be in ISO 8601 format (YYYY-MM-DD)")
        }
    }

    private fun validatePersonality(personality: String) {
        if (personality.trim().isEmpty()) {
            throw IllegalArgumentException("Personality description cannot be empty")
        }
    }

    private fun validateLanguage(language: String) {
        if (language.trim().isEmpty()) {
            throw IllegalArgumentException("Language cannot be empty")
        }
        // Basic validation for language tag (e.g., "en", "fr", "es")
        if (!Regex("^[a-z]{2}$").matches(language.trim())) {
            throw IllegalArgumentException("Language must be a valid two-letter code")
        }
    }

    /**
     * Returns a fallback narrative when AI generation fails.
     * This is also used in debug builds if stubbing is enabled.
     */
    @VisibleForTesting
    internal fun getDefaultNarrative(contact: Contact, deltaHours: Long): String {
        return when {
            deltaHours < 24 -> "${contact.name} was just thinking about you and wondered how you've been."
            deltaHours < 168 -> "${contact.name} recently came back from a short trip and has a story to share."
            deltaHours < 720 -> "${contact.name} has been busy with work lately, but made time to call you."
            else -> "${contact.name} reflects on how much has changed since you last spoke, and shares some big news."
        }
    }
}

/**
 * Domain model representing a contact.
 */
data class Contact(
    val id: Long,
    val name: String,
    val birthday: String,
    val personality: String,
    val voice: String,
    val language: String,
    val lastCallTime: String
)

// Extension to convert between data layer and domain layer
fun ContactEntity.toDomain() = Contact(
    id = id,
    name = name,
    birthday = birthday,
    personality = personality,
    voice = voice,
    language = language,
    lastCallTime = lastCallTime
)
