package com.kidsplay.kidseducation.data

import com.kidsplay.kidseducation.shared.LifeSkillLevel
import com.russhwolf.settings.Settings

/**
 * Key/value persistence (SharedPreferences / NSUserDefaults / java.util.prefs / localStorage).
 *
 * The old Android app kept its data in three different SharedPreferences files, so an in-place update
 * only keeps the child's data if each group is read from the same file:
 *  - [user]       name + language      -> the app's default prefs file (`<package>_preferences`)
 *  - [sound]      sound on/off         -> `MyPref`
 *  - [lifeSkills] life-skills progress -> `life_skills_progress`
 * Other platforms have no legacy data, so all three default to the same [Settings].
 */
class ProgressStore(
    private val user: Settings,
    private val sound: Settings = user,
    private val lifeSkills: Settings = user,
) {

    // ---- user settings (same keys the old Android app used) ----
    var userName: String
        get() = user.getString("USER_NAME", "")
        set(value) = user.putString("USER_NAME", value)

    var soundEnabled: Boolean
        get() = sound.getBoolean("Sound", true)
        set(value) = sound.putBoolean("Sound", value)

    /** "en" or "hi". */
    var language: String
        get() = user.getString("LANG", "en")
        set(value) = user.putString("LANG", value)

    // ---- life skills progress ----
    val totalStars: Int get() = lifeSkills.getInt(KEY_STARS_TOTAL, 0)

    /** Level 1 is always open; a later level opens once the previous one is completed. */
    fun isLevelUnlocked(level: LifeSkillLevel, all: List<LifeSkillLevel>): Boolean {
        if (level.unlockOrder <= 1) return true
        val previous = all.firstOrNull { it.unlockOrder == level.unlockOrder - 1 } ?: return true
        return isLevelCompleted(previous)
    }

    fun isLevelCompleted(level: LifeSkillLevel): Boolean = lifeSkills.getBoolean("completed_${level.key}", false)

    fun markLevelCompleted(level: LifeSkillLevel) = lifeSkills.putBoolean("completed_${level.key}", true)

    fun starsForLevel(level: LifeSkillLevel): Int = lifeSkills.getInt("stars_${level.key}", 0)

    fun correctCount(level: LifeSkillLevel): Int = lifeSkills.getInt("correct_${level.key}", 0)

    fun recordAnswer(level: LifeSkillLevel, correct: Boolean, starsEarned: Int) {
        lifeSkills.putInt(KEY_STARS_TOTAL, totalStars + starsEarned)
        lifeSkills.putInt("stars_${level.key}", starsForLevel(level) + starsEarned)
        val key = if (correct) "correct_${level.key}" else "incorrect_${level.key}"
        lifeSkills.putInt(key, lifeSkills.getInt(key, 0) + 1)
    }

    fun overallCompletionPercent(levels: List<LifeSkillLevel>): Int =
        if (levels.isEmpty()) 0 else levels.count { isLevelCompleted(it) } * 100 / levels.size

    private companion object {
        const val KEY_STARS_TOTAL = "stars_total"
    }
}
