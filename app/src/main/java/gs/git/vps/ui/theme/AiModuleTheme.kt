package gs.git.vps.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import gs.git.vps.ui.components.CodeColors

/**
 * Shared terminal-style palette for the wider AI module: Hub, Chat,
 * Models, Keys, Usage, Settings, Image generation and Video generation.
 *
 * Do not depend on AgentTerminalTheme here: Agent/Coding have their own
 * namespace, while this theme intentionally duplicates the same visual
 * values so the whole AI area looks unified without cross-package leaks.
 */
@Immutable
data class AiModuleColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val border: Color,
    val accent: Color,
    val accentDim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val warning: Color,
    val error: Color,
    val syntaxKeyword: Color,
    val syntaxFlag: Color,
    val syntaxString: Color,
    val syntaxArg: Color,
    val syntaxComment: Color,
    val syntaxNumber: Color,
)

val AiModuleDarkColors = AiModuleColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceElevated = Color(0xFF141414),
    border = Color(0xFF1F1F1F),
    accent = Color(0xFFA8D982),
    accentDim = Color(0xFF6B8C54),
    textPrimary = Color(0xFFE0E0E0),
    textSecondary = Color(0xFF999999),
    textMuted = Color(0xFF5C5C5C),
    warning = Color(0xFFE5C07B),
    error = Color(0xFFE06C75),
    syntaxKeyword = Color(0xFFA8D982),
    syntaxFlag = Color(0xFFE5C07B),
    syntaxString = Color(0xFF98C379),
    syntaxArg = Color(0xFFABB2BF),
    syntaxComment = Color(0xFF5C6370),
    syntaxNumber = Color(0xFFD19A66),
)

val AiModuleDraculaColors = AiModuleColors(
    background = Color(0xFF282A36),
    surface = Color(0xFF1E1F29),
    surfaceElevated = Color(0xFF343746),
    border = Color(0xFF44475A),
    accent = Color(0xFFBD93F9),
    accentDim = Color(0xFF6272A4),
    textPrimary = Color(0xFFF8F8F2),
    textSecondary = Color(0xFFE2E2DC),
    textMuted = Color(0xFF6272A4),
    warning = Color(0xFFF1FA8C),
    error = Color(0xFFFF5555),
    syntaxKeyword = Color(0xFFFF79C6),
    syntaxFlag = Color(0xFF50FA7B),
    syntaxString = Color(0xFFF1FA8C),
    syntaxArg = Color(0xFF8BE9FD),
    syntaxComment = Color(0xFF6272A4),
    syntaxNumber = Color(0xFFBD93F9),
)

val AiModuleGruvboxColors = AiModuleColors(
    background = Color(0xFF282828),
    surface = Color(0xFF1D2021),
    surfaceElevated = Color(0xFF3C3836),
    border = Color(0xFF504945),
    accent = Color(0xFFFABD2F),
    accentDim = Color(0xFFD79921),
    textPrimary = Color(0xFFEBDBB2),
    textSecondary = Color(0xFFA89984),
    textMuted = Color(0xFF7C6F64),
    warning = Color(0xFFFE8019),
    error = Color(0xFFFB4934),
    syntaxKeyword = Color(0xFFFB4934),
    syntaxFlag = Color(0xFF8EC07C),
    syntaxString = Color(0xFFB8BB26),
    syntaxArg = Color(0xFF83A598),
    syntaxComment = Color(0xFF928374),
    syntaxNumber = Color(0xFFD3869B),
)

val AiModuleOneDarkColors = AiModuleColors(
    background = Color(0xFF282C34),
    surface = Color(0xFF21252B),
    surfaceElevated = Color(0xFF2C313C),
    border = Color(0xFF3E4451),
    accent = Color(0xFF61AFEF),
    accentDim = Color(0xFF4B5263),
    textPrimary = Color(0xFFABB2BF),
    textSecondary = Color(0xFF828997),
    textMuted = Color(0xFF5C6370),
    warning = Color(0xFFE5C07B),
    error = Color(0xFFE06C75),
    syntaxKeyword = Color(0xFFC678DD),
    syntaxFlag = Color(0xFFE5C07B),
    syntaxString = Color(0xFF98C379),
    syntaxArg = Color(0xFFABB2BF),
    syntaxComment = Color(0xFF5C6370),
    syntaxNumber = Color(0xFFD19A66),
)

val AiModuleNordColors = AiModuleColors(
    background = Color(0xFF2E3440),
    surface = Color(0xFF242933),
    surfaceElevated = Color(0xFF3B4252),
    border = Color(0xFF434C5E),
    accent = Color(0xFF88C0D0),
    accentDim = Color(0xFF81A1C1),
    textPrimary = Color(0xFFD8DEE9),
    textSecondary = Color(0xFFE5E9F0),
    textMuted = Color(0xFF4C566A),
    warning = Color(0xFFEBCB8B),
    error = Color(0xFFBF616A),
    syntaxKeyword = Color(0xFF81A1C1),
    syntaxFlag = Color(0xFF8FBCBB),
    syntaxString = Color(0xFFA3BE8C),
    syntaxArg = Color(0xFFD8DEE9),
    syntaxComment = Color(0xFF4C566A),
    syntaxNumber = Color(0xFFB48EAD),
)

val AiModuleCyberpunkColors = AiModuleColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0D021A),
    surfaceElevated = Color(0xFF1B0533),
    border = Color(0xFF390066),
    accent = Color(0xFF00F0FF),
    accentDim = Color(0xFFFF0055),
    textPrimary = Color(0xFFE5E7EB),
    textSecondary = Color(0xFF00F0FF),
    textMuted = Color(0xFF5C1B8C),
    warning = Color(0xFFFDE24F),
    error = Color(0xFFFF0055),
    syntaxKeyword = Color(0xFFFF0055),
    syntaxFlag = Color(0xFF00F0FF),
    syntaxString = Color(0xFF39FF14),
    syntaxArg = Color(0xFFABB2BF),
    syntaxComment = Color(0xFF7B2CBF),
    syntaxNumber = Color(0xFFFFE600),
)

val AiModuleLightColors = AiModuleColors(
    background = Color(0xFFF6F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFF1F3F5),
    border = Color(0xFFE1E4E8),
    accent = Color(0xFF0969DA),
    accentDim = Color(0xFF218BFF),
    textPrimary = Color(0xFF24292F),
    textSecondary = Color(0xFF57606A),
    textMuted = Color(0xFF8C959F),
    warning = Color(0xFF9A6700),
    error = Color(0xFFCF222E),
    syntaxKeyword = Color(0xFFCF222E),
    syntaxFlag = Color(0xFF9A6700),
    syntaxString = Color(0xFF1A7F37),
    syntaxArg = Color(0xFF24292F),
    syntaxComment = Color(0xFF6E7781),
    syntaxNumber = Color(0xFF0550AE),
)

@Immutable
data class AiModuleTypography(
    val topBarTitle: TextUnit = 16.sp,
    val message: TextUnit = 14.sp,
    val code: TextUnit = 13.sp,
    val input: TextUnit = 14.sp,
    val toolCall: TextUnit = 13.sp,
    val costChip: TextUnit = 12.sp,
    val label: TextUnit = 12.sp,
)

val AiModuleDefaultTypography = AiModuleTypography()

// Default to AiModuleDarkColors instead of throwing. The AI module always
// wraps its screens in AiModuleSurface, but the GitHub module reuses these
// primitives in deeply-nested contexts (RepoCard called from Profile / Explore
// / AdvancedSearch, BranchPickerDialog opened from RepoDetailScreen, etc.)
// where forcing every call site to wrap is brittle and a missed wrap turns
// into a hard crash on entry. Falling back to the dark palette keeps the
// terminal-styled widgets visually consistent in any host composition.
val LocalAiModuleColors = compositionLocalOf<AiModuleColors> { AiModuleDarkColors }

val LocalAiModuleTypography = compositionLocalOf { AiModuleDefaultTypography }

object AiModuleTheme {
    val colors: AiModuleColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAiModuleColors.current

    val type: AiModuleTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalAiModuleTypography.current
}

@Composable
fun AiModuleSurface(
    modifier: Modifier = Modifier,
    colors: AiModuleColors? = null,
    typography: AiModuleTypography = AiModuleDefaultTypography,
    content: @Composable () -> Unit,
) {
    val resolvedColors = colors ?: when (ThemeState.mode) {
        gs.git.vps.data.AppThemeMode.DRACULA -> AiModuleDraculaColors
        gs.git.vps.data.AppThemeMode.GRUVBOX -> AiModuleGruvboxColors
        gs.git.vps.data.AppThemeMode.ONEDARK -> AiModuleOneDarkColors
        gs.git.vps.data.AppThemeMode.NORD -> AiModuleNordColors
        gs.git.vps.data.AppThemeMode.CYBERPUNK -> AiModuleCyberpunkColors
        gs.git.vps.data.AppThemeMode.LIGHT -> AiModuleLightColors
        else -> AiModuleDarkColors
    }
    CompositionLocalProvider(
        LocalAiModuleColors provides resolvedColors,
        LocalAiModuleTypography provides typography,
    ) {
        AiModuleContentBridge {
            Box(modifier.fillMaxSize().background(resolvedColors.background)) {
                content()
            }
        }
    }
}

@Composable
fun AiModuleContentBridge(content: @Composable () -> Unit) {
    content()
}

fun AiModuleColors.toCodeColors(): CodeColors = CodeColors(
    plain = textPrimary,
    keyword = syntaxKeyword,
    string = syntaxString,
    number = syntaxNumber,
    comment = syntaxComment,
    annotation = syntaxFlag,
)
