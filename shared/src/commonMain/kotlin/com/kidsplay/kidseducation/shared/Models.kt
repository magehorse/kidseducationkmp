package com.kidsplay.kidseducation.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/** One JSON configuration shared by the app and the server so they never drift apart. */
val SharedJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

// ------------------------------------------------------------------ learning content

@Serializable
data class LearningItem(
    /** Drawable resource name, e.g. "a_apple". */
    val image: String,
    /** What the TTS voice says. */
    val speak: String,
    /** What is written on screen. */
    val show: String,
    /** Hindi spoken text (only the Numbers category has it). */
    val speakHi: String? = null,
)

@Serializable
data class LearningCategory(
    val index: Int,
    val title: String,
    val icon: String,
    val items: List<LearningItem>,
)

@Serializable
data class LearningContent(val version: Int = 1, val categories: List<LearningCategory> = emptyList())

// ------------------------------------------------------------------ videos

@Serializable
data class VideoItem(val videoId: String, val title: String) {
    val thumbnailUrl: String get() = "https://i3.ytimg.com/vi/$videoId/hqdefault.jpg"
    val watchUrl: String get() = "https://www.youtube.com/watch?v=$videoId"
}

@Serializable
data class VideoCategory(val id: Int, val title: String, val videos: List<VideoItem> = emptyList())

@Serializable
data class VideoContent(val version: Int = 1, val categories: List<VideoCategory> = emptyList())

// ------------------------------------------------------------------ reels (legacy API shape)

/**
 * Same wire format as the legacy endpoint
 * `GET {base}/123456/get-video?id=37&type=category`.
 * "language" doubles as the category label, "image" is the .mp4 URL.
 */
@Serializable
data class ReelItem(
    val postId: String = "",
    val id: Int = 0,
    val type: String = "",
    val language: String = "",
    val image: String = "",
    @kotlinx.serialization.SerialName("is_paid") val isPaid: Boolean = false,
    val video: Boolean = true,
)

// ------------------------------------------------------------------ life skills

@Serializable
data class LifeSkillQuestion(
    val id: String,
    val type: String,
    val question: String,
    val emoji: String? = null,
    val options: List<String> = emptyList(),
    /** Int index for choice questions, a String ("Yes") for YES_NO, null for free text. */
    val correctAnswer: JsonElement? = null,
    val hint: String? = null,
    val explanation: String? = null,
    val rewardStars: Int = 1,
) {
    val questionType: LifeSkillQuestionType get() = LifeSkillQuestionType.from(type)

    /** Free-text questions have no wrong answer; everything the child says is accepted. */
    val hasAnswerKey: Boolean get() = correctAnswer != null && correctAnswer !is JsonNull

    fun isCorrect(selectedIndex: Int): Boolean {
        val key = correctAnswer as? JsonPrimitive ?: return true
        key.intOrNull?.let { return it == selectedIndex }
        val text = key.contentOrNull ?: return true
        return options.getOrNull(selectedIndex).equals(text, ignoreCase = true)
    }
}

enum class LifeSkillQuestionType {
    TEXT_INPUT, MULTIPLE_CHOICE, YES_NO, PICTURE_SELECT, SITUATION;

    companion object {
        fun from(raw: String): LifeSkillQuestionType = entries.firstOrNull { it.name == raw } ?: MULTIPLE_CHOICE
    }
}

@Serializable
data class LifeSkillLevel(
    val id: Int,
    val key: String,
    val title: String,
    val emoji: String,
    val unlockOrder: Int,
    val questions: List<LifeSkillQuestion>,
    val isCapstone: Boolean = false,
)

@Serializable
data class LifeSkillsData(
    val version: Int = 1,
    val language: String = "en",
    val levels: List<LifeSkillLevel> = emptyList(),
)

// ------------------------------------------------------------------ API contract

object ApiRoutes {
    const val HEALTH = "/health"
    const val LEARNING = "/api/v1/learning"
    const val VIDEOS = "/api/v1/videos"
    const val LIFE_SKILLS = "/api/v1/life-skills"
    /** Legacy-compatible reel feed + ad-config routes (same paths as the old PHP backend). */
    const val REELS = "/123456/get-video"
    const val APP_VERSION = "/kidsplay/appversion.php"
    const val PRIVACY = "/kidsplay/privacy-policy.html"
}
