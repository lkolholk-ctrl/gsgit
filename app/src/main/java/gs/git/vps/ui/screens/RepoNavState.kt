package gs.git.vps.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Единый источник правды навигации репо-экрана (RepoModule Phase 2 — ТОЧЕЧНЫЙ срез под редизайн
 * bottom-bar, НЕ полный лифт стейта). Держит ТОЛЬКО активную секцию.
 *
 * `selectedSection` — NULLABLE: `null` = лендинг (файловое дерево, дефолт на входе в репо), который
 * НЕ соответствует ни одному табу верхних чипов или бара → на входе ничего не подсвечено. Контент и
 * загрузка трактуют null как FILES (`selectedSection ?: FILES`), а подсветка читает сырой nullable
 * (`selectedSection == tab` → null не совпадает ни с чем). И верхние чипы, и bottom-bar читают/пишут
 * это одно состояние (двусторонний синхрон).
 *
 * rememberSaveable через listSaver: переживает поворот/смерть процесса; пустая строка = null;
 * ключ (repo.fullName) сбрасывает секцию при смене репо.
 */
@Stable
internal class RepoNavState(initialSection: RepoTab?) {
    var selectedSection by mutableStateOf(initialSection)
}

@Composable
internal fun rememberRepoNavState(key: Any): RepoNavState =
    rememberSaveable(key, saver = listSaver(
        save = { listOf(it.selectedSection?.name ?: "") },
        restore = { RepoNavState(it.first().takeIf { s -> s.isNotEmpty() }?.let { s -> RepoTab.valueOf(s) }) },
    )) { RepoNavState(null) }
