package com.aila.data

import android.text.Html
import android.text.TextUtils
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale
import java.util.regex.Pattern

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val birthday: String,
    val voice: String,
    val language: String,
    val lastCallTime: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    // Use a private field with a custom setter to ensure sanitization
    private var _personality: String = ""
    val personality: String
        get() = _personality

    init {
        require(name.isNotBlank()) { "Name cannot be null or empty" }
        require(isValidIsoDate(birthday)) { "Birthday must be in valid ISO 8601 format" }
        require(voice in listOf("male", "female", "neutral")) { "Voice must be one of: male, female, neutral" }
        require(isValidLanguageCode(language)) { "Language must be a valid BCP-47 code" }

        // Sanitize and set personality
        _personality = sanitizePersonality(personality)
    }

    companion object {
        private val ISO_DATE_PATTERN = Pattern.compile(
            "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])(?:T(?:[01]\\d|2[0-3]):[0-5]\\d:[0-5]\\d(?:\\.\\d{1,3})?(?:Z|[+-](?:[01]\\d|2[0-3]):[0-5]\\d)?)?$"
        )

        private val BCP47_PATTERN = Pattern.compile(
            "^[a-zA-Z]{2,3}(?:-[a-zA-Z]{3}(?:-[a-zA-Z]{3}){0,2})?(?:-[A-Z]{2}|-[A-Z]{5,8})?(?:-[0-9][0-9A-Z]{3})?(?:-[0-9A-WYZ](?:-[0-9A-Z]{2,8})+)*(?:-x(?:-[0-9A-Za-z]{1,8})+)?\$"
        )

        private fun isValidIsoDate(date: String): Boolean {
            return ISO_DATE_PATTERN.matcher(date).matches()
        }

        private fun isValidLanguageCode(code: String): Boolean {
            if (!BCP47_PATTERN.matcher(code).matches()) return false
            return Locale.forLanguageTag(code).isWellFormed
        }

        private fun sanitizePersonality(raw: String): String {
            if (raw.isEmpty()) return ""
            val spanned = Html.fromHtml(raw)
            return TextUtils.trimNoCopySpans(spanned).toString()
        }
    }

    // Allow personality to be passed in constructor but sanitized on set
    constructor(
        id: Long = 0,
        name: String,
        birthday: String,
        personality: String,
        voice: String,
        language: String,
        lastCallTime: Long = 0,
        createdAt: Long = System.currentTimeMillis()
    ) : this(id, name, birthday, voice, language, lastCallTime, createdAt) {
        _personality = sanitizePersonality(personality)
    }
}
