package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.ui.components.AiModuleGlyph
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/**
 * Стеклянный bottom-bar репо-экрана (Стадия 2 редизайна, data-driven). Скоупится ТОЛЬКО в
 * композицию RepoDetailScreen, не глобально; без Material NavigationBar — свой glass-Row.
 * Высота визуальной части (без системного nav-инсета) — [RepoBottomBarHeight]; экран добавляет
 * `RepoBottomBarHeight + navBarInset` в contentPadding/паддинг контента, чтобы низ не перекрывался.
 *
 * Каждый айтем дёргает onSelect(item); активность — снаружи через activeKey (источник правды —
 * RepoNavState.selectedSection + showRepoSettings, см. Стадию 1).
 */
internal val RepoBottomBarHeight = 56.dp

internal data class RepoBottomBarItem(
    val key: String,
    val label: String,
    val glyph: String,
)

@Composable
internal fun RepoBottomBar(
    items: List<RepoBottomBarItem>,
    activeKey: String,
    onSelect: (RepoBottomBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AiModuleTheme.colors
    Box(modifier.fillMaxWidth()) {
        // Glass-подложка: полупрозрачный surface на всю высоту (включая системный nav-инсет).
        Box(Modifier.matchParentSize().background(palette.surface.copy(alpha = 0.86f)))
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            // Hairline сверху вместо тени-пятна.
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.border.copy(alpha = 0.6f)))
            Row(
                Modifier.fillMaxWidth().height(RepoBottomBarHeight),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val sel = item.key == activeKey
                    val tint = if (sel) palette.accent else palette.textSecondary
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onSelect(item) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        AiModuleGlyph(item.glyph, tint = tint, fontSize = 17.sp)
                        Spacer(Modifier.height(3.dp))
                        gs.git.vps.ui.components.AiModuleText(
                            text = item.label,
                            color = tint,
                            fontFamily = JetBrainsMono,
                            fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 9.sp,
                        )
                    }
                }
            }
        }
    }
}
