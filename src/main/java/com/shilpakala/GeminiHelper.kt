package com.shilpakala

import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * GeminiHelper — wraps the Gemini API call for product description generation.
 *
 * HOW TO ADD YOUR API KEY
 * ───────────────────────
 * 1. Open your project's  local.properties  file
 *    (it is in the ROOT of the project, same folder as settings.gradle)
 * 2. Add this line at the bottom:
 *
 *       GEMINI_API_KEY=AIzaSy...paste_your_full_key_here
 *
 * 3. Save the file, then do  File → Sync Project with Gradle Files  in Android Studio.
 * 4. That's it — the key is read by build.gradle and injected into BuildConfig.GEMINI_API_KEY.
 *
 * Get a free key at: https://aistudio.google.com/app/apikey
 */
object GeminiHelper {

    // The model is created lazily the first time generateDescription() is called.
    // Using gemini-1.5-flash: fast, free tier available, good quality.
    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey    = BuildConfig.GEMINI_API_KEY
        )
    }

    /**
     * Generates a 2-line premium product description for an artisan product.
     *
     * Runs on [Dispatchers.IO] — safe to call from a coroutine launched on Main.
     *
     * @param productName  e.g. "Channapatna Spinning Top"
     * @param woodType     e.g. "Rosewood"
     * @param artisanName  e.g. "Raju Chitragara"
     * @param price        e.g. "850"
     *
     * @return [Result.success] with the 2-line string, or [Result.failure] with the exception.
     */
    suspend fun generateDescription(
        productName: String,
        woodType: String,
        artisanName: String,
        price: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // Guard: don't even try if the key wasn't set
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Gemini API key is missing.\n" +
                    "Open local.properties and add:\n" +
                    "GEMINI_API_KEY=your_key_here"
                )
            )
        }

        try {
            val prompt = buildPrompt(productName, woodType, artisanName, price)
            val response = model.generateContent(prompt)
            val text = response.text?.trim()
            if (text.isNullOrBlank()) {
                Result.failure(Exception("Empty response from AI. Please try again."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildPrompt(
        productName: String,
        woodType: String,
        artisanName: String,
        price: String
    ): String = """
        You are a luxury product copywriter specialising in Indian handicrafts.
        Write a 2-line premium product description for the item below.

        Product: $productName
        Material: $woodType
        Artisan: $artisanName
        Price: ₹$price

        Rules:
        - Exactly 2 lines, separated by a newline
        - Evoke heritage, craftsmanship, and pride of Karnataka
        - Suitable for a WhatsApp caption or Instagram post
        - No hashtags, no emojis, no bullet points, no markdown
        - Total length: under 40 words

        Reply with ONLY the 2-line description and nothing else.
    """.trimIndent()
}
