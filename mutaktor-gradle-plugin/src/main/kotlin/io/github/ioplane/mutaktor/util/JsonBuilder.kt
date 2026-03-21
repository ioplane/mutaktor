package io.github.ioplane.mutaktor.util

/**
 * Lightweight JSON string helpers. No external library needed.
 */
public object JsonBuilder {

    /**
     * Escapes special characters for safe embedding inside a JSON string value.
     *
     * Handles: backslash, double-quote, newline, carriage-return, tab,
     * and control characters (U+0000..U+001F).
     */
    public fun escapeJson(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when (ch) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch.code in 0..0x1F) {
                        sb.append("\\u%04x".format(ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Returns the string wrapped in double-quotes with JSON escaping applied.
     */
    public fun quote(s: String): String = "\"${escapeJson(s)}\""
}
