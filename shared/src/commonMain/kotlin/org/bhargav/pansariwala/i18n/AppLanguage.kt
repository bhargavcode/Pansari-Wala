package org.bhargav.pansariwala.i18n

/**
 * Supported UI languages. Default English lives in `composeResources/values/`.
 *
 * To add a language:
 * 1. Add an entry here
 * 2. Copy `values/strings.xml` → `values-<code>/strings.xml` and translate
 * System locale selects the folder automatically (Compose Multiplatform resources).
 */
enum class AppLanguage(val code: String, val displayLabel: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी"),
    KANNADA("kn", "ಕನ್ನಡ"),
    PUNJABI("pa", "ਪੰਜਾਬੀ"),
    MARATHI("mr", "मराठी"),
    BENGALI("bn", "বাংলা"),
    ;

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}
