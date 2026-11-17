package com.m3uplayer.app

data class M3uItem(val title: String, val uri: String)

data class ParseResult(val items: List<M3uItem>, val skipped: Int)

/** Holds the current playlist so it doesn't have to pass through an Intent. */
object PlaylistHolder {
    var items: List<M3uItem> = emptyList()
}

object M3uParser {
    private val scheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*://.*")

    fun parse(text: String): ParseResult {
        val items = mutableListOf<M3uItem>()
        var skipped = 0
        var pendingTitle: String? = null

        for (raw in text.lineSequence()) {
            val line = raw.trim().removePrefix("\uFEFF")
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                pendingTitle = line.substringAfter(',', "").trim().ifEmpty { null }
                continue
            }
            if (line.startsWith("#")) continue

            // Playable: web/content/file URLs, or an absolute Android path.
            // Not playable: Windows paths (C:\...) and relative paths.
            if (scheme.matches(line) || line.startsWith("/")) {
                items += M3uItem(pendingTitle ?: nameFrom(line), line)
            } else {
                skipped++
            }
            pendingTitle = null
        }
        return ParseResult(items, skipped)
    }

    private fun nameFrom(line: String): String =
        line.substringAfterLast('/').substringBefore('?').ifEmpty { line }
}
