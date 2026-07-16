package gs.git.vps.ui.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import gs.git.vps.data.github.CodeChange
import gs.git.vps.data.github.CodeWorkspaceAtomicStore
import gs.git.vps.data.github.CodeWorkspaceLoadResult
import gs.git.vps.data.github.CodeWorkspacePersistentState
import gs.git.vps.data.github.CodeWorkspaceSnapshot
import gs.git.vps.data.github.model.GHContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Single owner for the mutable Code workspace state and its ordered atomic persistence. */
@Stable
internal class CodeWorkspaceStateHolder(
    context: Context,
    private val repoFullName: String,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext
    private val writeMutex = Mutex()
    private val revisions = ConcurrentHashMap<String, AtomicLong>()

    val draft = mutableStateMapOf<String, CodeChange>()
    val bases = mutableStateMapOf<String, String>()
    val tabs = mutableStateListOf<GHContent>()
    val backHistory = mutableStateListOf<String>()
    val forwardHistory = mutableStateListOf<String>()

    var recoveredLastLoad by mutableStateOf(false)
        private set
    var persistenceError by mutableStateOf<String?>(null)
        private set

    suspend fun loadBranch(branch: String): CodeWorkspaceLoadResult {
        val result = CodeWorkspaceAtomicStore.load(appContext, repoFullName, branch)
        draft.clear(); draft.putAll(result.state.draft)
        bases.clear(); bases.putAll(result.state.bases)
        tabs.clear()
        tabs.addAll(result.state.workspace.openPaths.map(::contentForPath))
        backHistory.clear(); backHistory.addAll(result.state.workspace.backHistory)
        forwardHistory.clear(); forwardHistory.addAll(result.state.workspace.forwardHistory)
        recoveredLastLoad = result.recovered
        persistenceError = null
        return result
    }

    fun persist(branch: String, activePath: String?) {
        val state = snapshot(activePath)
        val revision = revisions.computeIfAbsent(branch) { AtomicLong() }.incrementAndGet()
        scope.launch {
            writeMutex.withLock {
                if (revisions[branch]?.get() != revision) return@withLock
                runCatching {
                    CodeWorkspaceAtomicStore.save(appContext, repoFullName, branch, state)
                }.onSuccess {
                    persistenceError = null
                }.onFailure {
                    persistenceError = it.message ?: "workspace persistence failed"
                }
            }
        }
    }

    /** Replaces a branch slot after all older queued writes have either finished or become stale. */
    suspend fun replaceBranch(branch: String, state: CodeWorkspacePersistentState) {
        revisions.computeIfAbsent(branch) { AtomicLong() }.incrementAndGet()
        writeMutex.withLock {
            CodeWorkspaceAtomicStore.save(appContext, repoFullName, branch, state)
        }
    }

    fun snapshot(activePath: String?): CodeWorkspacePersistentState = CodeWorkspacePersistentState(
        draft = draft.toMap(),
        bases = bases.toMap(),
        workspace = CodeWorkspaceSnapshot(
            openPaths = tabs.map { it.path },
            activePath = activePath,
            backHistory = backHistory.toList(),
            forwardHistory = forwardHistory.toList(),
        ),
    )

    private fun contentForPath(path: String) = GHContent(
        name = path.substringAfterLast('/'),
        path = path,
        type = "file",
        size = 0L,
        downloadUrl = "",
        sha = "",
    )
}

@Composable
internal fun rememberCodeWorkspaceStateHolder(
    context: Context,
    repoFullName: String,
): CodeWorkspaceStateHolder {
    val scope = rememberCoroutineScope()
    return remember(context.applicationContext, repoFullName) {
        CodeWorkspaceStateHolder(context.applicationContext, repoFullName, scope)
    }
}
