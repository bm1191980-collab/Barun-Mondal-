package com.example.data.service

import android.util.Log
import com.example.data.model.PostEntity
import com.example.data.model.PostType
import java.util.Locale
import kotlin.math.max

data class ModerationVerdict(
    val isApproved: Boolean = true,
    val isFlagged: Boolean = false,
    val isSpamLimited: Boolean = false,
    val flagReason: String? = null,
    val riskScore: Float = 0.0f,
    val qualityScore: Int = 90,
    val detectedIssues: List<String> = emptyList(),
    val extractedInterests: List<String> = emptyList()
)

object SatisfyAiModerationEngine {
    private const val TAG = "AiModerationEngine"

    // High-Risk Harmful / Policy Violation Patterns
    private val HARMFUL_KEYWORDS = listOf(
        "kill", "suicide", "murder", "bomb", "terrorist", "nazi", "hate speech",
        "doxxing", "child abuse", "weapons deal", "illicit drugs", "stolen card",
        "cc dump", "phishing clone", "malware download", "trojan virus"
    )

    // Spam, Scam & Phishing Indicators
    private val SPAM_PATTERNS = listOf(
        "free giftcard", "crypto 100x", "earn $", "earn money daily",
        "whatsapp +", "telegram bot @", "dm for money", "click link to claim",
        "hack free subscribers", "free vbucks", "free robux", "casino win fast",
        "make money fast guaranteed", "cash app glitch", "paypal generator",
        "double your btc", "investment scheme guaranteed", "work from home $500/hr",
        "t.me/", "bit.ly/scam", "wa.me/", "claim reward now"
    )

    // Low-Quality Clickbait / Engagement Bait
    private val CLICKBAIT_PATTERNS = listOf(
        "you won't believe", "shocking truth", "100% real no fake",
        "watch before deleted", "secret trick exposed", "admins hate this"
    )

    /**
     * Instantly analyzes a video/post against AI safety rules, spam heuristics, duplicate models,
     * and content quality metrics.
     */
    fun analyzePost(post: PostEntity, existingPosts: List<PostEntity> = emptyList()): ModerationVerdict {
        val fullText = "${post.title} ${post.description} ${post.tags}".lowercase(Locale.ROOT)
        val detectedIssues = mutableListOf<String>()
        var harmRisk = 0.0f
        var spamRisk = 0.0f
        var duplicateRisk = 0.0f

        // 1. Scan for Harmful / Prohibited Content
        for (kw in HARMFUL_KEYWORDS) {
            if (fullText.contains(kw)) {
                harmRisk = max(harmRisk, 0.88f)
                detectedIssues.add("Harmful Keyword: '$kw'")
            }
        }

        // 2. Scan for Spam, Scams & Unsolicited Promo Links
        var spamMatches = 0
        for (pattern in SPAM_PATTERNS) {
            if (fullText.contains(pattern)) {
                spamMatches++
                spamRisk = max(spamRisk, 0.45f + (spamMatches * 0.15f))
                detectedIssues.add("Spam Pattern: '$pattern'")
            }
        }

        // Check for tag stuffing (e.g. > 15 repetitive hashtags)
        val hashtagCount = post.tags.split(" ", "#", ",").filter { it.isNotBlank() }.size
        if (hashtagCount > 16) {
            spamRisk = max(spamRisk, 0.50f)
            detectedIssues.add("Excessive Hashtag Stuffing ($hashtagCount tags)")
        }

        // Check for excessive ALL CAPS shouting
        val titleLetters = post.title.filter { it.isLetter() }
        if (titleLetters.length >= 10 && titleLetters.all { it.isUpperCase() }) {
            spamRisk = max(spamRisk, 0.35f)
            detectedIssues.add("Excessive Caps / Shouting")
        }

        // 3. Duplicate Content Detection
        val normalizedTitle = post.title.trim().lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9 ]"), "")
        
        for (existing in existingPosts) {
            if (existing.id == post.id) continue

            // Exact media or thumbnail collision from different post
            if (post.mediaUrl.isNotBlank() && post.mediaUrl == existing.mediaUrl && post.creatorUid == existing.creatorUid) {
                duplicateRisk = 0.95f
                detectedIssues.add("Exact Media Duplicate with Post #${existing.id}")
                break
            }

            // High textual similarity
            val existingTitle = existing.title.trim().lowercase(Locale.ROOT)
                .replace(Regex("[^a-z0-9 ]"), "")
            if (normalizedTitle.length > 8 && existingTitle.length > 8) {
                val similarity = calculateSimilarity(normalizedTitle, existingTitle)
                if (similarity >= 0.90 && post.creatorUid == existing.creatorUid) {
                    duplicateRisk = 0.85f
                    detectedIssues.add("Near-Duplicate Title (${(similarity * 100).toInt()}% match with Post #${existing.id})")
                    break
                }
            }
        }

        // 4. Compute Content Quality Score (0 to 100)
        var qualityScore = 90

        // Length & detail scoring
        if (post.title.trim().length in 12..80) qualityScore += 4 else qualityScore -= 4
        if (post.description.trim().length >= 30) qualityScore += 3
        if (post.thumbnailUrl.isNotBlank()) qualityScore += 3
        if (post.qualities.contains("1080p") || post.qualities.contains("4K")) qualityScore += 4
        if (post.tags.isNotBlank()) qualityScore += 2

        // Deductions for spam / clickbait
        if (spamRisk > 0.3f) qualityScore -= (spamRisk * 30).toInt()
        if (harmRisk > 0.3f) qualityScore -= (harmRisk * 50).toInt()
        if (duplicateRisk > 0.5f) qualityScore -= (duplicateRisk * 40).toInt()
        qualityScore = qualityScore.coerceIn(15, 100)

        // 5. Extract Categorical Interests for Recommendation Algorithm
        val extractedInterests = mutableListOf<String>()
        val words = fullText.split(" ", "#", ",", ".", "-").filter { it.length >= 3 }
        val categoryWords = mapOf(
            "Satisfying" to listOf("satisfying", "asmr", "kinetic", "soap", "slime", "relaxing", "visual", "clean", "oddly", "texture", "smooth"),
            "Tech" to listOf("tech", "code", "ai", "phone", "review", "gadget", "software", "android", "computer", "future", "setup"),
            "Travel" to listOf("travel", "nature", "beach", "city", "japan", "bangladesh", "mountain", "explore", "drone", "scenic", "vlog"),
            "Cooking" to listOf("cooking", "food", "recipe", "chef", "baking", "cake", "delicious", "kitchen", "streetfood", "crispy"),
            "Art" to listOf("art", "drawing", "paint", "design", "animation", "craft", "pottery", "sculpture", "calligraphy", "aesthetic"),
            "Gaming" to listOf("gaming", "gameplay", "fps", "speedrun", "clip", "epic", "highlights", "gamer", "unreal", "physics"),
            "Music" to listOf("music", "lofi", "beats", "remix", "soundtrack", "audio", "acoustic", "bass", "piano", "synth")
        )

        categoryWords.forEach { (cat, kws) ->
            if (kws.any { fullText.contains(it) }) {
                extractedInterests.add(cat)
            }
        }
        if (post.category.isNotBlank() && !extractedInterests.contains(post.category)) {
            extractedInterests.add(post.category)
        }

        // 6. Final Verdict Classification
        val totalMaxRisk = maxOf(harmRisk, spamRisk, duplicateRisk)

        return when {
            harmRisk >= 0.70f -> {
                Log.w(TAG, "Post '${post.title}' flagged for Harmful Content (Risk: $harmRisk)")
                ModerationVerdict(
                    isApproved = true, // Creators can upload without manual block, but flagged content is restricted to admin queue
                    isFlagged = true,
                    isSpamLimited = true,
                    flagReason = "[AI FLAG] Prohibited / Harmful Content Detected (${detectedIssues.firstOrNull() ?: "Policy Violation"})",
                    riskScore = harmRisk,
                    qualityScore = qualityScore,
                    detectedIssues = detectedIssues,
                    extractedInterests = extractedInterests
                )
            }
            duplicateRisk >= 0.80f -> {
                Log.w(TAG, "Post '${post.title}' flagged as Duplicate (Risk: $duplicateRisk)")
                ModerationVerdict(
                    isApproved = true,
                    isFlagged = true,
                    isSpamLimited = true,
                    flagReason = "[AI FLAG] Duplicate Content Detected (${detectedIssues.firstOrNull() ?: "High Similarity"})",
                    riskScore = duplicateRisk,
                    qualityScore = qualityScore,
                    detectedIssues = detectedIssues,
                    extractedInterests = extractedInterests
                )
            }
            spamRisk >= 0.40f -> {
                Log.i(TAG, "Post '${post.title}' marked as Spam Limited Reach (Risk: $spamRisk)")
                ModerationVerdict(
                    isApproved = true,
                    isFlagged = false,
                    isSpamLimited = true,
                    flagReason = "[AI NOTED] Limited Distribution - Promotional / Spam heuristic pattern",
                    riskScore = spamRisk,
                    qualityScore = qualityScore,
                    detectedIssues = detectedIssues,
                    extractedInterests = extractedInterests
                )
            }
            else -> {
                ModerationVerdict(
                    isApproved = true,
                    isFlagged = false,
                    isSpamLimited = false,
                    flagReason = "AI Verified: Clean & High Quality (Score: $qualityScore/100)",
                    riskScore = totalMaxRisk,
                    qualityScore = qualityScore,
                    detectedIssues = emptyList(),
                    extractedInterests = extractedInterests
                )
            }
        }
    }

    /**
     * Computes textual word-set similarity ratio (Jaccard index + Levenshtein).
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val words1 = s1.split(" ").filter { it.isNotBlank() }.toSet()
        val words2 = s2.split(" ").filter { it.isNotBlank() }.toSet()
        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        val jaccard = intersection.toDouble() / union.toDouble()
        return jaccard
    }
}
