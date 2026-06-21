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
 * bottom-bar, НЕ полный лифт стейта). Держит ТОЛЬКО активную секцию (selectedSection: RepoTab).
 * И top-bar, и будущий glass bottom-bar читают/пишут одно это состояние.
 *
 * rememberSaveable-поведение исходного `var selectedTab` сохранено через listSaver:
 * переживает поворот/смерть процесса; ключ (repo.fullName) сбрасывает секцию при смене репо.
 */
@Stable
internal class RepoNavState(initialSection: RepoTab) {
    var selectedSection by mutableStateOf(initialSection)
}

@Composable
internal fun rememberRepoNavState(key: Any): RepoNavState =
    rememberSaveable(key, saver = listSaver(
        save = { listOf(it.selectedSection.name) },
        restore = { RepoNavState(RepoTab.valueOf(it.first())) },
    )) { RepoNavState(RepoTab.FILES) }
