package com.cts.jiraplugin.ai

import java.security.MessageDigest

data class AiAnalysis(
    val severity: String,
    val isDuplicate: Boolean,
    val clusterId: String,
    val rootCause: String
)

object AiService {

    // 🔹 1. Severity prediction (simple heuristic)
    private fun predictSeverity(stacktrace: String): String {
        return when {
            "NullPointerException" in stacktrace -> "Critical"
            "AssertionError" in stacktrace -> "Major"
            else -> "Minor"
        }
    }

    // 🔹 2. Duplicate detection (hash stacktrace)
    private val seenHashes = mutableSetOf<String>()

    private fun detectDuplicate(stacktrace: String): Pair<Boolean, String> {
        val hash = sha1(stacktrace)
        val alreadySeen = seenHashes.contains(hash)
        if (!alreadySeen) seenHashes.add(hash)
        return Pair(alreadySeen, hash.take(8)) // clusterId = first 8 chars
    }

    // 🔹 3. Root cause suggestion (simple heuristic, later AI)
    private fun suggestRootCause(stacktrace: String): String {
        return when {
            "NullPointerException" in stacktrace -> "Possible uninitialized object"
            "Timeout" in stacktrace -> "Likely network or async issue"
            "AssertionError" in stacktrace -> "Test assertion mismatch"
            else -> "Needs further investigation"
        }
    }

    // 🔹 4. Main analysis entrypoint
    fun analyze(stacktrace: String): AiAnalysis {
        val severity = predictSeverity(stacktrace)
        val (isDup, clusterId) = detectDuplicate(stacktrace)
        val rootCause = suggestRootCause(stacktrace)

        return AiAnalysis(severity, isDup, clusterId, rootCause)
    }

    private fun sha1(input: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
