package gs.git.vps.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gs.git.vps.data.github.GHOrg
import gs.git.vps.ui.theme.AiModuleTheme
import gs.git.vps.ui.theme.JetBrainsMono
import androidx.compose.foundation.layout.Spacer
import gs.git.vps.data.github.GHPackage
import gs.git.vps.data.github.GHPackageVersion
import gs.git.vps.data.github.GitHubManager
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import kotlinx.coroutines.launch

private data class PackageOwner(val type: String, val login: String)

private val PACKAGE_TYPES = listOf("all", "container", "docker", "npm", "maven", "nuget", "rubygems")

@Composable
internal fun PackagesScreen(userLogin: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var orgs by remember { mutableStateOf<List<GHOrg>>(emptyList()) }
    var selectedOwner by remember(userLogin) { mutableStateOf(PackageOwner("user", userLogin)) }
    var selectedType by remember { mutableStateOf("all") }
    var packages by remember { mutableStateOf<List<GHPackage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var selectedPackage by remember { mutableStateOf<GHPackage?>(null) }

    fun loadPackages() {
        loading = true
        scope.launch {
            packages = if (selectedOwner.type == "org") {
                GitHubManager.getOrgPackages(context, selectedOwner.login, selectedType)
            } else {
                GitHubManager.getUserPackages(context, selectedOwner.login, selectedType)
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { orgs = GitHubManager.getOrganizations(context) }
    LaunchedEffect(selectedOwner, selectedType) { loadPackages() }

    fun handlePackagesBack() {
        if (selectedPackage != null) selectedPackage = null else onBack()
    }

    selectedPackage?.let { pkg ->
        PackageDetailScreen(
            owner = selectedOwner,
            pkg = pkg,
            onBack = ::handlePackagesBack,
            onDeleted = {
                selectedPackage = null
                loadPackages()
            }
        )
        return
    }

    val visiblePackages = packages.filter {
        query.isBlank() ||
            it.name.contains(query, ignoreCase = true) ||
            it.packageType.contains(query, ignoreCase = true) ||
            it.repositoryName.contains(query, ignoreCase = true)
    }

    GitHubScreenFrame(
        title = "> packages",
        onBack = ::handlePackagesBack,
        trailing = {
            GitHubTopBarAction(
                glyph = GhGlyphs.REFRESH,
                onClick = { loadPackages() },
                tint = AiModuleTheme.colors.accent,
                contentDescription = "refresh packages",
            )
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { PackagesSummaryCard(packages) }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OwnerChip(Icons.Rounded.Person, selectedOwner.login, selectedOwner.type == "user") {
                        selectedOwner = PackageOwner("user", userLogin)
                    }
                    orgs.forEach { org ->
                        OwnerChip(Icons.Rounded.Business, org.login, selectedOwner.type == "org" && selectedOwner.login == org.login) {
                            selectedOwner = PackageOwner("org", org.login)
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PACKAGE_TYPES.forEach { type ->
                        PackageFilterChip(type, selectedType == type) { selectedType = type }
                    }
                }
            }
            item {
                AiModuleTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Search packages",
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leading = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp), tint = AiModuleTheme.colors.textSecondary) }
                )
            }
            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                        AiModuleSpinner(label = "loading…")
                    }
                }
            } else {
                items(visiblePackages) { pkg ->
                    PackageCard(pkg) { selectedPackage = pkg }
                }
                if (visiblePackages.isEmpty()) {
                    item {
                        EmptyPackagesCard(if (packages.isEmpty()) "No packages returned" else "No matching packages")
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageDetailScreen(owner: PackageOwner, pkg: GHPackage, onBack: () -> Unit, onDeleted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var detail by remember(pkg.id) { mutableStateOf(pkg) }
    var versions by remember(pkg.id) { mutableStateOf<List<GHPackageVersion>>(emptyList()) }
    var loading by remember(pkg.id) { mutableStateOf(true) }
    var deletePackageConfirm by remember { mutableStateOf(false) }
    var deleteVersionConfirm by remember { mutableStateOf<GHPackageVersion?>(null) }
    var actionInFlight by remember { mutableStateOf(false) }

    fun loadDetail() {
        loading = true
        scope.launch {
            detail = GitHubManager.getPackage(context, owner.type, owner.login, pkg.packageType, pkg.name) ?: pkg
            versions = GitHubManager.getPackageVersions(context, owner.type, owner.login, pkg.packageType, pkg.name)
            loading = false
        }
    }

    LaunchedEffect(owner, pkg.id) { loadDetail() }

    fun handlePackageDetailBack() {
        when {
            deleteVersionConfirm != null -> deleteVersionConfirm = null
            deletePackageConfirm -> deletePackageConfirm = false
            else -> onBack()
        }
    }

    GitHubScreenFrame(
        title = "> ${detail.name.ifBlank { "package" }.lowercase()}",
        onBack = ::handlePackageDetailBack,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GitHubTopBarAction(
                    glyph = GhGlyphs.REFRESH,
                    onClick = { loadDetail() },
                    tint = AiModuleTheme.colors.accent,
                    contentDescription = "refresh package",
                )
                if (detail.htmlUrl.isNotBlank()) {
                    GitHubTopBarAction(
                        glyph = GhGlyphs.OPEN_NEW,
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(detail.htmlUrl))) },
                        tint = AiModuleTheme.colors.accent,
                        contentDescription = "open package",
                    )
                }
                GitHubTopBarAction(
                    glyph = GhGlyphs.DELETE,
                    onClick = { deletePackageConfirm = true },
                    enabled = !actionInFlight,
                    tint = AiModuleTheme.colors.error,
                    contentDescription = "delete package",
                )
                GitHubTopBarAction(
                    glyph = GhGlyphs.CHECK,
                    onClick = {
                        scope.launch {
                            actionInFlight = true
                            val ok = GitHubManager.restorePackage(context, owner.type, owner.login, detail.packageType, detail.name)
                            Toast.makeText(context, if (ok) "Package restored" else "Failed", Toast.LENGTH_SHORT).show()
                            actionInFlight = false
                        }
                    },
                    enabled = !actionInFlight,
                    tint = AiModuleTheme.colors.accent,
                    contentDescription = "restore package",
                )
            }
        },
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { PackageHeaderCard(detail, owner) }
            if (versions.isNotEmpty()) {
                val latestVer = versions.firstOrNull()?.displayName() ?: "latest"
                item { ImportSnippetsPanel(detail, latestVer) }
            }
            item {
                Text("Versions", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary)
            }
            if (loading) {
                item {
                    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        AiModuleSpinner(label = "loading…")
                    }
                }
            } else {
                items(versions) { version ->
                    PackageVersionCard(
                        version = version,
                        onOpen = {
                            val url = version.htmlUrl.ifBlank { detail.htmlUrl }
                            if (url.isNotBlank()) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        },
                        onDelete = { deleteVersionConfirm = version }
                    )
                }
                if (versions.isEmpty()) item { EmptyPackagesCard("No package versions returned") }
            }
        }
    }

    if (deletePackageConfirm) {
        AiModuleAlertDialog(
            onDismissRequest = { deletePackageConfirm = false },
            title = "Delete package",
            content = { Text("Delete ${detail.name}? This removes the package from GitHub Packages.") },
            confirmButton = {
                AiModuleTextAction(
                    label = "delete",
                    enabled = !actionInFlight,
                    onClick = {
                        actionInFlight = true
                        scope.launch {
                            val ok = GitHubManager.deletePackage(context, owner.type, owner.login, detail.packageType, detail.name)
                            Toast.makeText(context, if (ok) "Package deleted" else "Failed", Toast.LENGTH_SHORT).show()
                            actionInFlight = false
                            deletePackageConfirm = false
                            if (ok) onDeleted()
                        }
                    },
                    tint = Color(0xFFFF3B30),
                )
            },
            dismissButton = {
                AiModuleTextAction(label = "cancel", onClick = { deletePackageConfirm = false }, tint = AiModuleTheme.colors.textSecondary)
            },
        )
    }

    deleteVersionConfirm?.let { version ->
        AiModuleAlertDialog(
            onDismissRequest = { deleteVersionConfirm = null },
            title = "Delete version",
            content = { Text("Delete version ${version.displayName()}?") },
            confirmButton = {
                AiModuleTextAction(
                    label = "delete",
                    enabled = !actionInFlight,
                    onClick = {
                        actionInFlight = true
                        scope.launch {
                            val ok = GitHubManager.deletePackageVersion(context, owner.type, owner.login, detail.packageType, detail.name, version.id)
                            Toast.makeText(context, if (ok) "Version deleted" else "Failed", Toast.LENGTH_SHORT).show()
                            if (ok) versions = versions.filterNot { it.id == version.id }
                            actionInFlight = false
                            deleteVersionConfirm = null
                        }
                    },
                    tint = Color(0xFFFF3B30),
                )
            },
            dismissButton = {
                AiModuleTextAction(label = "cancel", onClick = { deleteVersionConfirm = null }, tint = AiModuleTheme.colors.textSecondary)
            },
        )
    }
}

@Composable
private fun ImportSnippetsPanel(pkg: GHPackage, version: String) {
    val palette = AiModuleTheme.colors
    val context = LocalContext.current
    val name = pkg.name
    val type = pkg.packageType.lowercase()

    val tools = when (type) {
        "npm" -> listOf("npm", "yarn", "pnpm")
        "maven" -> listOf("gradle", "maven")
        "nuget" -> listOf("dotnet", "nuget pm")
        "rubygems" -> listOf("gem", "bundler")
        "container", "docker" -> listOf("docker", "dockerfile")
        else -> listOf("npm", "gradle", "dotnet", "docker")
    }

    var activeTool by remember(type) { mutableStateOf(tools.firstOrNull() ?: "npm") }

    val snippet = when (activeTool) {
        "npm" -> "npm install $name@$version"
        "yarn" -> "yarn add $name@$version"
        "pnpm" -> "pnpm add $name@$version"
        "gradle" -> "implementation(\"gh:${pkg.repositoryName}:$version\")"
        "maven" -> """
            <dependency>
              <groupId>gh.repo</groupId>
              <artifactId>$name</artifactId>
              <version>$version</version>
            </dependency>
        """.trimIndent()
        "dotnet" -> "dotnet add package $name --version $version"
        "nuget pm" -> "Install-Package $name -Version $version"
        "gem" -> "gem install $name -v $version"
        "bundler" -> "gem '$name', '~> $version'"
        "docker" -> "docker pull ghcr.io/${pkg.repositoryName.lowercase()}:$version"
        "dockerfile" -> "FROM ghcr.io/${pkg.repositoryName.lowercase()}:$version"
        else -> "install $name version $version"
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(palette.surface)
            .border(1.dp, palette.border, RoundedCornerShape(GitHubControlRadius))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "install instructions",
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accent
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.clickable {
                    val clip = android.content.ClipData.newPlainText("snippet", snippet)
                    (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                    Toast.makeText(context, "copied to clipboard", Toast.LENGTH_SHORT).show()
                }.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("[ copy ]", fontFamily = JetBrainsMono, fontSize = 11.sp, color = palette.accent)
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tools.forEach { tool ->
                val sel = activeTool == tool
                Box(
                    Modifier
                        .clickable { activeTool = tool }
                        .background(if (sel) palette.accent.copy(alpha = 0.12f) else palette.background, RoundedCornerShape(4.dp))
                        .border(1.dp, if (sel) palette.accent else palette.border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        tool,
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        color = if (sel) palette.accent else palette.textSecondary
                    )
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .background(palette.background, RoundedCornerShape(4.dp))
                .border(1.dp, palette.border, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            Text(
                snippet,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                color = palette.textPrimary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun PackagesSummaryCard(packages: List<GHPackage>) {
    val publicCount = packages.count { it.visibility == "public" }
    val privateCount = packages.count { it.visibility == "private" }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Archive, null, Modifier.size(18.dp), tint = AiModuleTheme.colors.accent)
            Column(Modifier.weight(1f)) {
                Text("GitHub Packages", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary)
                Text("${packages.size} packages loaded", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PackageStatPill("Public", publicCount, Color(0xFF34C759))
            PackageStatPill("Private", privateCount, AiModuleTheme.colors.textSecondary)
            PackageStatPill("Versions", packages.sumOf { it.versionCount }, AiModuleTheme.colors.accent)
        }
    }
}

@Composable
private fun PackageHeaderCard(pkg: GHPackage, owner: PackageOwner) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Archive, null, Modifier.size(22.dp), tint = AiModuleTheme.colors.accent)
            Column(Modifier.weight(1f)) {
                Text(pkg.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AiModuleTheme.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${owner.login} - ${pkg.packageType.ifBlank { "package" }}", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted)
            }
            PackagePill(pkg.visibility.ifBlank { "unknown" }, if (pkg.visibility == "public") Color(0xFF34C759) else AiModuleTheme.colors.textSecondary)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PackageStatPill("Versions", pkg.versionCount, AiModuleTheme.colors.accent)
            if (pkg.repositoryName.isNotBlank()) PackagePill(pkg.repositoryName, AiModuleTheme.colors.textSecondary)
            PackagePill("Updated ${pkg.updatedAt.shortDate()}", AiModuleTheme.colors.textMuted)
        }
    }
}

@Composable
private fun PackageCard(pkg: GHPackage, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).clickable(onClick = onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Archive, null, Modifier.size(20.dp), tint = AiModuleTheme.colors.accent)
            Column(Modifier.weight(1f)) {
                Text(pkg.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOf(pkg.packageType, pkg.repositoryName).filter { it.isNotBlank() }.joinToString(" - "), fontSize = 11.sp, color = AiModuleTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = AiModuleTheme.colors.textMuted)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PackagePill(pkg.visibility.ifBlank { "unknown" }, if (pkg.visibility == "public") Color(0xFF34C759) else AiModuleTheme.colors.textSecondary)
            PackageStatPill("Versions", pkg.versionCount, AiModuleTheme.colors.accent)
            PackagePill(pkg.updatedAt.shortDate(), AiModuleTheme.colors.textMuted)
        }
    }
}

@Composable
private fun PackageVersionCard(version: GHPackageVersion, onOpen: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Archive, null, Modifier.size(18.dp), tint = AiModuleTheme.colors.textSecondary)
            Column(Modifier.weight(1f)) {
                Text(version.displayName(), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AiModuleTheme.colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Updated ${version.updatedAt.shortDate()}", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.OpenInNew, null, Modifier.size(18.dp), tint = AiModuleTheme.colors.accent)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp), tint = Color(0xFFFF3B30))
            }
        }
        if (version.tags.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                version.tags.take(8).forEach { tag -> PackagePill(tag, AiModuleTheme.colors.accent) }
                if (version.tags.size > 8) PackagePill("+${version.tags.size - 8}", AiModuleTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun OwnerChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(GitHubControlRadius)).background(if (selected) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.surface).border(1.dp, if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textSecondary)
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textPrimary)
    }
}

@Composable
private fun PackageFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(999.dp)).background(if (selected) AiModuleTheme.colors.accent.copy(alpha = 0.14f) else AiModuleTheme.colors.surface).border(1.dp, if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = if (selected) AiModuleTheme.colors.accent else AiModuleTheme.colors.textSecondary)
    }
}

@Composable
private fun PackageStatPill(label: String, count: Int, color: Color) {
    PackagePill("$label $count", color)
}

@Composable
private fun PackagePill(label: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(label, fontSize = 11.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun EmptyPackagesCard(text: String) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.surface).border(1.dp, AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Archive, null, Modifier.size(28.dp), tint = AiModuleTheme.colors.textMuted)
        Text(text, fontSize = 13.sp, color = AiModuleTheme.colors.textSecondary)
    }
}

private fun String.shortDate(): String =
    takeIf { it.length >= 10 }?.take(10) ?: ifBlank { "unknown" }

private fun GHPackageVersion.displayName(): String =
    tags.firstOrNull()?.takeIf { it.isNotBlank() } ?: name.ifBlank { "#$id" }
