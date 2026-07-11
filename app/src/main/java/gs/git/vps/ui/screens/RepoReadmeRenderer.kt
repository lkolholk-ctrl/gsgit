package gs.git.vps.ui.screens

import android.content.Intent
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.em
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor


import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.CachePolicy
import coil.request.ImageRequest
import androidx.compose.material.icons.outlined.Link
import gs.git.vps.data.Strings
import gs.git.vps.data.github.*
import gs.git.vps.data.github.model.GHContributor
import gs.git.vps.data.github.model.GHReaction
import gs.git.vps.data.github.model.GHBlameRange
import gs.git.vps.data.github.model.GHGitCommit
import gs.git.vps.data.github.model.GHGitTagDetail
import gs.git.vps.data.github.model.GHGitBlob
import gs.git.vps.data.github.model.GHGitTree
import gs.git.vps.data.github.model.GHGitRef
import gs.git.vps.data.github.model.GHCommitStatus
import gs.git.vps.data.github.model.GHCommitDetail
import gs.git.vps.data.github.model.GHCommit
import gs.git.vps.data.github.model.GHRelease
import gs.git.vps.data.github.model.GHContent
import gs.git.vps.data.github.model.GHRepo
import gs.git.vps.data.github.model.GHRepoEvent
import gs.git.vps.data.github.model.GHRepoPerson
import gs.git.vps.data.github.model.GHTrafficPath
import gs.git.vps.data.github.model.GHTrafficReferrer
import gs.git.vps.data.github.model.GHTrafficSeries
import gs.git.vps.data.github.model.GHCheckRun
import gs.git.vps.data.github.model.GHCheckSuite
import gs.git.vps.data.github.model.GHPullRequest
import gs.git.vps.data.github.model.GHPullMergeStatus
import gs.git.vps.data.github.model.GHPullReview
import gs.git.vps.data.github.model.GHPullFile
import gs.git.vps.data.github.model.GHReviewComment
import gs.git.vps.data.github.model.GHWorkflow
import gs.git.vps.data.github.model.GHWorkflowRun
import gs.git.vps.data.github.model.GHComment
import gs.git.vps.data.github.model.GHIssue
import gs.git.vps.data.github.model.GHIssueDetail
import gs.git.vps.data.github.model.GHIssueEvent
import gs.git.vps.data.github.model.GHLabel
import gs.git.vps.data.github.model.GHMilestone
import gs.git.vps.data.github.model.GHTimelineEvent
import gs.git.vps.notifications.GitHubNotificationTarget
import gs.git.vps.ui.components.AiModuleAlertDialog
import gs.git.vps.ui.components.AiModuleGlyph
import gs.git.vps.ui.components.AiModuleGlyphAction
import gs.git.vps.ui.components.AiModuleIcon
import gs.git.vps.ui.components.AiModuleIcon as Icon
import gs.git.vps.ui.components.AiModuleIconButton
import gs.git.vps.ui.components.AiModuleIconButton as IconButton
import gs.git.vps.ui.components.AiModulePageBar
import gs.git.vps.ui.components.AiModulePillButton
import gs.git.vps.ui.components.AiModuleHairline
import gs.git.vps.ui.components.AiModuleSearchField
import gs.git.vps.ui.components.AiModuleSpinner
import gs.git.vps.ui.components.AiModuleText
import gs.git.vps.ui.components.AiModuleText as Text
import gs.git.vps.ui.components.AiModuleTextAction
import gs.git.vps.ui.components.AiModuleTextField
import gs.git.vps.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import java.io.File
import java.net.URLEncoder
import okhttp3.OkHttpClient
import org.json.JSONObject

/**
 * README-рендерер репо-модуля: загрузка README, парсинг markdown/HTML в блоки, рендер,
 * HTML-документ-вьюха, загрузка картинок, резолв ссылок. Вынесено из GitHubRepoModule.kt
 * (Фаза 1 декомпозиции UI — чистое разнесение по файлам, поведение не менялось). Тот же пакет
 * `gs.git.vps.ui.screens`; внешне используемые символы (ReadmeTab, ReadmeHtmlDocument,
 * fetchReadmeForRender, resolveReadmeLink, GitHubMarkdownDocument, parseReadmeBlocks,
 * ReadmeBlockView, ReadmeRenderBlock, ReadmeFetchResult) — `internal`.
 */

internal const val README_RENDER_TAG = "ReadmeRender"
internal const val README_MAX_RENDER_BYTES = 500 * 1024
internal const val README_FETCH_TIMEOUT_MS = 10_000L
internal const val README_TOTAL_TIMEOUT_MS = 15_000L
internal const val README_IMAGE_TIMEOUT_MS = 5_000L
internal const val README_MAX_CODE_LINES = 1_000
internal const val README_MAX_TABLE_ROWS = 50
internal const val README_MAX_LINE_CHARS = 4_000
internal const val README_DEFAULT_IMAGE_ASPECT_RATIO = 16f / 9f
internal const val README_IMAGE_USER_AGENT = "GsGit-Android/1.0"
internal val README_PLAIN_URL_REGEX = Regex("""https?://[^\s<>)"]+""")

// Regression test repos (must not freeze):
// - d2phap/imageglass (large with HTML and images)
// - microsoft/vscode (large general)
// - public-apis/public-apis (huge table)

internal data class ReadmeFetchResult(val markdown: String, val renderedHtml: String, val path: String)

internal suspend fun fetchReadmeForRender(context: Context, owner: String, repo: String, ref: String?): ReadmeFetchResult {
    val encodedOwner = owner.encodeGithubPathPart()
    val encodedRepo = repo.encodeGithubPathPart()
    val refQuery = ref?.takeIf { it.isNotBlank() }?.let { "?ref=${it.encodeGithubPathPart()}" }.orEmpty()
    val endpoint = "/repos/$encodedOwner/$encodedRepo/readme$refQuery"

    val rawResult = GitHubManager.request(
        context,
        endpoint,
        extraHeaders = mapOf("Accept" to "application/vnd.github+json"),
        trackErrors = false,
    )
    val markdownResult = if (!rawResult.success) {
        Log.w(README_RENDER_TAG, "raw fetch HTTP ${rawResult.code} $owner/$repo")
        "" to ""
    } else {
        val json = JSONObject(rawResult.body)
        val content = json.optString("content", "")
        val path = json.optString("path", "")
        val markdown = if (content.isBlank()) {
            ""
        } else {
            String(android.util.Base64.decode(content.replace("\n", ""), android.util.Base64.DEFAULT))
        }
        markdown to path
    }

    val htmlResult = GitHubManager.request(
        context,
        endpoint,
        extraHeaders = mapOf("Accept" to "application/vnd.github.html+json"),
        trackErrors = false,
    )
    val renderedHtml = if (!htmlResult.success) {
        Log.w(README_RENDER_TAG, "html fetch HTTP ${htmlResult.code} $owner/$repo")
        ""
    } else {
        htmlResult.body
    }

    return ReadmeFetchResult(
        markdown = markdownResult.first,
        renderedHtml = renderedHtml,
        path = markdownResult.second,
    )
}

private fun String.encodeGithubPathPart(): String =
    URLEncoder.encode(this, Charsets.UTF_8.name()).replace("+", "%20")

@Composable
internal fun ReadmeTab(
    readme: String?,
    renderedHtml: String?,
    blocks: List<ReadmeRenderBlock>?,
    error: String?,
    languages: Map<String, Long>,
    contributors: List<GHContributor>,
    releases: List<GHRelease>,
    repo: GHRepo,
    onRetry: () -> Unit,
    onOpenFile: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val readmeImageLoader = rememberReadmeImageLoader(context)
    val colors = AiModuleTheme.colors
    val onLinkClick: (String) -> Unit = { url ->
        if (url.isNotBlank() && !url.startsWith("#")) {
            val resolved = resolveReadmeLink(url, repo)
            if (resolved != null) onOpenFile(resolved)
            else context.openReadmeUrl(url)
        }
    }
    var rawView by remember(readme) { mutableStateOf(false) }
    var visibleCount by remember(readme) { mutableIntStateOf(250) }
    var renderCompleteLogged by remember(readme, blocks?.size ?: -1) { mutableStateOf(false) }
    val safeBlocks = blocks.orEmpty()
    val shownBlocks = safeBlocks.take(visibleCount)

    when {
        !renderedHtml.isNullOrBlank() && error == null && !rawView -> {
            Box(Modifier.fillMaxSize().background(colors.background)) {
                ReadmeHtmlDocument(
                    html = renderedHtml,
                    repo = repo,
                    modifier = Modifier.fillMaxSize(),
                    onNavigateLink = onLinkClick,
                )
            }
        }
        else -> {
            if (!readme.isNullOrBlank() && error == null && !rawView) {
                LaunchedEffect(readme, safeBlocks.size) {
                    Log.d(README_RENDER_TAG, "render start ${repo.owner}/${repo.name} blocks=${safeBlocks.size}")
                }
                SideEffect {
                    if (!renderCompleteLogged) {
                        Log.d(README_RENDER_TAG, "render complete ${repo.owner}/${repo.name} blocks=${safeBlocks.size}")
                        renderCompleteLogged = true
                    }
                }
            }
            LazyColumn(
                Modifier.fillMaxSize().background(colors.background),
                contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 28.dp),
            ) {
                when {
                    error != null -> item {
                        ReadmeErrorCard(error, readme.orEmpty(), repo, onRetry = onRetry)
                    }
                    readme.isNullOrBlank() -> item {
                        Text(Strings.ghNoReadme, fontSize = 15.sp, color = colors.textMuted, lineHeight = 22.sp)
                    }
                    rawView -> item {
                        ReadmeRawBlock(readme.orEmpty())
                    }
                    shownBlocks.isEmpty() -> item {
                        ReadmeErrorCard("README has no renderable markdown blocks.", readme.orEmpty(), repo, onViewRaw = { rawView = true })
                    }
                    else -> {
                        item(key = "readme_doc_top_${repo.owner}_${repo.name}") {
                            Spacer(Modifier.height(2.dp))
                        }
                        items(shownBlocks, key = { it.stableId }) { block ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                            ) {
                                ReadmeBlockView(block, readmeImageLoader, onLinkClick)
                            }
                        }
                        item(key = "readme_doc_bottom_${repo.owner}_${repo.name}_${shownBlocks.size}") {
                            Spacer(Modifier.height(16.dp))
                        }
                        if (visibleCount < safeBlocks.size) {
                            item {
                                GitHubTerminalButton(
                                    "expand more README content (${safeBlocks.size - visibleCount} hidden)",
                                    onClick = { visibleCount += 250 },
                                    color = AiModuleTheme.colors.accent,
                                )
                            }
                        }
                    }
                }
                item {
                    ReadmeRepositoryFooter(repo, releases, contributors, languages)
                }
            }
        }
    }
}

@Composable
internal fun ReadmeHtmlDocument(
    html: String,
    repo: GHRepo,
    modifier: Modifier = Modifier,
    onNavigateLink: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val colors = AiModuleTheme.colors
    val scope = rememberCoroutineScope()
    
    val bgHex = colors.background.toHex()
    val textHex = colors.textPrimary.toHex()
    val mutedHex = colors.textMuted.toHex()
    val secondaryHex = colors.textSecondary.toHex()
    val borderHex = colors.border.toHex()
    val surfaceHex = colors.surface.toHex()
    val accentHex = colors.accent.toHex()
    val colorScheme = if (colors.background.luminance() > 0.5f) "light" else "dark"

    val pageHtml = remember(html, bgHex, textHex, mutedHex, secondaryHex, borderHex, surfaceHex, accentHex, colorScheme) {
        buildGitHubReadmeHtmlPage(
            readmeHtml = html,
            bg = bgHex,
            text = textHex,
            muted = mutedHex,
            secondary = secondaryHex,
            border = borderHex,
            surface = surfaceHex,
            accent = accentHex,
            colorScheme = colorScheme
        )
    }
    
    val baseUrl = remember(repo.owner, repo.name) { "https://github.com/${repo.owner}/${repo.name}/" }
    val loadTag = remember(baseUrl, pageHtml) { "${baseUrl.hashCode()}:${pageHtml.hashCode()}" }
    
    var headings by remember { mutableStateOf<List<HeadingItem>>(emptyList()) }
    var showToCDrawer by remember { mutableStateOf(false) }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    
    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize().background(colors.background),
            factory = { ctx ->
                WebView(ctx).apply {
                    activeWebView = this
                    setBackgroundColor(colors.background.toArgb())
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.builtInZoomControls = false
                    settings.displayZoomControls = false
                    settings.loadsImagesAutomatically = true
                    settings.textZoom = 100
                    overScrollMode = WebView.OVER_SCROLL_NEVER
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    
                    addJavascriptInterface(ToCInterface(context) { parsedHeadings ->
                        headings = parsedHeadings
                    }, "ToCInterface")
                    
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val uri = request?.url ?: return false
                            val scheme = uri.scheme.orEmpty()
                            if (scheme != "http" && scheme != "https") return false
                            val repoAnchor = uri.host == "github.com" &&
                                uri.path == "/${repo.owner}/${repo.name}/" &&
                                !uri.fragment.isNullOrBlank()
                            if (repoAnchor) return false
                            // Route internal file/dir links through the file viewer
                            val resolved = resolveReadmeLink(uri.toString(), repo)
                            if (resolved != null) {
                                onNavigateLink(uri.toString())
                                return true
                            }
                            // Same-repo github.com URLs that didn't resolve: also intercept
                            // to prevent deep-link loop back into our own app
                            val seg = uri.pathSegments
                            if (uri.host in listOf("github.com", "www.github.com") &&
                                seg.size >= 2 && seg[0] == repo.owner && seg[1] == repo.name) {
                                onNavigateLink(uri.toString())
                                return true
                            }
                            return runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                true
                            }.getOrDefault(false)
                        }
                    }
                }
            },
            update = { webView ->
                if (webView.tag != loadTag) {
                    webView.tag = loadTag
                    webView.loadDataWithBaseURL(baseUrl, pageHtml, "text/html", "UTF-8", null)
                }
            },
        )
        
        if (headings.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(GitHubControlRadius))
                    .background(colors.accent.copy(alpha = 0.90f))
                    .clickable { showToCDrawer = true }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = "[= CONTENT =]",
                    color = colors.background,
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (showToCDrawer) {
            AiModuleAlertDialog(
                onDismissRequest = { showToCDrawer = false },
                title = "table of contents",
                content = {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(headings) { item ->
                            val indent = ((item.level - 1) * 12).dp
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = indent)
                                    .clip(RoundedCornerShape(GitHubControlRadius))
                                    .background(colors.surfaceElevated)
                                    .clickable {
                                        showToCDrawer = false
                                        activeWebView?.evaluateJavascript("scrollToElement('${item.id}')", null)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#".repeat(item.level) + " " + item.title,
                                    color = if (item.level == 1) colors.accent else colors.textPrimary,
                                    fontSize = (14 - item.level).coerceAtLeast(11).sp,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = if (item.level == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    AiModuleTextAction(label = "close", onClick = { showToCDrawer = false })
                }
            )
        }
    }
}

private fun buildGitHubReadmeHtmlPage(
    readmeHtml: String,
    bg: String,
    text: String,
    muted: String,
    secondary: String,
    border: String,
    surface: String,
    accent: String,
    colorScheme: String
): String = """
<!doctype html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"></script>
<script type="module">
  import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';
  mermaid.initialize({ startOnLoad: true, theme: '${if (colorScheme == "light") "default" else "dark"}' });
</script>
<script>
  document.addEventListener("DOMContentLoaded", function() {
    document.querySelectorAll('pre code.language-mermaid').forEach(function(codeBlock) {
      var pre = codeBlock.parentNode;
      var div = document.createElement('div');
      div.className = 'mermaid';
      div.textContent = codeBlock.textContent;
      pre.parentNode.replaceChild(div, pre);
    });

    renderMathInElement(document.body, {
      delimiters: [
        {left: '$$', right: '$$', display: true},
        {left: '$', right: '$', display: false},
        {left: '\\(', right: '\\)', display: false},
        {left: '\\[', right: '\\]', display: true}
      ],
      throwOnError : false
    });

    var headings = [];
    document.querySelectorAll('h1, h2, h3, h4').forEach(function(h, idx) {
      var id = h.id || 'heading-' + idx;
      h.id = id;
      headings.push({
        id: id,
        title: h.textContent.trim(),
        level: parseInt(h.tagName.substring(1))
      });
    });
    if (window.ToCInterface) {
      ToCInterface.sendHeadings(JSON.stringify(headings));
    }
    document.querySelectorAll('pre').forEach(function(preBlock) {
      preBlock.style.position = 'relative';
      var button = document.createElement('button');
      button.className = 'copy-button';
      button.textContent = 'Copy';
      button.addEventListener('click', function() {
        var codeText = preBlock.querySelector('code')?.textContent || preBlock.textContent;
        if (window.ToCInterface && window.ToCInterface.copyToClipboard) {
          window.ToCInterface.copyToClipboard(codeText);
          button.textContent = 'Copied!';
          setTimeout(function() { button.textContent = 'Copy'; }, 2000);
        } else {
          navigator.clipboard.writeText(codeText).then(function() {
            button.textContent = 'Copied!';
            setTimeout(function() { button.textContent = 'Copy'; }, 2000);
          }).catch(function() {
            button.textContent = 'Error';
          });
        }
      });
      preBlock.appendChild(button);
    });
  });

  function scrollToElement(id) {
    var el = document.getElementById(id);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }
</script>
<style>
  .copy-button {
    position: absolute;
    top: 8px;
    right: 8px;
    padding: 4px 8px;
    font-size: 11px;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
    color: var(--muted);
    background: var(--bg);
    border: 1px solid var(--border);
    border-radius: 4px;
    cursor: pointer;
    opacity: 0;
    transition: opacity 0.2s, background 0.2s;
  }
  pre:hover .copy-button {
    opacity: 1;
  }
  .copy-button:hover {
    background: var(--surface);
    color: var(--text);
  }
  :root {
    color-scheme: $colorScheme;
    --bg: $bg;
    --text: $text;
    --muted: $muted;
    --secondary: $secondary;
    --border: $border;
    --surface: $surface;
    --inline: ${border}30;
    --link: $accent;
  }
  * { box-sizing: border-box; }
  html, body { margin: 0; padding: 0; background: var(--bg); color: var(--text); }
  body {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
    font-size: 14px;
    line-height: 1.45;
    overflow-wrap: anywhere;
  }
  #readme { background: var(--bg); }
  .markdown-body {
    max-width: 100%;
    padding: 48px 20px 24px 20px;
    color: var(--text);
    background: var(--bg);
  }
  .markdown-heading {
    position: relative;
    margin-top: 16px;
    margin-bottom: 8px;
  }
  .markdown-heading:first-child { margin-top: 0; }
  .markdown-heading .anchor {
    position: absolute;
    left: -31px;
    top: 0.34em;
    width: 20px;
    height: 20px;
    opacity: .5;
  }
  .markdown-heading .anchor svg { fill: var(--muted); width: 20px; height: 20px; }
  h1, h2, h3, h4, h5, h6 {
    margin: 0;
    color: var(--text);
    font-weight: 700;
    line-height: 1.16;
    letter-spacing: 0;
  }
  h1 {
    font-size: 28px;
    padding-bottom: 10px;
    border-bottom: 1px solid var(--border);
  }
  h2 {
    font-size: 20px;
    padding-bottom: 8px;
    border-bottom: 1px solid var(--border);
  }
  h3 { font-size: 18px; padding-bottom: 6px; border-bottom: 1px solid rgba(48, 54, 61, .75); }
  h4 { font-size: 16px; }
  h5 { font-size: 14px; }
  h6 { font-size: 13px; color: var(--muted); }
  p, ul, ol, blockquote, pre, table { margin-top: 0; margin-bottom: 10px; }
  a { color: var(--link); text-decoration: underline; text-underline-offset: 2px; font-weight: 650; }
  strong { font-weight: 700; }
  ul, ol { padding-left: 29px; }
  li { margin: 4px 0; padding-left: 4px; }
  li::marker { color: var(--text); }
  code {
    padding: .18em .42em;
    border-radius: 6px;
    background: var(--inline);
    color: var(--text);
    font-family: ui-monospace, SFMono-Regular, SFMono, Consolas, "Liberation Mono", Menlo, monospace;
    font-size: .86em;
  }
  pre {
    overflow-x: auto;
    padding: 16px;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: var(--surface);
  }
  pre code { padding: 0; background: transparent; white-space: pre; }
  blockquote {
    padding: 0 1em;
    color: var(--secondary);
    border-left: .25em solid var(--border);
  }
  hr { height: 1px; border: 0; background: var(--border); margin: 28px 0; }
  img, video { max-width: 100%; height: auto; }
  p img { margin: 5px 3px 7px 0; vertical-align: middle; }
  table {
    display: block;
    width: 100%;
    max-width: 100%;
    overflow-x: auto;
    border-spacing: 0;
    border-collapse: collapse;
  }
  th, td {
    padding: 8px 11px;
    border: 1px solid var(--border);
    vertical-align: top;
  }
  th { font-weight: 700; background: var(--surface); }
  tr:nth-child(2n) { background: var(--surface); }
  markdown-accessiblity-table { display: block; max-width: 100%; overflow-x: auto; }
  kbd {
    display: inline-block;
    padding: 3px 5px;
    border: 1px solid var(--border);
    border-radius: 6px;
    background: var(--inline);
    color: var(--text);
    font: 12px ui-monospace, SFMono-Regular, SFMono, Consolas, monospace;
  }
  @media (max-width: 430px) {
    .markdown-body { padding: 36px 16px 20px 16px; }
    h1 { font-size: 26px; }
    h2 { font-size: 20px; }
    h3 { font-size: 18px; }
  }
</style>
</head>
<body>
$readmeHtml
</body>
</html>
""".trimIndent()

@Composable
private fun ReadmeRepositoryFooter(
    repo: GHRepo,
    releases: List<GHRelease>,
    contributors: List<GHContributor>,
    languages: Map<String, Long>,
) {
    Spacer(Modifier.height(20.dp))
    if (releases.isNotEmpty()) {
        ReadmeFooterDivider()
        ReadmeReleasesSummary(releases)
    }
    if (contributors.isNotEmpty()) {
        ReadmeFooterDivider()
        ReadmeContributorsSummary(contributors)
    }
    if (languages.isNotEmpty()) {
        ReadmeFooterDivider()
        ReadmeLanguagesSummary(languages)
    } else if (repo.language.isNotBlank()) {
        ReadmeFooterDivider()
        ReadmeSingleLanguageSummary(repo.language)
    }
}

@Composable
private fun ReadmeFooterDivider() {
    Box(Modifier.fillMaxWidth().padding(vertical = 18.dp).height(1.dp).background(AiModuleTheme.colors.border.copy(alpha = 0.55f)))
}

@Composable
private fun ReadmeFooterHeading(title: String, count: Int? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = AiModuleTheme.colors.textPrimary, lineHeight = 30.sp)
        if (count != null) {
            Text(
                formatGitHubNumber(count),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AiModuleTheme.colors.textSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(AiModuleTheme.colors.surface)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun ReadmeReleasesSummary(releases: List<GHRelease>) {
    val latest = releases.firstOrNull()
    ReadmeFooterHeading("Releases", releases.size)
    Spacer(Modifier.height(14.dp))
    if (latest != null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Label, null, Modifier.size(22.dp), tint = GitHubSuccessGreen)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(latest.tag.ifBlank { latest.name.ifBlank { "latest" } }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AiModuleTheme.colors.textPrimary)
                    ReadmeStatusPill("Latest", GitHubSuccessGreen)
                }
                latest.createdAt.takeIf { it.isNotBlank() }?.let {
                    Text(it.take(10), fontSize = 13.sp, color = AiModuleTheme.colors.textMuted)
                }
            }
        }
        if (releases.size > 1) {
            Spacer(Modifier.height(13.dp))
            Text("+ ${formatGitHubNumber(releases.size - 1)} releases", fontSize = 16.sp, color = AiModuleTheme.colors.accent)
        }
    }
}

@Composable
private fun ReadmeContributorsSummary(contributors: List<GHContributor>) {
    ReadmeFooterHeading("Contributors", contributors.size)
    Spacer(Modifier.height(14.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy((-6).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        contributors.take(14).forEach { contributor ->
            AsyncImage(
                contributor.avatarUrl,
                contributor.login,
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, AiModuleTheme.colors.background, CircleShape),
            )
        }
    }
    if (contributors.size > 14) {
        Spacer(Modifier.height(12.dp))
        Text("+ ${formatGitHubNumber(contributors.size - 14)} contributors", fontSize = 16.sp, color = AiModuleTheme.colors.accent)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadmeLanguagesSummary(languages: Map<String, Long>) {
    val total = languages.values.sum().toFloat().coerceAtLeast(1f)
    ReadmeFooterHeading(Strings.ghLanguages)
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(999.dp))) {
        languages.forEach { (language, bytes) ->
            Box(Modifier.weight(bytes / total).fillMaxHeight().background(langColor(language)))
        }
    }
    Spacer(Modifier.height(12.dp))
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        languages.forEach { (language, bytes) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(11.dp).clip(CircleShape).background(langColor(language)))
                Text(language, fontSize = 15.sp, color = AiModuleTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                Text("${"%.1f".format(bytes / total * 100)}%", fontSize = 15.sp, color = AiModuleTheme.colors.textMuted)
            }
        }
    }
}

@Composable
private fun ReadmeSingleLanguageSummary(language: String) {
    ReadmeFooterHeading(Strings.ghLanguages)
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(999.dp))) {
        Box(Modifier.fillMaxSize().background(langColor(language)))
    }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(11.dp).clip(CircleShape).background(langColor(language)))
        Text(language, fontSize = 15.sp, color = AiModuleTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
        Text("100.0%", fontSize = 15.sp, color = AiModuleTheme.colors.textMuted)
    }
}

@Composable
private fun ReadmeStatusPill(text: String, color: Color) {
    Text(
        text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.8f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
internal fun GitHubMarkdownDocument(
    markdown: String,
    repo: GHRepo,
    readmePath: String = "",
    modifier: Modifier = Modifier,
    maxBlocks: Int? = null,
    onLinkClick: (String) -> Unit = {},
) {
    val imageLoader = rememberReadmeImageLoader(LocalContext.current)
    var blocks by remember(markdown, repo.owner, repo.name, repo.defaultBranch, readmePath) { mutableStateOf<List<ReadmeRenderBlock>?>(null) }
    LaunchedEffect(markdown, repo.owner, repo.name, repo.defaultBranch, readmePath) {
        blocks = withContext(Dispatchers.Default) { parseReadmeBlocks(markdown, repo, readmePath) }
    }
    val safeBlocks = blocks
    if (safeBlocks == null) {
        Text("Rendering markdown...", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted)
    } else {
        Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            safeBlocks.let { if (maxBlocks == null) it else it.take(maxBlocks) }.forEach { block ->
                ReadmeBlockView(block, imageLoader, onLinkClick)
            }
        }
    }
}

@Composable
internal fun ReadmeBlockView(block: ReadmeRenderBlock, imageLoader: ImageLoader, onLinkClick: (String) -> Unit = {}) {
    when (block) {
        is ReadmeRenderBlock.Heading -> ReadmeHeading(block)
        is ReadmeRenderBlock.Paragraph -> ReadmeText(block.text, onLinkClick = onLinkClick)
        is ReadmeRenderBlock.Bullet -> ReadmeBullet(block.text, block.ordered, block.checked, block.level, block.marker, onLinkClick = onLinkClick)
        is ReadmeRenderBlock.Quote -> Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(4.dp).heightIn(min = 28.dp).background(AiModuleTheme.colors.border, RoundedCornerShape(GitHubControlRadius)))
            ReadmeText(block.text, modifier = Modifier.weight(1f), onLinkClick = onLinkClick)
        }
        is ReadmeRenderBlock.Rule -> Box(Modifier.fillMaxWidth().padding(vertical = 18.dp).height(1.dp).background(AiModuleTheme.colors.border.copy(alpha = 0.62f)))
        is ReadmeRenderBlock.Image -> ReadmeImage(block, imageLoader)
        is ReadmeRenderBlock.ImageRow -> ReadmeImageRow(block.images, imageLoader)
        is ReadmeRenderBlock.Code -> ReadmeCodeBlock(block)
        is ReadmeRenderBlock.Table -> ReadmeTable(block.rows, onLinkClick = onLinkClick)
        is ReadmeRenderBlock.Link -> ReadmeLinkCard(block.text, block.url, onLinkClick = onLinkClick)
    }
}

@Composable
private fun ReadmeHeading(block: ReadmeRenderBlock.Heading) {
    val colors = AiModuleTheme.colors
    val topPadding = when (block.level) {
        1 -> 10.dp
        2 -> 14.dp
        else -> 10.dp
    }
    Column(Modifier.fillMaxWidth().padding(top = topPadding, bottom = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (block.anchor.isNotBlank()) {
                Icon(
                    Icons.Outlined.Link,
                    null,
                    Modifier.size(if (block.level <= 2) 20.dp else 16.dp),
                    tint = colors.textMuted.copy(alpha = 0.78f),
                )
            }
            val size = when (block.level) {
                1 -> 24.sp
                2 -> 20.sp
                3 -> 17.sp
                else -> 15.sp
            }
            Text(
                readmeInlineAnnotated(block.text),
                fontSize = size,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                lineHeight = (size.value + 6).sp,
                modifier = Modifier.weight(1f)
            )
        }
        if (block.level <= 3) {
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.62f)))
        }
    }
}

@Composable
private fun ReadmeErrorCard(message: String, raw: String, repo: GHRepo, onViewRaw: (() -> Unit)? = null, onRetry: (() -> Unit)? = null) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(GitHubControlRadius)).background(AiModuleTheme.colors.background).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(message, fontSize = 14.sp, color = AiModuleTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
        Text("README rendering was stopped to keep the app responsive.", fontSize = 12.sp, color = AiModuleTheme.colors.textMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            if (onRetry != null) Chip(Icons.Rounded.Refresh, "Retry", AiModuleTheme.colors.accent, onRetry)
            if (raw.isNotBlank() && onViewRaw != null) Chip(Icons.Rounded.Article, "View raw", AiModuleTheme.colors.accent, onViewRaw)
            Chip(Icons.Rounded.OpenInNew, "Open in browser", AiModuleTheme.colors.accent) { context.openReadmeUrl(readmeBrowserUrl(repo)) }
        }
    }
}

@Composable
private fun ReadmeRawBlock(markdown: String) {
    val preview = remember(markdown) { markdown.lineSequence().take(500).joinToString("\n") { it.take(README_MAX_LINE_CHARS) } }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(AiModuleTheme.colors.surface)
            .border(1.dp, AiModuleTheme.colors.border.copy(alpha = 0.55f), RoundedCornerShape(GitHubControlRadius))
            .padding(12.dp)
    ) {
        Text(
            preview,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = AiModuleTheme.colors.textPrimary,
            lineHeight = 17.sp
        )
        if (markdown.lines().size > 500) {
            Spacer(Modifier.height(8.dp))
            Text("Raw preview truncated to first 500 lines.", fontSize = 11.sp, color = AiModuleTheme.colors.textMuted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadmeText(text: String, modifier: Modifier = Modifier, onLinkClick: (String) -> Unit = {}) {
    val segments = remember(text) { readmeInlineSegments(text) }
    if (segments.size == 1 && segments.first() is ReadmeInlineSegment.Text) {
        val annotated = readmeInlineAnnotated(text)
        androidx.compose.material3.Text(
            text = annotated,
            modifier = modifier,
            style = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, lineHeight = 19.sp)
        )
    } else {
        FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            segments.forEach { segment ->
                when (segment) {
                    is ReadmeInlineSegment.Code -> {
                        Text(
                            segment.text,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = AiModuleTheme.colors.textPrimary,
                            lineHeight = 17.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(GitHubControlRadius))
                                .background(AiModuleTheme.colors.surface)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    is ReadmeInlineSegment.Text -> {
                        val annotated = readmeInlineAnnotated(segment.text)
                        androidx.compose.material3.Text(
                            text = annotated,
                            style = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = AiModuleTheme.colors.textPrimary, lineHeight = 19.sp)
                        )
                    }
                }
            }
        }
    }
}

private sealed class ReadmeInlineSegment {
    data class Text(val text: String) : ReadmeInlineSegment()
    data class Code(val text: String) : ReadmeInlineSegment()
}

private fun readmeInlineSegments(text: String): List<ReadmeInlineSegment> {
    val segments = mutableListOf<ReadmeInlineSegment>()
    var index = 0
    while (index < text.length) {
        val start = text.indexOf('`', index)
        if (start < 0) {
            text.substring(index).takeIf { it.isNotBlank() }?.let { segments += ReadmeInlineSegment.Text(it) }
            break
        }
        text.substring(index, start).takeIf { it.isNotBlank() }?.let { segments += ReadmeInlineSegment.Text(it) }
        val end = text.indexOf('`', start + 1)
        if (end < 0) {
            text.substring(start).takeIf { it.isNotBlank() }?.let { segments += ReadmeInlineSegment.Text(it) }
            break
        }
        text.substring(start + 1, end).takeIf { it.isNotBlank() }?.let { segments += ReadmeInlineSegment.Code(it) }
        index = end + 1
    }
    return segments.ifEmpty { listOf(ReadmeInlineSegment.Text(text)) }
}

@Composable
private fun ReadmeBullet(text: String, ordered: Boolean = false, checked: Boolean? = null, level: Int = 0, markerText: String? = null, onLinkClick: (String) -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = (level * 18).dp, top = 3.dp, bottom = 3.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        val marker = markerText ?: when (checked) {
            true -> "✓"
            false -> "□"
            null -> if (ordered) "1." else "•"
        }
        Text(marker, fontSize = 13.sp, color = if (checked == true) GitHubSuccessGreen else AiModuleTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 18.dp))
        ReadmeText(text, modifier = Modifier.weight(1f), onLinkClick = onLinkClick)
    }
}

@Composable
internal fun rememberReadmeImageLoader(context: Context): ImageLoader {
    val appContext = context.applicationContext
    return remember(appContext) {
        ImageLoader.Builder(appContext)
            .okHttpClient(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", README_IMAGE_USER_AGENT)
                                .build()
                        )
                    }
                    .build()
            )
            .components { add(SvgDecoder.Factory()) }
            .build()
    }
}

@Composable
private fun ReadmeImage(block: ReadmeRenderBlock.Image, imageLoader: ImageLoader) {
    if (block.inline) {
        InlineReadmeImage(block, imageLoader)
        return
    }
    val context = LocalContext.current
    val placeholder = ColorPainter(AiModuleTheme.colors.background)
    var failed by remember(block.url) { mutableStateOf(false) }
    var loaded by remember(block.url) { mutableStateOf(false) }
    val animatedGif = remember(block.url) { block.url.substringBefore('?').endsWith(".gif", ignoreCase = true) }
    LaunchedEffect(block.url) {
        failed = false
        loaded = false
        delay(README_IMAGE_TIMEOUT_MS)
        if (!loaded && !animatedGif) {
            Log.w(README_RENDER_TAG, "image timeout")
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (animatedGif) {
            ReadmeLinkCard(block.alt.ifBlank { "Animated image skipped" }, block.url)
        } else if (failed) {
            ReadmeImageUnavailable(block.alt)
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(block.url)
                    .size(2048, 2048)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .crossfade(false)
                    .build(),
                contentDescription = block.alt,
                imageLoader = imageLoader,
                placeholder = placeholder,
                error = placeholder,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(block.aspectRatio.coerceIn(0.5f, 3f))
                    .heightIn(min = 200.dp, max = 360.dp)
                    .clip(RoundedCornerShape(GitHubControlRadius)),
                onSuccess = { loaded = true },
                onError = {
                    loaded = true
                    failed = true
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReadmeImageRow(images: List<ReadmeRenderBlock.Image>, imageLoader: ImageLoader) {
    FlowRow(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        images.forEach { InlineReadmeImage(it.copy(inline = true), imageLoader) }
    }
}

@Composable
private fun InlineReadmeImage(block: ReadmeRenderBlock.Image, imageLoader: ImageLoader) {
    val context = LocalContext.current
    var failed by remember(block.url) { mutableStateOf(false) }
    val height = (block.heightHintDp ?: 24).coerceIn(16, 56).dp
    if (failed) {
        ReadmeImageUnavailable(block.alt)
        return
    }
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(block.url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build(),
        contentDescription = block.alt,
        imageLoader = imageLoader,
        contentScale = ContentScale.Fit,
        modifier = Modifier.height(height).widthIn(min = 16.dp, max = 220.dp).clip(RoundedCornerShape(GitHubControlRadius)),
        onError = { failed = true }
    )
}

@Composable
private fun ReadmeImageUnavailable(alt: String) {
    val colors = AiModuleTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Rounded.BrokenImage, null, Modifier.size(16.dp), tint = colors.textMuted)
        Text(
            alt.ifBlank { "image unavailable" },
            fontSize = 12.sp,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ReadmeCodeBlock(block: ReadmeRenderBlock.Code) {
    val context = LocalContext.current
    val colors = AiModuleTheme.colors
    var expanded by remember(block.code) { mutableStateOf(false) }
    val lines = remember(block.code, expanded) {
        val allLines = block.code.lines()
        if (expanded || allLines.size <= README_MAX_CODE_LINES) allLines else allLines.take(120)
    }
    val totalLines = remember(block.code) { block.code.lines().size }
    val language = block.language.ifBlank { "text" }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(GitHubControlRadius))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(GitHubControlRadius))
    ) {
        Column(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 52.dp, top = 10.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            lines.forEach { line ->
                Text(
                    highlightLine(line.take(README_MAX_LINE_CHARS).ifEmpty { " " }, language, colors),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = colors.textPrimary,
                    lineHeight = 18.sp,
                    softWrap = false,
                )
            }
            if (!expanded && totalLines > README_MAX_CODE_LINES) {
                Spacer(Modifier.height(8.dp))
                GitHubTerminalButton("expand large code block", onClick = { expanded = true }, color = colors.accent)
            }
        }
        IconButton(
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(34.dp),
            onClick = {
                val clip = android.content.ClipData.newPlainText("readme-code", block.code)
                (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)
                Toast.makeText(context, Strings.done, Toast.LENGTH_SHORT).show()
            }
        ) {
            Icon(Icons.Rounded.ContentCopy, null, Modifier.size(20.dp), tint = colors.textMuted)
        }
    }
}

@Composable
private fun ReadmeTable(rows: List<List<String>>, onLinkClick: (String) -> Unit = {}) {
    if (rows.isEmpty()) return
    var expanded by remember(rows) { mutableStateOf(false) }
    val colors = AiModuleTheme.colors
    val visibleRows = if (expanded || rows.size <= README_MAX_TABLE_ROWS) rows else rows.take(README_MAX_TABLE_ROWS)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .horizontalScroll(rememberScrollState())
            .clip(RoundedCornerShape(GitHubControlRadius))
            .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(GitHubControlRadius))
    ) {
        Column {
            visibleRows.forEachIndexed { rowIndex, row ->
                Row(Modifier.background(if (rowIndex == 0) colors.surfaceElevated else colors.surface)) {
                    row.forEachIndexed { cellIndex, cell ->
                        Box(
                            Modifier
                                .widthIn(min = 122.dp, max = 260.dp)
                                .then(if (cellIndex != 0) Modifier.border(0.5.dp, colors.border.copy(alpha = 0.32f)) else Modifier)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                readmeInlineAnnotated(cell),
                                fontSize = 14.sp,
                                color = colors.textPrimary,
                                lineHeight = 20.sp,
                                fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                if (rowIndex != visibleRows.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.border.copy(alpha = 0.4f)))
            }
            if (!expanded && rows.size > README_MAX_TABLE_ROWS) {
                GitHubTerminalButton(
                    "expand large table (${rows.size - README_MAX_TABLE_ROWS} rows hidden)",
                    onClick = { expanded = true },
                    color = colors.accent,
                )
            }
        }
    }
}

@Composable
private fun ReadmeLinkCard(text: String, url: String, onLinkClick: (String) -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(GitHubControlRadius))
            .clickable { onLinkClick(url) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("→", fontSize = 14.sp, color = AiModuleTheme.colors.textPrimary)
        Text(
            text.ifBlank { url },
            fontSize = 13.sp,
            color = AiModuleTheme.colors.accent,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun readmeInlineAnnotated(text: String): AnnotatedString {
    val colors = AiModuleTheme.colors
    return androidx.compose.ui.text.buildAnnotatedString {
        var i = 0
        val clean = stripReadmeHtml(text)
        while (i < clean.length) {
            when {
                clean.startsWith("**", i) -> {
                    val end = clean.indexOf("**", i + 2)
                    if (end > i) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary))
                        append(clean.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else append(clean[i++])
                }
                clean[i] == '*' -> {
                    val end = clean.indexOf('*', i + 1)
                    if (end > i) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontStyle = FontStyle.Italic, color = colors.textPrimary))
                        append(clean.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else append(clean[i++])
                }
                clean[i] == '`' -> {
                    val end = clean.indexOf('`', i + 1)
                    if (end > i) {
                        pushStyle(androidx.compose.ui.text.SpanStyle(fontFamily = FontFamily.Monospace, background = colors.background, color = colors.textPrimary))
                        append(clean.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else append(clean[i++])
                }
                clean[i] == '[' -> {
                    val closeBracket = clean.indexOf(']', i)
                    val openParen = if (closeBracket > 0 && closeBracket + 1 < clean.length && clean[closeBracket + 1] == '(') closeBracket + 1 else -1
                    val closeParen = if (openParen > 0) clean.indexOf(')', openParen) else -1
                    if (closeParen > 0) {
                        val label = clean.substring(i + 1, closeBracket)
                        val url = clean.substring(openParen + 1, closeParen).substringBefore(' ').trim()
                        val start = length
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = colors.accent, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline))
                        append(label)
                        pop()
                        addLink(LinkAnnotation.Url(url), start, length)
                        i = closeParen + 1
                    } else append(clean[i++])
                }
                README_PLAIN_URL_REGEX.find(clean, i)?.range?.first == i -> {
                    val rawUrl = README_PLAIN_URL_REGEX.find(clean, i)!!.value
                    val url = rawUrl.trimEnd('.', ',', ';', ':')
                    val start = length
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = colors.accent, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline))
                    append(url)
                    pop()
                    addLink(LinkAnnotation.Url(url), start, length)
                    val trailing = rawUrl.drop(url.length)
                    if (trailing.isNotEmpty()) append(trailing)
                    i += rawUrl.length
                }
                else -> append(clean[i++])
            }
        }
    }
}

internal sealed class ReadmeRenderBlock {
    abstract val stableId: String
    data class Heading(val level: Int, val text: String, val anchor: String = "", override val stableId: String = "") : ReadmeRenderBlock()
    data class Paragraph(val text: String, override val stableId: String = "") : ReadmeRenderBlock()
    data class Bullet(val text: String, val ordered: Boolean, val checked: Boolean? = null, val level: Int = 0, val marker: String? = null, override val stableId: String = "") : ReadmeRenderBlock()
    data class Quote(val text: String, override val stableId: String = "") : ReadmeRenderBlock()
    data class Rule(override val stableId: String = "") : ReadmeRenderBlock()
    data class Image(val url: String, val alt: String, val aspectRatio: Float = README_DEFAULT_IMAGE_ASPECT_RATIO, val inline: Boolean = false, val heightHintDp: Int? = null, override val stableId: String = "") : ReadmeRenderBlock()
    data class ImageRow(val images: List<Image>, override val stableId: String = "") : ReadmeRenderBlock()
    data class Code(val language: String, val code: String, override val stableId: String = "") : ReadmeRenderBlock()
    data class Table(val rows: List<List<String>>, override val stableId: String = "") : ReadmeRenderBlock()
    data class Link(val text: String, val url: String, override val stableId: String = "") : ReadmeRenderBlock()
}

internal suspend fun parseReadmeBlocks(markdown: String, repo: GHRepo, readmePath: String = ""): List<ReadmeRenderBlock> {
    val blocks = mutableListOf<ReadmeRenderBlock>()
    val lines = markdown.replace("\r\n", "\n").lines()
    var i = 0
    var guard = 0
    while (i < lines.size) {
        if (guard++ > lines.size + 1_000) {
            throw IllegalStateException("README parser made no forward progress near line $i")
        }
        if (guard % 100 == 0) yield()
        val rawLine = lines[i]
        val line = rawLine.trim()
        when {
            line.isBlank() -> i++
            readmeIsSetextHeading(lines, i) -> {
                val cleanText = stripReadmeHtml(line)
                val level = if (lines[i + 1].trim().firstOrNull() == '=') 1 else 2
                blocks += ReadmeRenderBlock.Heading(level, cleanText, readmeHeadingAnchor(cleanText))
                i += 2
            }
            line.startsWith("```") || line.startsWith("~~~") -> {
                val fence = line.take(3)
                val language = line.drop(3).trim().take(24)
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith(fence)) {
                    code += lines[i].trimEnd().take(README_MAX_LINE_CHARS)
                    i++
                }
                if (i < lines.size) i++
                blocks += ReadmeRenderBlock.Code(language, code.joinToString("\n"))
            }
            readmeMarkdownImageLinkBlocks(line, repo, readmePath).isNotEmpty() -> {
                readmeTextWithoutImages(line).takeIf { it.isNotBlank() }?.let { blocks += ReadmeRenderBlock.Paragraph(stripReadmeHtml(it)) }
                blocks += readmeMarkdownImageLinkBlocks(line, repo, readmePath)
                i++
            }
            readmeMarkdownImages(line, repo, readmePath).isNotEmpty() -> {
                val images = readmeMarkdownImages(line, repo, readmePath)
                readmeTextWithoutImages(line).takeIf { it.isNotBlank() }?.let { blocks += ReadmeRenderBlock.Paragraph(stripReadmeHtml(it)) }
                blocks += readmeImageBlocks(images)
                i++
            }
            readmeHtmlImageLinkBlocks(line, repo, readmePath).isNotEmpty() -> {
                readmeTextWithoutImages(line).takeIf { it.isNotBlank() }?.let { blocks += ReadmeRenderBlock.Paragraph(stripReadmeHtml(it)) }
                blocks += readmeHtmlImageLinkBlocks(line, repo, readmePath)
                i++
            }
            readmeHtmlImages(line, repo, readmePath).isNotEmpty() -> {
                val images = readmeHtmlImages(line, repo, readmePath)
                readmeTextWithoutImages(line).takeIf { it.isNotBlank() }?.let { blocks += ReadmeRenderBlock.Paragraph(stripReadmeHtml(it)) }
                blocks += readmeImageBlocks(images)
                i++
            }
            readmeHtmlAnchorImageBlock(lines, i, repo, readmePath) != null -> {
                val anchorBlock = readmeHtmlAnchorImageBlock(lines, i, repo, readmePath)!!
                blocks += anchorBlock.blocks
                i = anchorBlock.nextIndex
            }
            line.equals("</a>", ignoreCase = true) || line.startsWith("<br", ignoreCase = true) -> i++
            line.startsWith("<a ", ignoreCase = true) -> {
                readmeHtmlLink(line)?.let { blocks += ReadmeRenderBlock.Link(it.first, it.second) }
                i++
            }
            line.startsWith("#") -> {
                val level = line.takeWhile { it == '#' }.length.coerceIn(1, 4)
                val text = line.drop(level).trim()
                if (text.isNotBlank()) {
                    val cleanText = stripReadmeHtml(text)
                    blocks += ReadmeRenderBlock.Heading(level, cleanText, readmeHeadingAnchor(cleanText))
                }
                i++
            }
            line.startsWith(">") -> {
                blocks += ReadmeRenderBlock.Quote(stripReadmeHtml(line.removePrefix(">").trim()))
                i++
            }
            line == "---" || line == "***" || line == "___" -> {
                blocks += ReadmeRenderBlock.Rule()
                i++
            }
            readmeLooksLikeTable(lines, i) -> {
                val rows = mutableListOf<List<String>>()
                rows += readmeTableCells(lines[i])
                i += 2
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    rows += readmeTableCells(lines[i])
                    i++
                }
                blocks += ReadmeRenderBlock.Table(rows)
            }
            line.startsWith("- [ ]", ignoreCase = true) || line.startsWith("* [ ]", ignoreCase = true) -> {
                blocks += ReadmeRenderBlock.Bullet(stripReadmeHtml(line.drop(5).trim()), ordered = false, checked = false, level = readmeListLevel(rawLine))
                i++
            }
            line.startsWith("- [x]", ignoreCase = true) || line.startsWith("* [x]", ignoreCase = true) -> {
                blocks += ReadmeRenderBlock.Bullet(stripReadmeHtml(line.drop(5).trim()), ordered = false, checked = true, level = readmeListLevel(rawLine))
                i++
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                blocks += ReadmeRenderBlock.Bullet(stripReadmeHtml(line.drop(2).trim()), ordered = false, level = readmeListLevel(rawLine))
                i++
            }
            Regex("^\\d+[.)]\\s+.*").matches(line) -> {
                val orderedMarker = Regex("^(\\d+[.)])\\s+").find(line)?.groupValues?.getOrNull(1)
                blocks += ReadmeRenderBlock.Bullet(stripReadmeHtml(line.replaceFirst(Regex("^\\d+[.)]\\s+"), "")), ordered = true, level = readmeListLevel(rawLine), marker = orderedMarker)
                i++
            }
            readmeStandaloneMarkdownLink(line) != null -> {
                val link = readmeStandaloneMarkdownLink(line)!!
                blocks += ReadmeRenderBlock.Link(link.first, readmeResolveUrl(link.second, repo, readmePath))
                i++
            }
            else -> {
                val paragraph = mutableListOf<String>()
                val paragraphStart = i
                while (i < lines.size) {
                    val current = lines[i].trim()
                    if (current.isBlank() || current.startsWith("#") || current.startsWith("```") || current.startsWith("~~~") ||
                        current.startsWith("- ") || current.startsWith("* ") || current.startsWith(">") || current.startsWith("|") ||
                        readmeMarkdownImages(current, repo, readmePath).isNotEmpty() || readmeHtmlImages(current, repo, readmePath).isNotEmpty()
                    ) break
                    paragraph += stripReadmeHtml(current)
                    i++
                }
                if (i == paragraphStart) {
                    stripReadmeHtml(line).takeIf { it.isNotBlank() }?.let { blocks += ReadmeRenderBlock.Paragraph(it) }
                    i++
                } else {
                    paragraph.joinToString(" ").trim().takeIf { it.isNotBlank() }?.let { blocks += ReadmeRenderBlock.Paragraph(it) }
                }
            }
        }
    }
    return blocks.withStableReadmeIds()
}

private fun List<ReadmeRenderBlock>.withStableReadmeIds(): List<ReadmeRenderBlock> =
    mapIndexed { index, block ->
        val type = block.readmeBlockType()
        val stableId = "${index}_${type}_${block.readmeKeyContent().hashCode()}"
        when (block) {
            is ReadmeRenderBlock.Heading -> block.copy(stableId = stableId)
            is ReadmeRenderBlock.Paragraph -> block.copy(stableId = stableId)
            is ReadmeRenderBlock.Bullet -> block.copy(stableId = stableId)
            is ReadmeRenderBlock.Quote -> block.copy(stableId = stableId)
            is ReadmeRenderBlock.Rule -> block.copy(stableId = stableId)
            is ReadmeRenderBlock.Image -> block.copy(stableId = stableId)
            is ReadmeRenderBlock.ImageRow -> block.copy(stableId = stableId)
            is ReadmeRenderBlock.Code -> block.copy(stableId = stableId)
            is ReadmeRenderBlock.Table -> block.copy(stableId = stableId)
            is ReadmeRenderBlock.Link -> block.copy(stableId = stableId)
        }
    }

private fun ReadmeRenderBlock.readmeBlockType(): String = when (this) {
    is ReadmeRenderBlock.Heading -> "heading"
    is ReadmeRenderBlock.Paragraph -> "paragraph"
    is ReadmeRenderBlock.Bullet -> "bullet"
    is ReadmeRenderBlock.Quote -> "quote"
    is ReadmeRenderBlock.Rule -> "rule"
    is ReadmeRenderBlock.Image -> "image"
    is ReadmeRenderBlock.ImageRow -> "image_row"
    is ReadmeRenderBlock.Code -> "code"
    is ReadmeRenderBlock.Table -> "table"
    is ReadmeRenderBlock.Link -> "link"
}

private fun ReadmeRenderBlock.readmeKeyContent(): String = when (this) {
    is ReadmeRenderBlock.Heading -> "$level|$text|$anchor"
    is ReadmeRenderBlock.Paragraph -> text
    is ReadmeRenderBlock.Bullet -> "$ordered|$checked|$level|$marker|$text"
    is ReadmeRenderBlock.Quote -> text
    is ReadmeRenderBlock.Rule -> "rule"
    is ReadmeRenderBlock.Image -> "$url|$alt|$aspectRatio|$inline|$heightHintDp"
    is ReadmeRenderBlock.ImageRow -> images.joinToString("|") { it.readmeKeyContent() }
    is ReadmeRenderBlock.Code -> "$language|$code"
    is ReadmeRenderBlock.Table -> rows.joinToString("|") { it.joinToString("\u001F") }
    is ReadmeRenderBlock.Link -> "$text|$url"
}

private fun readmeMarkdownImages(line: String, repo: GHRepo, readmePath: String): List<ReadmeRenderBlock.Image> {
    val regex = Regex("!\\[([^]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)")
    val matches = regex.findAll(line).toList()
    val hasInlineText = readmeTextWithoutImages(line).isNotBlank()
    return matches.mapNotNull { match ->
        val raw = match.groupValues.getOrNull(2).orEmpty()
        val alt = match.groupValues.getOrNull(1).orEmpty()
        val url = readmeResolveUrl(raw, repo, readmePath)
        if (raw.isBlank()) null else ReadmeRenderBlock.Image(
            url = url,
            alt = alt,
            inline = hasInlineText || matches.size > 1 || readmeIsInlineImage(url, alt, null),
            heightHintDp = if (readmeIsBadgeImage(url, alt)) 20 else null
        )
    }.toList()
}

private fun readmeMarkdownImageLinkBlocks(line: String, repo: GHRepo, readmePath: String): List<ReadmeRenderBlock> {
    val regex = Regex("\\[!\\[([^]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)")
    val matches = regex.findAll(line).toList()
    if (matches.isEmpty()) return emptyList()

    val blocks = mutableListOf<ReadmeRenderBlock>()
    val inlineImages = mutableListOf<ReadmeRenderBlock.Image>()
    fun flushInlineImages() {
        if (inlineImages.isNotEmpty()) {
            blocks += readmeImageBlocks(inlineImages.toList())
            inlineImages.clear()
        }
    }

    matches.forEach { match ->
        val alt = match.groupValues.getOrNull(1).orEmpty()
        val rawImageUrl = match.groupValues.getOrNull(2).orEmpty()
        val rawTargetUrl = match.groupValues.getOrNull(3).orEmpty()
        val imageUrl = readmeResolveUrl(rawImageUrl, repo, readmePath)
        if (rawImageUrl.isNotBlank() && readmeIsBadgeImage(imageUrl, alt)) {
            inlineImages += ReadmeRenderBlock.Image(
                url = imageUrl,
                alt = alt,
                inline = true,
                heightHintDp = 20
            )
        } else {
            flushInlineImages()
            if (rawImageUrl.isNotBlank()) {
                blocks += ReadmeRenderBlock.Image(
                    url = imageUrl,
                    alt = alt.ifBlank { rawTargetUrl },
                    inline = false
                )
            }
        }
    }
    flushInlineImages()
    return blocks
}

private fun readmeHtmlImageLinkBlocks(line: String, repo: GHRepo, readmePath: String): List<ReadmeRenderBlock> {
    val regex = Regex("<a\\b[^>]*href=[\"'][^\"']+[\"'][^>]*>.*?<img\\b[^>]*>.*?</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val matches = regex.findAll(line).toList()
    if (matches.isEmpty()) return emptyList()

    val blocks = mutableListOf<ReadmeRenderBlock>()
    val inlineImages = mutableListOf<ReadmeRenderBlock.Image>()
    fun flushInlineImages() {
        if (inlineImages.isNotEmpty()) {
            blocks += readmeImageBlocks(inlineImages.toList())
            inlineImages.clear()
        }
    }

    matches.forEach { match ->
        val anchorTag = match.value
        val imageTag = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE).find(anchorTag)?.value.orEmpty()
        val href = readmeHtmlAttr(anchorTag, "href")
        val rawImageUrl = readmeHtmlAttr(imageTag, "src")
        val alt = readmeHtmlAttr(imageTag, "alt")
        val heightHint = readmeHtmlImageHeightDp(imageTag)
        val imageUrl = readmeResolveUrl(rawImageUrl, repo, readmePath)
        if (rawImageUrl.isNotBlank() && readmeIsBadgeImage(imageUrl, alt)) {
            inlineImages += ReadmeRenderBlock.Image(
                url = imageUrl,
                alt = alt,
                inline = true,
                heightHintDp = heightHint ?: if (readmeIsBadgeImage(imageUrl, alt)) 20 else null
            )
        } else {
            flushInlineImages()
            if (rawImageUrl.isNotBlank()) {
                blocks += ReadmeRenderBlock.Image(
                    url = imageUrl,
                    alt = alt.ifBlank { href },
                    aspectRatio = readmeHtmlImageAspectRatio(imageTag),
                    inline = readmeIsInlineImage(imageUrl, alt, heightHint),
                    heightHintDp = heightHint
                )
            }
        }
    }
    flushInlineImages()
    return blocks
}

private data class ReadmeHtmlAnchorImageParseResult(
    val blocks: List<ReadmeRenderBlock>,
    val nextIndex: Int,
)

private fun readmeHtmlAnchorImageBlock(
    lines: List<String>,
    index: Int,
    repo: GHRepo,
    readmePath: String,
): ReadmeHtmlAnchorImageParseResult? {
    val first = lines.getOrNull(index)?.trim().orEmpty()
    if (!first.startsWith("<a ", ignoreCase = true)) return null

    val collected = mutableListOf<String>()
    var i = index
    while (i < lines.size && collected.size < 12) {
        val current = lines[i].trim()
        collected += current
        i++
        if (current.contains("</a>", ignoreCase = true)) break
        if (i > index && current.startsWith("<a ", ignoreCase = true)) break
    }

    val joined = collected.joinToString(" ")
    if (!joined.contains("<img", ignoreCase = true)) return null
    val blocks = readmeHtmlImageLinkBlocks(joined, repo, readmePath)
    return if (blocks.isNotEmpty()) {
        ReadmeHtmlAnchorImageParseResult(blocks, i)
    } else {
        null
    }
}

private fun readmeHtmlImages(line: String, repo: GHRepo, readmePath: String): List<ReadmeRenderBlock.Image> {
    val regex = Regex("<img\\b[^>]*src=[\"']([^\"']+)[\"'][^>]*>", RegexOption.IGNORE_CASE)
    val matches = regex.findAll(line).toList()
    val hasInlineText = readmeTextWithoutImages(line).isNotBlank()
    return matches.mapNotNull { match ->
        val raw = match.groupValues.getOrNull(1).orEmpty()
        val tag = match.value
        val alt = readmeHtmlAttr(tag, "alt")
        val heightHint = readmeHtmlImageHeightDp(tag)
        val url = readmeResolveUrl(raw, repo, readmePath)
        if (raw.isBlank()) null else ReadmeRenderBlock.Image(
            url = url,
            alt = alt,
            aspectRatio = readmeHtmlImageAspectRatio(tag),
            inline = hasInlineText || matches.size > 1 || readmeIsInlineImage(url, alt, heightHint),
            heightHintDp = heightHint
        )
    }.toList()
}

private fun readmeImageBlocks(images: List<ReadmeRenderBlock.Image>): List<ReadmeRenderBlock> =
    if (images.isEmpty()) {
        emptyList()
    } else if (images.size > 1 || images.all { it.inline }) {
        listOf(ReadmeRenderBlock.ImageRow(images.map { it.copy(inline = true) }))
    } else {
        images
    }

private fun readmeTextWithoutImages(line: String): String =
    line.replace(Regex("\\[!\\[[^]]*]\\([^)]+\\)]\\([^)]+\\)"), "")
        .replace(Regex("!\\[([^]]*)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)"), "")
        .replace(Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?a\\b[^>]*>", RegexOption.IGNORE_CASE), "")
        .trim()

private fun readmeIsInlineImage(url: String, alt: String, heightHintDp: Int?): Boolean =
    (heightHintDp != null && heightHintDp < 64) || readmeIsBadgeImage(url, alt)

private fun readmeIsBadgeImage(url: String, alt: String): Boolean {
    val lowerUrl = url.lowercase()
    val lowerAlt = alt.lowercase()
    return "shields.io" in lowerUrl ||
        "img.shields.io" in lowerUrl ||
        "badge.fury.io" in lowerUrl ||
        "badgen.net" in lowerUrl ||
        "crowdin" in lowerUrl ||
        "localized.svg" in lowerUrl ||
        "badge" in lowerUrl ||
        "badge" in lowerAlt ||
        "crowdin" in lowerAlt ||
        "localized" in lowerAlt ||
        "license" in lowerAlt
}

private fun readmeHtmlImageAspectRatio(tag: String): Float {
    val width = readmeHtmlAttr(tag, "width").filter { it.isDigit() }.toFloatOrNull()
    val height = readmeHtmlAttr(tag, "height").filter { it.isDigit() }.toFloatOrNull()
    return if (width != null && height != null && width > 0f && height > 0f) {
        width / height
    } else {
        README_DEFAULT_IMAGE_ASPECT_RATIO
    }
}

private fun readmeHtmlImageHeightDp(tag: String): Int? =
    readmeHtmlAttr(tag, "height").filter { it.isDigit() }.toIntOrNull()

private fun readmeListLevel(rawLine: String): Int =
    (rawLine.takeWhile { it == ' ' }.length / 2).coerceIn(0, 6)

private fun readmeIsSetextHeading(lines: List<String>, index: Int): Boolean {
    val current = lines.getOrNull(index)?.trim().orEmpty()
    val next = lines.getOrNull(index + 1)?.trim().orEmpty()
    if (current.isBlank() || current.startsWith("#") || current.startsWith("|")) return false
    if (current.startsWith("<") || current.startsWith("- ") || current.startsWith("* ")) return false
    return next.length >= 3 && next.all { it == '=' || it == '-' }
}

private fun readmeHeadingAnchor(text: String): String =
    text.lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .trim()
        .replace(Regex("\\s+"), "-")

private fun readmeHtmlLink(line: String): Pair<String, String>? {
    val href = readmeHtmlAttr(line, "href")
    if (href.isBlank()) return null
    val label = Regex(">([^<]+)<", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)?.trim().orEmpty()
    return label.ifBlank { href } to href
}

private fun readmeHtmlAttr(line: String, attr: String): String =
    Regex("$attr=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1).orEmpty()

private fun readmeStandaloneMarkdownLink(line: String): Pair<String, String>? {
    val match = Regex("^\\[([^]]+)]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)$").find(line) ?: return null
    return match.groupValues[1] to match.groupValues[2]
}

private fun readmeLooksLikeTable(lines: List<String>, index: Int): Boolean {
    if (index + 1 >= lines.size) return false
    val header = lines[index].trim()
    val divider = lines[index + 1].trim()
    return header.startsWith("|") && header.endsWith("|") && divider.matches(Regex("^\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)+\\|?$"))
}

private fun readmeTableCells(line: String): List<String> =
    line.trim().trim('|').split('|').map { stripReadmeHtml(it.trim()) }

private fun stripReadmeHtml(text: String): String =
    text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</?p[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?div[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?span[^>]*>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("</?strong[^>]*>", RegexOption.IGNORE_CASE), "**")
        .replace(Regex("</?b[^>]*>", RegexOption.IGNORE_CASE), "**")
        .replace(Regex("</?em[^>]*>", RegexOption.IGNORE_CASE), "*")
        .replace(Regex("</?i[^>]*>", RegexOption.IGNORE_CASE), "*")
        .replace(Regex("<a\\b[^>]*href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", RegexOption.IGNORE_CASE), "[$2]($1)")
        .replace(Regex("<[^>]+>"), "")
        .trim()
        .let { readmeSafeText(it) }

private fun readmeSafeText(text: String): String =
    text.lineSequence().joinToString("\n") { line ->
        if (line.length <= README_MAX_LINE_CHARS) line else line.take(README_MAX_LINE_CHARS) + "…"
    }

private fun readmeResolveUrl(raw: String, repo: GHRepo, readmePath: String = ""): String {
    val url = readmeCleanImageUrl(raw)
    if (url.startsWith("http://") || url.startsWith("https://")) return url
    if (url.startsWith("//")) return "https:$url"
    if (url.startsWith("#")) return url
    val (path, suffix) = readmeSplitPathSuffix(url)
    val baseDir = readmePath.substringBeforeLast('/', missingDelimiterValue = "")
    val joinedPath = if (path.startsWith("/")) {
        path.trimStart('/')
    } else {
        listOf(baseDir, path.removePrefix("./")).filter { it.isNotBlank() }.joinToString("/")
    }
    val normalizedPath = readmeNormalizePath(joinedPath)
    return "https://raw.githubusercontent.com/${repo.owner}/${repo.name}/${repo.defaultBranch.ifBlank { "main" }}/$normalizedPath$suffix"
}

private fun readmeCleanImageUrl(raw: String): String =
    raw.trim()
        .removeSurrounding("<", ">")
        .replace("&amp;", "&")
        .replace("&#38;", "&")

private fun readmeSplitPathSuffix(url: String): Pair<String, String> {
    val suffixStart = listOf(url.indexOf('?'), url.indexOf('#')).filter { it >= 0 }.minOrNull() ?: return url to ""
    return url.take(suffixStart) to url.drop(suffixStart)
}

private fun readmeNormalizePath(path: String): String {
    val segments = mutableListOf<String>()
    path.split('/').forEach { segment ->
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isNotEmpty()) segments.removeAt(segments.lastIndex)
            else -> segments += segment
        }
    }
    return segments.joinToString("/")
}

internal fun resolveReadmeLink(url: String, repo: GHRepo): String? {
    val uri = try { Uri.parse(url) } catch (_: Exception) { return null }
    // raw.githubusercontent.com/owner/repo/branch/path
    if (uri.host == "raw.githubusercontent.com") {
        val seg = uri.pathSegments
        if (seg.size >= 4 && seg[0] == repo.owner && seg[1] == repo.name) {
            return seg.drop(3).joinToString("/")
        }
        if (seg.size >= 4) return seg.drop(3).joinToString("/")
    }
    if (uri.host == "github.com" || uri.host == "www.github.com") {
        val seg = uri.pathSegments
        if (seg.size >= 2 && seg[0] == repo.owner && seg[1] == repo.name) {
            if (seg.size >= 4 && seg[2] == "blob") return seg.drop(4).joinToString("/")
            if (seg.size >= 4 && seg[2] == "tree") return seg.drop(4).joinToString("/")
            // github.com/owner/repo/filename.ext (no /blob/) — treat as repo file
            if (seg.size == 3 && seg[2].contains(".")) return seg[2]
        }
        if (seg.size >= 4 && seg[2] == "blob") return seg.drop(4).joinToString("/")
    }
    if (uri.scheme.isNullOrBlank() && !url.startsWith("/") && !url.startsWith("#")) {
        val clean = url.removePrefix("./").removePrefix("../")
        if (clean.contains(".") || clean.contains("/")) return clean
    }
    return null
}

private fun readmeBrowserUrl(repo: GHRepo): String =
    "https://github.com/${repo.owner}/${repo.name}#readme"

internal fun android.content.Context.openReadmeUrl(url: String) {
    if (url.isBlank() || url.startsWith("#")) return
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: Exception) {
        Toast.makeText(this, Strings.error, Toast.LENGTH_SHORT).show()
    }
}
