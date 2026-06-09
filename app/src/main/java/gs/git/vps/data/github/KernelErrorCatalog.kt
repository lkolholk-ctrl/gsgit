package gs.git.vps.data.github

import android.content.Context
import java.util.regex.Pattern

/**
 * Kernel error catalog for GitHub Actions log diagnosis.
 */
data class KernelErrorCatalog(
    val version: String = "2.0",
    val source: String = "local_database",
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
        val patterns = listOf(
            KernelErrorPattern(
                id = "gcc_clang_error",
                regex = """(?m)^([^:\n]+):([0-9]+):([0-9]+):\s*(?:fatal\s*)?error:\s*(.*)$""",
                message = "C/C++ compilation error in %s at line %s: %s",
                severity = "error"
            ),
            KernelErrorPattern(
                id = "gcc_clang_warning",
                regex = """(?m)^([^:\n]+):([0-9]+):([0-9]+):\s*warning:\s*(.*)$""",
                message = "C/C++ warning in %s at line %s: %s",
                severity = "warning"
            ),
            KernelErrorPattern(
                id = "gradle_dependency_error",
                regex = """(?i)(Could not resolve all files|Could not find|Could not download|Dependency resolution failed|UnknownRepositoryException)""",
                message = "Gradle dependency resolution failure: %s",
                severity = "error"
            ),
            KernelErrorPattern(
                id = "gradle_compile_error",
                regex = """(?m)^e:\s*([^\n:]+):\s*\(([0-9]+),\s*([0-9]+)\):\s*(.*)$""",
                message = "Kotlin compiler error in %s at line %s: %s",
                severity = "error"
            ),
            KernelErrorPattern(
                id = "npm_build_error",
                regex = """(?i)(npm ERR!|ELIFECYCLE|sh: 1:.*|Cannot find module\s+'([^']+)'|Failed to compile)""",
                message = "npm/Node.js build failure: %s",
                severity = "error"
            ),
            KernelErrorPattern(
                id = "maven_compile_error",
                regex = """(?m)^\[ERROR\]\s*([^\n:]+):\[([0-9]+),([0-9]+)\]\s*(.*)$""",
                message = "Maven compiler error in %s at line %s: %s",
                severity = "error"
            ),
            KernelErrorPattern(
                id = "python_traceback",
                regex = """(?m)File\s*"([^"]+)",\s*line\s*([0-9]+),\s*in\s*(.*)$""",
                message = "Python exception in %s at line %s (in %s)",
                severity = "error"
            ),
            KernelErrorPattern(
                id = "java_stacktrace",
                regex = """(?m)at\s+([a-zA-Z0-9_.]+\.[a-zA-Z0-9_$]+)\(([a-zA-Z0-9_]+)\.java:([0-9]+)\)""",
                message = "Java Exception traceback at %s (%s.java:%s)",
                severity = "error"
            )
        )
        return KernelErrorCatalog(patterns = patterns)
    }

    fun diagnose(context: Context, catalog: KernelErrorCatalog?, log: String): List<String> {
        if (catalog == null || log.isBlank()) return emptyList()
        val results = mutableListOf<String>()

        for (pattern in catalog.patterns) {
            try {
                val regex = Pattern.compile(pattern.regex)
                val matcher = regex.matcher(log)
                var count = 0
                while (matcher.find() && count < 8) {
                    val msg = when (pattern.id) {
                        "gcc_clang_error", "gcc_clang_warning" -> {
                            val file = matcher.group(1)?.substringAfterLast("/") ?: ""
                            val line = matcher.group(2) ?: ""
                            val errMsg = matcher.group(4) ?: ""
                            pattern.message.format(file, line, errMsg)
                        }
                        "gradle_compile_error" -> {
                            val file = matcher.group(1)?.substringAfterLast("/") ?: ""
                            val line = matcher.group(2) ?: ""
                            val errMsg = matcher.group(4) ?: ""
                            pattern.message.format(file, line, errMsg)
                        }
                        "maven_compile_error" -> {
                            val file = matcher.group(1)?.substringAfterLast("/") ?: ""
                            val line = matcher.group(2) ?: ""
                            val errMsg = matcher.group(4) ?: ""
                            pattern.message.format(file, line, errMsg)
                        }
                        "python_traceback" -> {
                            val file = matcher.group(1)?.substringAfterLast("/") ?: ""
                            val line = matcher.group(2) ?: ""
                            val func = matcher.group(3) ?: ""
                            pattern.message.format(file, line, func)
                        }
                        "java_stacktrace" -> {
                            val method = matcher.group(1) ?: ""
                            val file = matcher.group(2) ?: ""
                            val line = matcher.group(3) ?: ""
                            pattern.message.format(method, file, line)
                        }
                        else -> {
                            val match = matcher.group(0) ?: ""
                            pattern.message.format(match.trim().take(120))
                        }
                    }
                    if (msg !in results) {
                        results.add(msg)
                        count++
                    }
                }
            } catch (e: Exception) {
                // Ignore invalid patterns
            }
        }
        return results
    }
}

