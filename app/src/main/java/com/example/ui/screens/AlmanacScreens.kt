package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.almanac.AlmanacArticle
import com.example.data.almanac.AlmanacArticleType
import com.example.data.almanac.AlmanacMarkdown
import com.example.data.almanac.SongStoryPanel
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.StealieCardDark
import com.example.ui.theme.StealieCyan
import com.example.ui.theme.StealieGold
import com.example.ui.theme.StealieMidnight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.TextTertiaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmanacListScreen(
    articles: List<AlmanacArticle>,
    onOpenArticle: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text("Almanac", color = StealieGold, fontWeight = FontWeight.Bold)
            },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("almanac_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StealieGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = StealieMidnight)
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("almanac_list")
        ) {
            item {
                Text(
                    text = "Bundled essays with cited sources. Nothing here is invented for color.",
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
            AlmanacArticleType.entries.forEach { type ->
                val group = articles.filter { it.type == type }
                if (group.isEmpty()) return@forEach
                item {
                    Text(
                        text = type.title.uppercase(),
                        color = StealieGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(group, key = { it.id }) { article ->
                    Surface(
                        color = StealieCardDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArticle(article.id) }
                            .testTag("almanac_item_${article.id}")
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(article.title, color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (article.summary.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(article.summary, color = TextSecondaryDark, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmanacArticleScreen(
    article: AlmanacArticle,
    onOpenShow: (String) -> Unit,
    onBack: () -> Unit,
    songStoryPanels: List<SongStoryPanel> = emptyList()
) {
    val blocks = remember(article.bodyMarkdown) { AlmanacMarkdown.parseBlocks(article.bodyMarkdown) }
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    article.type.title,
                    color = StealieGold,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StealieGold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = StealieMidnight)
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("almanac_article")
        ) {
            item {
                Text(article.title, color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp)
            }
            if (songStoryPanels.isNotEmpty()) {
                item {
                    Text(
                        "From your catalog + sourced credits",
                        color = StealieGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                items(songStoryPanels, key = { it.songName }) { panel ->
                    SongStoryPanelCard(panel)
                }
            }
            items(blocks.size) { i ->
                when (val block = blocks[i]) {
                    is AlmanacMarkdown.Block.Heading -> Text(
                        text = block.text,
                        color = StealieGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = when (block.level) {
                            1 -> 20.sp
                            2 -> 17.sp
                            else -> 15.sp
                        }
                    )
                    is AlmanacMarkdown.Block.Bullet -> Row {
                        Text("•  ", color = StealieCyan, fontSize = 14.sp)
                        InlineMarkdownText(block.text, Modifier.weight(1f))
                    }
                    is AlmanacMarkdown.Block.Paragraph -> InlineMarkdownText(block.text)
                }
            }
            if (article.relatedShows.isNotEmpty()) {
                item {
                    Text("Related shows", color = StealieGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                items(article.relatedShows) { showId ->
                    TextButton(onClick = { onOpenShow(showId) }) {
                        Text(showId, color = StealieCyan, fontSize = 13.sp)
                    }
                }
            }
            if (article.sources.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("Sources", color = StealieGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                items(article.sources) { url ->
                    TextButton(onClick = { uriHandler.openUri(url) }) {
                        Text(url, color = StealieCyan, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SongStoryPanelCard(panel: SongStoryPanel) {
    val uriHandler = LocalUriHandler.current
    Surface(
        color = StealieCardDark,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("song_story_panel_${panel.songName}")
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(panel.songName, color = TextPrimaryDark, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            panel.writers?.let {
                Text("Writers: $it", color = TextSecondaryDark, fontSize = 12.sp)
            }
            panel.debutDate?.let {
                Text("Debut (sourced): $it", color = TextSecondaryDark, fontSize = 12.sp)
            }
            Text(
                "Played ${panel.timesPlayedInCatalog}× in catalog" +
                    listOfNotNull(panel.firstCatalogDate, panel.lastCatalogDate)
                        .takeIf { it.isNotEmpty() }
                        ?.let { " (${it.joinToString(" → ")})" }
                        .orEmpty(),
                color = TextTertiaryDark,
                fontSize = 12.sp
            )
            if (panel.topInto.isNotEmpty()) {
                Text("Often from: ${panel.topInto.joinToString(", ")}", color = TextSecondaryDark, fontSize = 12.sp)
            }
            if (panel.topOutOf.isNotEmpty()) {
                Text("Often into: ${panel.topOutOf.joinToString(", ")}", color = TextSecondaryDark, fontSize = 12.sp)
            }
            panel.sourceUrl?.let { url ->
                TextButton(onClick = { uriHandler.openUri(url) }, contentPadding = PaddingValues(0.dp)) {
                    Text("Source", color = StealieCyan, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun InlineMarkdownText(text: String, modifier: Modifier = Modifier) {
    val annotated = remember(text) { annotateInlineMarkdown(text) }
    Text(
        text = annotated,
        modifier = modifier,
        color = TextSecondaryDark,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
}

private fun annotateInlineMarkdown(raw: String) = buildAnnotatedString {
    // Links: [label](url); bold: **text** — scanned left-to-right.
    var i = 0
    while (i < raw.length) {
        when {
            raw.startsWith("[", i) -> {
                val close = raw.indexOf("](", i)
                val end = if (close >= 0) raw.indexOf(')', close + 2) else -1
                if (close > i && end > close) {
                    val label = raw.substring(i + 1, close)
                    val url = raw.substring(close + 2, end)
                    withLink(
                        LinkAnnotation.Url(
                            url,
                            TextLinkStyles(
                                style = SpanStyle(
                                    color = StealieCyan,
                                    textDecoration = TextDecoration.Underline
                                )
                            )
                        )
                    ) {
                        append(label)
                    }
                    i = end + 1
                } else {
                    append(raw[i])
                    i++
                }
            }
            raw.startsWith("**", i) -> {
                val end = raw.indexOf("**", i + 2)
                if (end > i) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimaryDark)) {
                        append(raw.substring(i + 2, end))
                    }
                    i = end + 2
                } else {
                    append("**")
                    i += 2
                }
            }
            else -> {
                append(raw[i])
                i++
            }
        }
    }
}
