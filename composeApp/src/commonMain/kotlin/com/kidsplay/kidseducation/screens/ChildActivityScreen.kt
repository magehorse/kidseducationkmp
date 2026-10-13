package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.data.ActivityEntry
import com.kidsplay.kidseducation.data.ActivityKind
import com.kidsplay.kidseducation.data.ActivityLog
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

private const val MAX_DAYS = 14
private const val SESSION_GAP_MS = 5 * 60 * 1000L

private class DayReport(val date: LocalDate, val entries: List<ActivityEntry>) {
    /** Rough active time: events less than 5 minutes apart belong to one sitting (+1 min for the last one). */
    val minutes: Int
        get() {
            val times = entries.map { it.time }.sorted()
            if (times.isEmpty()) return 0
            var total = 0L
            var start = times.first()
            var prev = start
            for (t in times.drop(1)) {
                if (t - prev > SESSION_GAP_MS) { total += prev - start + 60_000; start = t }
                prev = t
            }
            total += prev - start + 60_000
            return (total / 60_000).toInt()
        }
}

private fun dayLabel(date: LocalDate): String = when (date) {
    ActivityLog.today() -> "Today"
    ActivityLog.yesterday() -> "Yesterday"
    else -> "${date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}, " +
        "${date.dayOfMonth} ${date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }}"
}

/** Consecutive days with activity, counting back from today (or yesterday if today is still empty). */
private fun streak(days: Set<LocalDate>): Int {
    var day = if (ActivityLog.today() in days) ActivityLog.today() else ActivityLog.yesterday()
    var n = 0
    while (day in days) { n++; day = day.minus(1, DateTimeUnit.DAY) }
    return n
}

private fun kindColor(kind: ActivityKind): Color = when (kind) {
    ActivityKind.Learn -> Color(0xFF3A6FD8)
    ActivityKind.Game -> Color(0xFFFF9800)
    ActivityKind.Video -> Color(0xFFEC407A)
    ActivityKind.Reels -> Color(0xFF7E57C2)
    ActivityKind.Draw -> Color(0xFF4CAF50)
    ActivityKind.LifeSkills -> Color(0xFFFFC107)
}

/** "Alphabets: A for Apple, B for Ball (+3 more)" — one line per group. */
private fun groupedLines(list: List<ActivityEntry>): List<String> =
    list.groupBy { it.title }.map { (title, items) ->
        val details = items.map { it.detail }.filter { it.isNotBlank() }.distinct()
        val shown = details.take(8).joinToString(", ")
        val more = if (details.size > 8) " (+${details.size - 8} more)" else ""
        when {
            details.isEmpty() -> title
            title.isBlank() -> shown + more
            else -> "$title: $shown$more"
        }
    }

/** "Match Pattern – 5 pairs ×2" — counts repeats. */
private fun countedLines(list: List<ActivityEntry>): List<String> =
    list.groupBy { it.title to it.detail }.map { (key, items) ->
        val name = if (key.second.isBlank()) key.first else "${key.first} – ${key.second}"
        if (items.size > 1) "$name ×${items.size}" else name
    }

private fun linesFor(kind: ActivityKind, list: List<ActivityEntry>): List<String> = when (kind) {
    ActivityKind.Learn, ActivityKind.Video -> groupedLines(list)
    ActivityKind.Reels -> listOf("Watched ${list.size} reel${if (list.size == 1) "" else "s"}")
    ActivityKind.Game, ActivityKind.Draw, ActivityKind.LifeSkills -> countedLines(list)
}

/**
 * Parent report: what the child learned, played and watched every day. Reached from Settings -> Child Activity.
 * Everything is stored on this device only.
 */
@Composable
fun ChildActivityScreen(onBack: () -> Unit) {
    val app = LocalAppState.current
    var entries by remember { mutableStateOf(app.activity.all()) }
    var confirmClear by remember { mutableStateOf(false) }

    val byDay = remember(entries) {
        entries.groupBy { ActivityLog.dayOf(it.time) }.entries
            .sortedByDescending { it.key }
            .take(MAX_DAYS).map { DayReport(it.key, it.value) }
    }
    val today = byDay.firstOrNull { it.date == ActivityLog.today() }
    val name = app.userName.ifBlank { "your child" }

    ScreenFrame("Child Activity", onBack) {
        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TileCard(Modifier.fillMaxWidth(), borderColor = KidsColors.Sun) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("👨‍👩‍👧 What did $name learn?", style = MaterialTheme.typography.titleMedium, fontSize = 16.sp)
                        Text(
                            "A simple diary of lessons, games and videos. It stays on this phone only.",
                            fontSize = 12.sp, color = Color.Gray,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val learnedToday = today?.entries?.filter { it.kind == ActivityKind.Learn }
                                ?.map { it.title + it.detail }?.distinct()?.size ?: 0
                            val gamesToday = today?.entries?.count { it.kind == ActivityKind.Game } ?: 0
                            val videosToday = today?.entries?.filter { it.kind == ActivityKind.Video }
                                ?.map { it.detail }?.distinct()?.size ?: 0
                            StatChip("📚", "$learnedToday", "Learned", Modifier.weight(1f))
                            StatChip("🎮", "$gamesToday", "Games", Modifier.weight(1f))
                            StatChip("🎬", "$videosToday", "Videos", Modifier.weight(1f))
                            StatChip("⏱", "${today?.minutes ?: 0}m", "Today", Modifier.weight(1f))
                        }
                        val s = streak(byDay.map { it.date }.toSet())
                        if (s > 0) Text("🔥 $s day${if (s == 1) "" else "s"} in a row", fontSize = 13.sp, color = KidsColors.HeaderBottom)
                    }
                }
            }

            if (byDay.isEmpty()) {
                item {
                    Text(
                        "Nothing yet.\nWhen your child learns or plays, it will show up here day by day.",
                        modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center, color = Color.Gray,
                    )
                }
            }

            items(byDay) { day ->
                TileCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                dayLabel(day.date), Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, color = KidsColors.HeaderBottom,
                            )
                            Text("≈ ${day.minutes} min", fontSize = 12.sp, color = Color.Gray)
                        }
                        ActivityKind.entries.forEach { kind ->
                            val ofKind = day.entries.filter { it.kind == kind }
                            if (ofKind.isNotEmpty()) KindSection(kind, linesFor(kind, ofKind))
                        }
                    }
                }
            }

            if (entries.isNotEmpty()) {
                item {
                    BigButton("Clear history", { confirmClear = true }, Modifier.fillMaxWidth(), color = KidsColors.Sun)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("This removes the activity diary from this device.") },
            confirmButton = {
                TextButton(onClick = { app.activity.clear(); entries = emptyList(); confirmClear = false }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatChip(emoji: String, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(KidsColors.PageTop).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$emoji $value", fontSize = 15.sp, style = MaterialTheme.typography.titleMedium, color = KidsColors.Ink)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun KindSection(kind: ActivityKind, lines: List<String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(kindColor(kind).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Text(kind.emoji, fontSize = 17.sp) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(kind.heading, fontSize = 13.sp, style = MaterialTheme.typography.labelLarge, color = kindColor(kind))
            lines.forEach { Text(it, fontSize = 13.sp, color = KidsColors.Ink) }
        }
    }
}
