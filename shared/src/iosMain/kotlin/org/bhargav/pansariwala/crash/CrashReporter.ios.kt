package org.bhargav.pansariwala.crash

private class IosCrashReporter : CrashReporter {
    private val customKeys = linkedMapOf<String, String>()

    override fun setCustomKeys(params: Map<String, String>) {
        customKeys.putAll(params)
    }

    override fun recordException(throwable: Throwable, params: Map<String, String>) {
        if (params.isNotEmpty()) setCustomKeys(params)
        println("CrashReporter recordException keys=$customKeys error=${throwable.message}")
    }

    override fun log(message: String) {
        println("CrashReporter: $message")
    }
}

actual fun createCrashReporter(): CrashReporter = IosCrashReporter()
