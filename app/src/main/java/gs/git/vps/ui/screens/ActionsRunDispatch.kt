package gs.git.vps.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import coil.compose.AsyncImage
import gs.git.vps.R
import gs.git.vps.data.Strings
import gs.git.vps.data.github.*
import gs.git.vps.data.github.model.GHActionSecret
import gs.git.vps.data.github.model.GHActionVariable
import gs.git.vps.data.github.model.GHCodespace
import gs.git.vps.data.github.model.GHEnvironment
import gs.git.vps.data.github.model.GHEnvironmentProtectionRule
import gs.git.vps.data.github.model.GHDeploymentBranchPolicy
import gs.git.vps.data.github.model.GHEnvironmentSecret
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleSectionLabel
import gs.git.vps.data.github.canWrite
import gs.git.vps.data.github.KernelErrorCatalog
import gs.git.vps.data.github.KernelErrorPatterns
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHActionRunner
import gs.git.vps.data.github.model.GHActionRunnerGroup
import gs.git.vps.data.github.model.GHActionsCacheEntry
import gs.git.vps.data.github.model.GHActionsCacheUsage
import gs.git.vps.data.github.model.GHActionsPermissions
import gs.git.vps.data.github.model.GHActionsRetention
import gs.git.vps.data.github.model.GHActionsUsage
import gs.git.vps.data.github.model.GHArtifact
import gs.git.vps.data.github.model.GHCheckAnnotation
import gs.git.vps.data.github.model.GHCheckRun
import gs.git.vps.data.github.model.GHDeployment
import gs.git.vps.data.github.model.GHJob
import gs.git.vps.data.github.model.GHPendingDeployment
import gs.git.vps.data.github.model.GHStep
import gs.git.vps.data.github.model.GHWorkflow
import gs.git.vps.data.github.model.GHWorkflowDispatchInput
import gs.git.vps.data.github.model.GHWorkflowDispatchSchema
import gs.git.vps.data.github.model.GHWorkflowPermissions
import gs.git.vps.data.github.model.GHWorkflowRun
import gs.git.vps.data.github.model.GHWorkflowRunReview
import gs.git.vps.ui.theme.Blue
import gs.git.vps.ui.theme.Green
import gs.git.vps.ui.theme.Orange
import gs.git.vps.ui.theme.Purple
import gs.git.vps.ui.theme.Red
import gs.git.vps.ui.theme.Teal
import gs.git.vps.ui.components.AiModuleDestructiveButton
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleGlyph
import gs.git.vps.ui.components.AiModuleGlyphAction
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModuleSearchField
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.components.AiModulePrimaryButton
import gs.git.vps.ui.components.AiModuleSecondaryButton
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.aiModuleStatusBadge
import gs.git.vps.ui.theme.AiModuleSurface
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import gs.git.vps.ui.theme.SeparatorColor
import gs.git.vps.ui.theme.SurfaceLight
import gs.git.vps.ui.theme.SurfaceWhite
import gs.git.vps.ui.theme.TextPrimary
import gs.git.vps.ui.theme.TextSecondary
import gs.git.vps.ui.theme.TextTertiary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Запуск workflow: DynamicDispatchInputs + поля dispatch-инпутов, ModernRunCard (карточка
 * запуска рана), сохранение/загрузка значений инпутов. Вынесено из GitHubActionsModule.kt
 * (Фаза 1, чистое перемещение).
 */

@Composable
internal fun DynamicDispatchInputs(
    schema: GHWorkflowDispatchSchema?,
    values: Map<String, String>,
    missingRequiredInputs: List<String>,
    onValueChange: (String, String) -> Unit
) {
    val inputs = schema?.inputs.orEmpty()
    val palette = AiModuleTheme.colors
    if (schema == null) {
        Text("This workflow has no workflow_dispatch trigger", fontSize = 11.sp, color = palette.textMuted, fontFamily = JetBrainsMono)
        return
    }
    if (inputs.isEmpty()) {
        Text("This workflow has no workflow_dispatch inputs", fontSize = 11.sp, color = palette.textMuted, fontFamily = JetBrainsMono)
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (missingRequiredInputs.isNotEmpty()) {
            Text(
                "Required inputs missing: ${missingRequiredInputs.joinToString(", ")}",
                fontSize = 11.sp,
                color = palette.warning,
                fontFamily = JetBrainsMono
            )
        }
        inputs.forEach { input ->
            WorkflowDispatchInputField(
                input = input,
                value = values[input.key].orEmpty(),
                onValueChange = { onValueChange(input.key, it) }
            )
        }
    }
}

@Composable
private fun WorkflowDispatchInputField(
    input: GHWorkflowDispatchInput,
    value: String,
    onValueChange: (String) -> Unit
) {
    val choices = dispatchInputChoices(input)
    val missingRequired = input.required && dispatchInputValue(input, mapOf(input.key to value)).isBlank()
    val palette = AiModuleTheme.colors
    val isDefault = value == input.defaultValue
    
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(input.key, fontSize = 12.sp, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            if (input.required) MiniActionsBadge("required", palette.warning)
            if (input.type.isNotBlank()) MiniActionsBadge(input.type, palette.textSecondary)
            Spacer(modifier = Modifier.weight(1f))
            if (!isDefault) {
                Text(
                    text = "reset to default",
                    fontSize = 10.sp,
                    color = palette.accent,
                    fontFamily = JetBrainsMono,
                    modifier = Modifier.clickable { onValueChange(input.defaultValue) }
                )
            }
        }
        if (input.description.isNotBlank()) {
            Text(input.description, fontSize = 10.sp, color = palette.textMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        
        if (input.type.lowercase() == "boolean") {
            val isTrue = value.lowercase() == "true"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(palette.surfaceElevated)
                    .border(1.dp, if (isTrue) palette.accent.copy(alpha = 0.5f) else palette.border, RoundedCornerShape(GitHubControlRadius))
                    .clickable { onValueChange((!isTrue).toString()) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isTrue) "Enabled (true)" else "Disabled (false)",
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMono,
                    color = if (isTrue) palette.accent else palette.textSecondary
                )
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isTrue) palette.accent.copy(alpha = 0.2f) else palette.border.copy(alpha = 0.3f))
                        .border(1.dp, if (isTrue) palette.accent else palette.border, RoundedCornerShape(10.dp))
                        .padding(2.dp),
                    contentAlignment = if (isTrue) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(if (isTrue) palette.accent else palette.textMuted)
                    )
                }
            }
        } else if (choices.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(GitHubControlRadius))
                        .background(palette.surfaceElevated)
                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                        .clickable { expanded = true }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = value.ifBlank { "Select option..." },
                        fontSize = 12.sp,
                        fontFamily = JetBrainsMono,
                        color = if (value.isNotBlank()) palette.textPrimary else palette.textMuted
                    )
                    Text(
                        text = "▼",
                        fontSize = 8.sp,
                        color = palette.textMuted,
                        fontFamily = JetBrainsMono
                    )
                }
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(palette.surface)
                        .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
                ) {
                    choices.forEach { choice ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    choice, 
                                    fontFamily = JetBrainsMono, 
                                    fontSize = 12.sp, 
                                    color = if (value == choice) palette.accent else palette.textPrimary 
                                ) 
                            },
                            onClick = {
                                onValueChange(choice)
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = palette.textPrimary,
                                leadingIconColor = palette.accent
                            )
                        )
                    }
                }
            }
        } else {
            AiModuleTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = input.key,
                modifier = Modifier.fillMaxWidth(),
                singleLine = input.type.lowercase() != "environment"
            )
        }
        if (missingRequired) {
            Text("Required value", fontSize = 10.sp, color = palette.warning)
        }
    }
}

private fun dispatchInputChoices(input: GHWorkflowDispatchInput): List<String> = when {
    input.options.isNotEmpty() -> input.options
    input.type.equals("boolean", ignoreCase = true) -> listOf("true", "false")
    else -> emptyList()
}

internal fun missingDispatchInputs(schema: GHWorkflowDispatchSchema?, values: Map<String, String>): List<String> =
    schema?.inputs.orEmpty()
        .filter { it.required && dispatchInputValue(it, values).isBlank() }
        .map { it.key }

internal fun dispatchInputValue(input: GHWorkflowDispatchInput, values: Map<String, String>): String =
    values[input.key].orEmpty().ifBlank { input.defaultValue }.trim()

internal fun loadSavedDispatchInputValues(
    context: Context,
    repo: GHRepo,
    workflow: GHWorkflow,
    schema: GHWorkflowDispatchSchema
): Map<String, String> {
    val prefs = context.getSharedPreferences(ACTIONS_INPUT_PREFS, Context.MODE_PRIVATE)
    return schema.inputs.associate { input ->
        input.key to (prefs.getString(dispatchInputPrefKey(repo, workflow, input.key), null) ?: input.defaultValue)
    }
}

internal fun saveDispatchInputValues(
    context: Context,
    repo: GHRepo,
    workflow: GHWorkflow,
    values: Map<String, String>
) {
    val editor = context.getSharedPreferences(ACTIONS_INPUT_PREFS, Context.MODE_PRIVATE).edit()
    values.forEach { (key, value) ->
        editor.putString(dispatchInputPrefKey(repo, workflow, key), value)
    }
    editor.apply()
}

private fun dispatchInputPrefKey(repo: GHRepo, workflow: GHWorkflow, inputKey: String): String =
    "${repo.owner}/${repo.name}/${workflow.id}/$inputKey"

@Composable
private fun ModernRunCard(
    run: GHWorkflowRun,
    nowMs: Long,
    canWrite: Boolean = true,
    onRunClick: () -> Unit,
    onCancel: () -> Unit,
    onRerun: () -> Unit
) {
    val statusColor = runStatusColor(run)
    val live = isRunActive(run)
    val elapsed = calcRunDuration(run, nowMs)
    val colors = AiModuleTheme.colors

    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .ghGlassCard(12.dp)
            .clickable(onClick = onRunClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                if (run.actorAvatar.isNotBlank()) {
                    AsyncImage(
                        model = run.actorAvatar,
                        contentDescription = run.actor,
                        modifier = Modifier.size(32.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(colors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(runStatusIcon(run), null, tint = statusColor, modifier = Modifier.size(16.dp))
                    }
                }
                if (live) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .padding(1.dp)
                    ) {
                        Box(
                            Modifier.fillMaxSize().clip(CircleShape).background(statusColor)
                        )
                    }
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    run.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (run.displayTitle.isNotBlank()) {
                    Text(
                        run.displayTitle,
                        fontSize = 12.sp,
                        color = colors.textMuted,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                "#${run.runNumber}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = colors.textMuted,
                maxLines = 1
            )
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniActionsBadge(displayRunStatus(run), statusColor)
            if (run.branch.isNotBlank()) MiniActionsBadge(run.branch, Blue)
            if (run.event.isNotBlank()) MiniActionsBadge(run.event, Purple)
            if (run.runAttempt > 1) MiniActionsBadge("attempt ${run.runAttempt}", Orange)
        }

        val footerParts = buildList {
            if (run.actor.isNotBlank()) add(run.actor)
            if (elapsed.isNotBlank()) add(elapsed)
            if (run.headSha.length >= 7) add(run.headSha.take(7))
        }
        if (footerParts.isNotEmpty()) {
            Text(
                footerParts.joinToString("  ·  "),
                fontSize = 11.sp,
                color = colors.textMuted,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border.copy(alpha = 0.10f))
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canWrite) {
                if (live) {
                    Chip(Icons.Rounded.Cancel, Strings.cancel, Red) { onCancel() }
                } else {
                    Chip(Icons.Rounded.Refresh, Strings.ghRerun) { onRerun() }
                }
            }
            Spacer(Modifier.weight(1f))
            Chip(Icons.Rounded.Article, "Open") { onRunClick() }
        }
    }
}
