package com.raywdude.waterresistance

import android.content.Context

/**
 * Keeps the last few test results on the device. Stored as plain delimited lines so the app
 * needs no serialisation dependency.
 */
class HistoryStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<HistoryEntry> =
        prefs.getString(KEY_ENTRIES, null)
            ?.lineSequence()
            ?.mapNotNull { parse(it) }
            ?.toList()
            ?: emptyList()

    fun add(entry: HistoryEntry) {
        persist((listOf(entry) + load()).take(MAX_ENTRIES))
    }

    fun remove(entry: HistoryEntry) {
        val entries = load().toMutableList()
        if (entries.remove(entry)) {
            persist(entries)
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_ENTRIES).apply()
    }

    private fun persist(entries: List<HistoryEntry>) {
        prefs.edit().putString(KEY_ENTRIES, entries.joinToString("\n") { format(it) }).apply()
    }

    private fun format(entry: HistoryEntry): String =
        listOf(
            entry.epochMillis.toString(),
            entry.peakDelta.toString(),
            entry.passThreshold.toString(),
            if (entry.passed) "1" else "0",
        ).joinToString("|")

    private fun parse(line: String): HistoryEntry? {
        val parts = line.split("|")
        if (parts.size != 4) return null
        return HistoryEntry(
            epochMillis = parts[0].toLongOrNull() ?: return null,
            peakDelta = parts[1].toFloatOrNull() ?: return null,
            passThreshold = parts[2].toFloatOrNull() ?: return null,
            passed = parts[3] == "1",
        )
    }

    companion object {
        const val MAX_ENTRIES = 20
        private const val PREFS_NAME = "seal_test_history"
        private const val KEY_ENTRIES = "entries"
    }
}
