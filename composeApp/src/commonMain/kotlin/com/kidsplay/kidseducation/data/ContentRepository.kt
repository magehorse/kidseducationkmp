package com.kidsplay.kidseducation.data

import com.kidsplay.kidseducation.resources.Res
import com.kidsplay.kidseducation.shared.LearningCategory
import com.kidsplay.kidseducation.shared.LearningContent
import com.kidsplay.kidseducation.shared.LifeSkillLevel
import com.kidsplay.kidseducation.shared.LifeSkillsData
import com.kidsplay.kidseducation.shared.SharedJson
import com.kidsplay.kidseducation.shared.VideoCategory
import com.kidsplay.kidseducation.shared.VideoContent
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Offline-first content: everything ships inside the app as compose resources
 * (files/learning.json, files/videos.json, files/life_skills_questions.json).
 * The Ktor server serves the very same files, so an online refresh can be added later.
 */
class ContentRepository {
    private val lock = Mutex()
    private var learning: LearningContent? = null
    private var videos: VideoContent? = null
    private var lifeSkills: LifeSkillsData? = null

    private suspend fun readText(path: String): String = Res.readBytes(path).decodeToString()

    suspend fun learningCategories(): List<LearningCategory> = lock.withLock {
        learning ?: SharedJson.decodeFromString<LearningContent>(readText("files/learning.json")).also { learning = it }
    }.categories

    suspend fun learningCategory(index: Int): LearningCategory? =
        learningCategories().firstOrNull { it.index == index }

    suspend fun videoCategories(): List<VideoCategory> = lock.withLock {
        videos ?: SharedJson.decodeFromString<VideoContent>(readText("files/videos.json")).also { videos = it }
    }.categories

    suspend fun lifeSkillLevels(): List<LifeSkillLevel> = lock.withLock {
        lifeSkills ?: SharedJson.decodeFromString<LifeSkillsData>(readText("files/life_skills_questions.json"))
            .also { lifeSkills = it }
    }.levels.sortedBy { it.unlockOrder }
}
