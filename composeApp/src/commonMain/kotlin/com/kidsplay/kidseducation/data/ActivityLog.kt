package com.kidsplay.kidseducation.data

import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** What kind of thing the child did. Drives the icon / wording in the parent report. */
enum class ActivityKind(val emoji: String, val heading: String) {
    Learn("📚", "Learned"),
    Game("🎮", "Played"),
    Video("🎬", "Watched videos"),
    Reels("📱", "Kids Reels"),
    Draw("🎨", "Drawing"),
    LifeSkills("🌟", "Life skills"),
}

/** One thing the child did. [title] is the group (e.g. "Alphabets"), [detail] the specific item (e.g. "A for Apple"). */
class ActivityEntry(val time: Long, val kind: ActivityKind, val title: String, val detail: String)

/**
 * Diary of what the child learns and plays, shown to parents (Settings -> Child Activity).
 * Stored on the device only (never uploaded); the newest [MAX_ENTRIES] entries are kept.
 */
class ActivityLog(private val settings: Settings) {
    private val entries: MutableList<ActivityEntry> = load()

    fun all(): List<ActivityEntry> = entries.toList()

    fun record(kind: ActivityKind, title: String, detail: String = "") {
        val now = Clock.System.now().toEpochMilliseconds()
        val last = entries.lastOrNull()
        // Ignore accidental repeats (recomposition, language change) within a few seconds.
        if (last != null && last.kind == kind && last.title == title && last.detail == detail && now - last.time < 4_000) return
        entries.add(ActivityEntry(now, kind, clean(title), clean(detail)))
        while (entries.size > MAX_ENTRIES) entries.removeAt(0)
        save()
    }

    fun clear() {
        entries.clear()
        settings.remove(KEY)
    }

    private fun clean(text: String) = text.replace('\t', ' ').replace('\n', ' ').trim()

    private fun save() {
        settings.putString(KEY, entries.joinToString("\n") { "${it.time}\t${it.kind.name}\t${it.title}\t${it.detail}" })
    }

    private fun load(): MutableList<ActivityEntry> {
        val raw = settings.getString(KEY, "")
        if (raw.isBlank()) return mutableListOf()
        return raw.lineSequence().mapNotNull { line ->
            val p = line.split('\t')
            if (p.size < 4) return@mapNotNull null
            val time = p[0].toLongOrNull() ?: return@mapNotNull null
            val kind = ActivityKind.entries.firstOrNull { it.name == p[1] } ?: return@mapNotNull null
            ActivityEntry(time, kind, p[2], p[3])
        }.toMutableList()
    }

    companion object {
        private const val KEY = "child_activity_log"
        private const val MAX_ENTRIES = 800

        fun dayOf(time: Long): LocalDate = Instant.fromEpochMilliseconds(time).toLocalDateTime(TimeZone.currentSystemDefault()).date

        fun today(): LocalDate = dayOf(Clock.System.now().toEpochMilliseconds())

        fun yesterday(): LocalDate = today().minus(1, DateTimeUnit.DAY)
    }
}
