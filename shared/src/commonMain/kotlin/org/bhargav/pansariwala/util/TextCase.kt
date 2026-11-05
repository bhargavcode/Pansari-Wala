package org.bhargav.pansariwala.util

/** First letter of the whole string, for notes and free text. */
fun String.withLeadingCap(): String =
    if (isEmpty() || !first().isLowerCase()) this else replaceFirstChar { it.uppercaseChar() }

/** First letter of every word, for names and brands. */
fun String.withWordCaps(): String = split(' ').joinToString(" ") { word ->
    if (word.isEmpty() || !word.first().isLowerCase()) word else word.replaceFirstChar { it.uppercaseChar() }
}
