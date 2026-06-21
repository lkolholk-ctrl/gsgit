package gs.git.vps.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono

/**
 * Bottom-bar репо-экрана в стиле Telegram-on-Android (Стадия 2 редизайна): плавающая закруглённая
 * «таблетка» с отступами от краёв, иконка + жирный лейбл под ней, активный айтем — скруглённая
 * плашка + accent-тинт, опциональный бейдж-счётчик. Скоупится ТОЛЬКО в композицию RepoDetailScreen;
 * без Material NavigationBar — свой Row.
 *
 * Цвет активного — palette.accent (выбор юзера в Appearance), НЕ хардкод. Лейблы — JetBrains Mono
 * (терминальный стиль приложения). Источник правды активности — снаружи через activeKey
 * (RepoNavState.selectedSection / showRepoSettings, см. Стадию 1).
 *
 * Экран резервирует снизу [RepoBottomBarReservedHeight] + системный nav-инсет, чтобы низ контента
 * не перекрывался плавающим баром.
 */
internal val RepoBottomBarHeight = 60.dp
private val RepoBottomBarMargin = 12.dp
internal val RepoBottomBarReservedHeight = RepoBottomBarHeight + RepoBottomBarMargin * 2
/** Высота мягкого fade-перехода контента в фон у бара (telegram-style «замыленность», не выше бара). */
internal val RepoBottomBarFadeHeight = 28.dp

internal data class RepoBottomBarItem(
    val key: String,
    val label: String,
    val icon: ImageVector,
    /** Секция, которую айтем занимает в едином selectedSection. null — не секция (напр. settings = отдельный экран). */
    val section: RepoTab? = null,
    val badgeCount: Int = 0,
)

@Composable
internal fun RepoBottomBar(
    items: List<RepoBottomBarItem>,
    activeKey: String,
    onSelect: (RepoBottomBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = AiModuleTheme.colors
    val pillShape = RoundedCornerShape(percent = 50)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = RepoBottomBarMargin, vertical = RepoBottomBarMargin),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(RepoBottomBarHeight)
                .clip(pillShape)
                .background(palette.surface)
                .border(1.dp, palette.border.copy(alpha = 0.45f), pillShape)
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val sel = item.key == activeKey
                val tint = if (sel) palette.accent else palette.textPrimary
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(if (sel) palette.accent.copy(alpha = 0.13f) else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp), tint = tint)
                            if (item.badgeCount > 0) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 11.dp, y = (-7).dp)
                                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                        .clip(CircleShape)
                                        .background(palette.error)
                                        .border(1.5.dp, palette.surface, CircleShape)
                                        .padding(horizontal = 4.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AiModuleText(
                                        text = if (item.badgeCount > 99) "99+" else item.badgeCount.toString(),
                                        color = Color.White,
                                        fontFamily = JetBrainsMono,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 8.sp,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        AiModuleText(
                            text = item.label,
                            color = tint,
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                        )
                    }
                }
            }
        }
    }
}
