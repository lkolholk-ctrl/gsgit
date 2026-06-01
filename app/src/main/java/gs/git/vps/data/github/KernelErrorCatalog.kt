package gs.git.vps.data.github

import android.content.Context

/**
 * Placeholder kernel error catalog for GitHub Actions log diagnosis.
 */
data class KernelErrorCatalog(
    val version: String = "1.0",
    val source: String = "placeholder",
    val patterns: List<KernelErrorPattern> = emptyList()
)

data class KernelErrorPattern(
    val id: String,
    val regex: String,
    val message: String,
    val severity: String = "warning"
)

object KernelErrorPatterns {
    fun load(context: Context): KernelErrorCatalog {
        return KernelErrorCatalog()
    }

    fun diagnose(context: Context, catalog: KernelErrorCatalog?, log: String): List<String> {
        if (catalog == null) return emptyList()
        return emptyList()
    }
}
