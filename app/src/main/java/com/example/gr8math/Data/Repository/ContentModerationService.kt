package com.example.gr8math.Data.Repository

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

/**
 * A service that mimics the web's checkContentModeration logic.
 */
object ContentModerationService {

    /**
     * Result returned by the moderation check.
     * @param isSafe true if the text is clean, false otherwise.
     * @param offendingWord the word/link that triggered the flag, or null.
     * @param reasonCode "Banned Word", "Suspicious Link", etc.
     */
    data class ModerationResult(
        val isSafe: Boolean,
        val offendingWord: String?,
        val reasonCode: String?
    )

    @Serializable
    private data class BannedWord(val word: String)

    /**
     * Checks the given text for banned words and suspicious links.
     * @param db Your Supabase client instance (e.g., SupabaseService.client).
     * @param text The combined title + content to scan.
     * @return A [ModerationResult] indicating safety status.
     */
    suspend fun checkContentModeration(
        db: io.github.jan.supabase.SupabaseClient,  // Adjust import to your client type
        text: String
    ): ModerationResult {
        return try {
            // 1. Fetch banned words from the database
            val bannedWords = db.from("banned_words")
                .select()
                .decodeList<BannedWord>()
                .map { it.word.lowercase() }

            // 2. Check for any banned word in the text
            val lowerText = text.lowercase()
            val foundWord = bannedWords.firstOrNull { lowerText.contains(it) }
            if (foundWord != null) {
                return ModerationResult(false, foundWord, "Banned Word")
            }

            // 3. Extract all links and flag suspicious ones (exclude trusted domains)
            val linkRegex = Regex("(?:https?://|www\\.)[^\\s\"'<>]+")
            val matches = linkRegex.findAll(text).map { it.value }.toList()
            val suspicious = matches.filter { link ->
                !link.contains("fly.storage.tigris.dev") && !link.contains("math.now.sh")
            }

            if (suspicious.isNotEmpty()) {
                return ModerationResult(false, suspicious.first(), "Suspicious Link")
            }

            // 4. All clear
            ModerationResult(true, null, null)

        } catch (e: Exception) {
            // On error, better to block than to accidentally allow something
            ModerationResult(false, "Moderation check failed", "Error")
        }
    }
}