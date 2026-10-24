package org.bhargav.pansariwala.server.security

import java.util.concurrent.ConcurrentHashMap

/** Too many failed logins for an account or IP; [retryAfterSec] tells the client when to try again. */
class LoginLockedException(val retryAfterSec: Int) :
    RuntimeException("Too many failed login attempts. Try again later")

/**
 * In-memory brute-force guard for password logins (POS and master console).
 * An account locks after [maxAccountFailures] misses in [windowMs]; an IP after [maxIpFailures].
 */
class LoginGuard(
    private val maxAccountFailures: Int = 5,
    private val maxIpFailures: Int = 20,
    private val windowMs: Long = 15 * 60_000L,
    private val lockMs: Long = 15 * 60_000L,
) {
    private class Bucket {
        val failures = ArrayDeque<Long>()
        var lockedUntil = 0L
    }

    private val buckets = ConcurrentHashMap<String, Bucket>()

    /** Throws [LoginLockedException] when either the account or the IP is locked. */
    fun check(scope: String, username: String, ip: String?) {
        val now = System.currentTimeMillis()
        keys(scope, username, ip).forEach { key ->
            val bucket = buckets[key] ?: return@forEach
            synchronized(bucket) {
                if (bucket.lockedUntil > now) throw LoginLockedException(secs(bucket.lockedUntil - now))
            }
        }
    }

    fun onFailure(scope: String, username: String, ip: String?) {
        val now = System.currentTimeMillis()
        keys(scope, username, ip).forEach { key ->
            val limit = if (key.startsWith(IP_PREFIX)) maxIpFailures else maxAccountFailures
            val bucket = buckets.computeIfAbsent(key) { Bucket() }
            synchronized(bucket) {
                while (bucket.failures.isNotEmpty() && now - bucket.failures.first() > windowMs) bucket.failures.removeFirst()
                bucket.failures.addLast(now)
                if (bucket.failures.size >= limit) {
                    bucket.lockedUntil = now + lockMs
                    bucket.failures.clear()
                }
            }
        }
        if (buckets.size > MAX_TRACKED) {
            buckets.entries.removeIf { (_, b) -> synchronized(b) { b.lockedUntil < now && b.failures.isEmpty() } }
        }
    }

    fun onSuccess(scope: String, username: String) {
        buckets.remove(accountKey(scope, username))
    }

    private fun keys(scope: String, username: String, ip: String?): List<String> =
        listOfNotNull(accountKey(scope, username), ip?.trim()?.takeIf { it.isNotEmpty() }?.let { "$IP_PREFIX$scope:$it" })

    private fun accountKey(scope: String, username: String) = "acct:$scope:${username.trim().lowercase()}"

    private fun secs(ms: Long) = ((ms + 999) / 1_000).toInt().coerceAtLeast(1)

    private companion object {
        const val IP_PREFIX = "ip:"
        const val MAX_TRACKED = 20_000
    }
}
