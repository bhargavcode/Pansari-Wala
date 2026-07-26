package org.bhargav.pansariwala.domain.auth

/**
 * Lightweight deterministic hash for demo/local auth. Not for production security;
 * swap for a real KDF (e.g. Argon2/bcrypt via platform APIs) later.
 */
object Credentials {
    fun hash(password: String): String {
        var h = 1125899906842597L // prime
        for (c in password) {
            h = 31 * h + c.code
        }
        return h.toString()
    }
}
