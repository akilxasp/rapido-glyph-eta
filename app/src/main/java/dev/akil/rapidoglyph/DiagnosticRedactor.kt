package dev.akil.rapidoglyph

object DiagnosticRedactor {
    fun payloadSummary(payload: String): String {
        if (payload.isBlank()) return "(none)"
        val lines = payload.lineSequence().count()
        return "(redacted by default; $lines line${if (lines == 1) "" else "s"}, " +
            "${payload.length} characters)"
    }
}
