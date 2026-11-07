package org.bhargav.pansariwala.i18n

/**
 * Supported UI languages. Default English lives in `composeResources/values/`.
 *
 * To add a language:
 * 1. Add an entry here
 * 2. Copy `values/strings.xml` → `values-<code>/strings.xml` and translate
 * System locale selects the folder automatically (Compose Multiplatform resources).
 */
enum class AppLanguage(val code: String, val displayLabel: String, val englishName: String) {
    ENGLISH("en", "English", "English"),
    HINDI("hi", "हिन्दी", "Hindi"),
    KANNADA("kn", "ಕನ್ನಡ", "Kannada"),
    PUNJABI("pa", "ਪੰਜਾਬੀ", "Punjabi"),
    MARATHI("mr", "मराठी", "Marathi"),
    BENGALI("bn", "বাংলা", "Bengali"),
    ;

    /** Indian speech locale (`hi-IN`, `en-IN`, …) for on-device recognition. */
    val speechLocale: String get() = "$code-IN"

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

/** Latest app language, so speech can follow it without a suspend call on the audio thread. */
object AppSpeechLanguage {
    var code: String = AppLanguage.ENGLISH.code

    fun current(): AppLanguage = AppLanguage.fromCode(code)
}
