package gs.git.vps.security

import android.content.Context
import gs.git.vps.BuildConfig

/**
 * Single process-wide decision for operations that expose account secrets.
 *
 * Native checks are intentionally advisory in debug builds: emulators and rooted
 * test devices are normal there. Release builds block token access by default.
 */
object SecurityGate {
    private const val PREFS = "github_prefs"
    private const val KEY_POLICY = "security_environment_policy"
    private const val CHECK_UNAVAILABLE = -1

    enum class Policy {
        BLOCK_SENSITIVE,
        WARN_ONLY,
        WIPE,
    }

    data class Decision(
        val checkCode: Int,
        val policy: Policy,
    ) {
        val environmentSafe: Boolean get() = checkCode == 0
        val allowsSensitiveData: Boolean get() = environmentSafe || policy == Policy.WARN_ONLY
        val shouldWipe: Boolean get() = !environmentSafe && policy == Policy.WIPE
    }

    @Volatile
    private var cachedCheckCode: Int? = null

    /** Runs native checks at most once per process and never crashes the app. */
    fun initialize(context: Context) {
        decision(context)
    }

    fun decision(context: Context): Decision {
        val code = cachedCheckCode ?: synchronized(this) {
            cachedCheckCode ?: runCatching { NativeSecurity.runSecurityChecks() }
                .getOrElse { CHECK_UNAVAILABLE }
                .also { cachedCheckCode = it }
        }
        return Decision(code, policy(context))
    }

    fun policy(context: Context): Policy {
        val fallback = if (BuildConfig.DEBUG) Policy.WARN_ONLY else Policy.BLOCK_SENSITIVE
        val stored = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_POLICY, null)
        return stored?.let { value -> Policy.entries.firstOrNull { it.name == value } } ?: fallback
    }

    fun setPolicy(context: Context, policy: Policy) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_POLICY, policy.name)
            .apply()
    }

    fun environmentMessage(context: Context): String {
        val current = decision(context)
        if (current.environmentSafe) return ""
        val reason = when (current.checkCode) {
            CHECK_UNAVAILABLE -> "security check unavailable"
            1, 2 -> "instrumentation detected"
            3 -> "root access detected"
            4 -> "Magisk detected"
            5 -> "debugger detected"
            6 -> "emulator detected"
            else -> "unsafe environment detected"
        }
        return if (current.allowsSensitiveData) {
            "Security warning: $reason"
        } else {
            "GitHub token access blocked: $reason"
        }
    }

    fun blockedMessage(context: Context): String = decision(context)
        .takeIf { !it.allowsSensitiveData }
        ?.let { environmentMessage(context) }
        .orEmpty()
}
