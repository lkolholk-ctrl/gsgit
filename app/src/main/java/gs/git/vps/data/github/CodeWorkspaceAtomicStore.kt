package gs.git.vps.data.github

import android.content.Context
import android.util.AtomicFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

internal data class CodeWorkspacePersistentState(
    val draft: Map<String, CodeChange> = emptyMap(),
    val bases: Map<String, String> = emptyMap(),
    val workspace: CodeWorkspaceSnapshot = CodeWorkspaceSnapshot(),
)

internal data class CodeWorkspaceLoadResult(
    val state: CodeWorkspacePersistentState,
    val recovered: Boolean = false,
    val migrated: Boolean = false,
)

/**
 * One atomic snapshot for draft + merge ancestors + tabs/history. The previous verified snapshot
 * is kept in a second AtomicFile. A torn/corrupt primary is restored from that recovery copy.
 */
internal object CodeWorkspaceAtomicStore {
    private const val FORMAT_VERSION = 1
    private const val MAX_BYTES = 10_000_000
    private const val DIR = "code_workspace_state"

    suspend fun load(context: Context, repoFullName: String, branch: String): CodeWorkspaceLoadResult =
        withContext(Dispatchers.IO) {
            val files = files(context, repoFullName, branch)
            val primaryRaw = readRaw(files.primary)
            decode(primaryRaw, repoFullName, branch)?.let { return@withContext CodeWorkspaceLoadResult(it) }

            val recoveryRaw = readRaw(files.recovery)
            decode(recoveryRaw, repoFullName, branch)?.let { recovered ->
                writeRaw(files.primary, recoveryRaw!!)
                return@withContext CodeWorkspaceLoadResult(recovered, recovered = true)
            }

            val migrated = CodeWorkspacePersistentState(
                draft = CodeDraftStore.load(context, repoFullName, branch),
                bases = CodeDraftBaseStore.load(context, repoFullName, branch),
                workspace = CodeWorkspaceStore.load(context, repoFullName, branch),
            )
            saveInternal(files, repoFullName, branch, migrated)
            CodeWorkspaceLoadResult(migrated, migrated = true)
        }

    suspend fun save(
        context: Context,
        repoFullName: String,
        branch: String,
        state: CodeWorkspacePersistentState,
    ) = withContext(Dispatchers.IO) {
        saveInternal(files(context, repoFullName, branch), repoFullName, branch, state)
    }

    private data class StoreFiles(val primary: AtomicFile, val recovery: AtomicFile)

    private fun files(context: Context, repoFullName: String, branch: String): StoreFiles {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val key = sha256("$repoFullName@$branch")
        return StoreFiles(
            primary = AtomicFile(File(dir, "$key.json")),
            recovery = AtomicFile(File(dir, "$key.recovery.json")),
        )
    }

    private fun saveInternal(
        files: StoreFiles,
        repoFullName: String,
        branch: String,
        state: CodeWorkspacePersistentState,
    ) {
        val currentRaw = readRaw(files.primary)
        if (decode(currentRaw, repoFullName, branch) != null) writeRaw(files.recovery, currentRaw!!)

        val payload = JSONObject().apply {
            put("draft", CodeDraftStore.encodeCodeDraft(state.draft))
            put("bases", JSONObject().apply {
                state.bases.toSortedMap().forEach { (path, content) -> put(path, content) }
            })
            put("workspace", CodeWorkspaceStore.encodeWorkspaceSnapshot(state.workspace))
        }
        val envelope = JSONObject().apply {
            put("version", FORMAT_VERSION)
            put("repo", repoFullName)
            put("branch", branch)
            put("savedAt", System.currentTimeMillis())
            put("payload", payload)
            put("sha256", sha256(payload.toString()))
        }.toString()
        require(envelope.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Code workspace snapshot is too large" }
        writeRaw(files.primary, envelope)
        if (decode(readRaw(files.recovery), repoFullName, branch) == null) {
            writeRaw(files.recovery, envelope)
        }
    }

    private fun decode(raw: String?, repoFullName: String, branch: String): CodeWorkspacePersistentState? {
        if (raw == null || raw.toByteArray(Charsets.UTF_8).size > MAX_BYTES) return null
        return runCatching {
            val root = JSONObject(raw)
            require(root.optInt("version") == FORMAT_VERSION)
            require(root.optString("repo") == repoFullName)
            require(root.optString("branch") == branch)
            val payload = root.getJSONObject("payload")
            require(root.optString("sha256") == sha256(payload.toString()))
            val basesObject = payload.optJSONObject("bases") ?: JSONObject()
            val bases = buildMap {
                val keys = basesObject.keys()
                while (keys.hasNext()) {
                    val path = keys.next()
                    put(path, basesObject.optString(path, ""))
                }
            }
            CodeWorkspacePersistentState(
                draft = CodeDraftStore.decodeCodeDraft(payload.getJSONObject("draft")),
                bases = bases,
                workspace = CodeWorkspaceStore.decodeWorkspaceSnapshot(payload.getJSONObject("workspace")),
            )
        }.getOrNull()
    }

    private fun readRaw(file: AtomicFile): String? = runCatching {
        file.openRead().use { input ->
            val bytes = input.readBytes()
            require(bytes.size <= MAX_BYTES)
            bytes.toString(Charsets.UTF_8)
        }
    }.getOrNull()

    private fun writeRaw(file: AtomicFile, raw: String) {
        val output = file.startWrite()
        try {
            output.write(raw.toByteArray(Charsets.UTF_8))
            output.flush()
            file.finishWrite(output)
        } catch (e: Exception) {
            file.failWrite(output)
            throw e
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
