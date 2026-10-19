package org.bhargav.pansariwala.voice

/**
 * Rough Devanagari → Latin transliteration so Hindi STT output ("शुगर") can match English product names
 * ("Sugar") and Latin speech can match Hindi-only names. Non-Devanagari characters pass through.
 */
internal object Transliterator {

    private const val HALANT = '\u094D'
    private const val NUKTA = '\u093C'

    private val consonants = mapOf(
        'क' to "k", 'ख' to "kh", 'ग' to "g", 'घ' to "gh", 'ङ' to "n",
        'च' to "ch", 'छ' to "chh", 'ज' to "j", 'झ' to "jh", 'ञ' to "n",
        'ट' to "t", 'ठ' to "th", 'ड' to "d", 'ढ' to "dh", 'ण' to "n",
        'त' to "t", 'थ' to "th", 'द' to "d", 'ध' to "dh", 'न' to "n",
        'प' to "p", 'फ' to "f", 'ब' to "b", 'भ' to "bh", 'म' to "m",
        'य' to "y", 'र' to "r", 'ल' to "l", 'व' to "v", 'श' to "sh",
        'ष' to "sh", 'स' to "s", 'ह' to "h",
    )

    /** Consonant + nukta (ज़ → z). */
    private val nuktaForms = mapOf('ज' to "z", 'फ' to "f", 'ड' to "r", 'ढ' to "rh", 'क' to "q", 'ख' to "kh", 'ग' to "g")

    private val vowels = mapOf(
        'अ' to "a", 'आ' to "aa", 'इ' to "i", 'ई' to "ee", 'उ' to "u", 'ऊ' to "oo",
        'ए' to "e", 'ऐ' to "ai", 'ओ' to "o", 'औ' to "au", 'ऋ' to "ri", 'ऑ' to "o",
    )

    private val matras = mapOf(
        'ा' to "aa", 'ि' to "i", 'ी' to "ee", 'ु' to "u", 'ू' to "oo", 'े' to "e",
        'ै' to "ai", 'ो' to "o", 'ौ' to "au", 'ृ' to "ri", 'ॅ' to "e", 'ॉ' to "o",
    )

    private val devanagariDigits = "०१२३४५६७८९"

    fun hasDevanagari(value: String): Boolean = value.any { it in '\u0900'..'\u097F' }

    fun toLatin(value: String): String {
        if (!hasDevanagari(value)) return value
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i]
            val next = value.getOrNull(i + 1)
            val consonant = consonants[c]
            when {
                consonant != null -> {
                    val nukta = next == NUKTA
                    out.append(if (nukta) nuktaForms[c] ?: consonant else consonant)
                    if (nukta) i++
                    val after = value.getOrNull(i + 1)
                    // Inherent "a" unless a matra/halant follows or the word ends (schwa deletion).
                    if (after != null && after != HALANT && after !in matras && isLetter(after)) out.append('a')
                }
                c in matras -> out.append(matras.getValue(c))
                c in vowels -> out.append(vowels.getValue(c))
                c == 'ं' || c == 'ँ' -> out.append('n')
                c == 'ः' -> out.append('h')
                c == HALANT || c == NUKTA -> Unit
                c in devanagariDigits -> out.append(devanagariDigits.indexOf(c))
                else -> out.append(c)
            }
            i++
        }
        return out.toString()
    }

    /** Folds spelling variants both sides may use: aa/a, ee/i, oo/u, ph/f, w/v. */
    fun fold(value: String): String = value
        .replace("aa", "a")
        .replace("ee", "i")
        .replace("oo", "u")
        .replace("ph", "f")
        .replace('w', 'v')

    private fun isLetter(c: Char): Boolean = c in consonants || c in vowels || c == 'ं' || c == 'ँ'
}
