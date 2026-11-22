package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.credits.CatalogCredits
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.StealieCardDark
import com.example.ui.theme.StealieCyan
import com.example.ui.theme.StealieGold
import com.example.ui.theme.StealieMidnight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutCreditsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        runCatching { context.startActivity(intent) }
            .onFailure {
                Toast.makeText(context, "No browser available to open link", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "About & Credits",
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Catalog sources & licenses",
                            color = StealieGold,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("about_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StealieGold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = StealieMidnight,
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        containerColor = StealieMidnight,
        modifier = modifier.testTag("about_credits_screen")
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Text(
                    text = CatalogCredits.INTRO,
                    color = TextSecondaryDark,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            items(CatalogCredits.sources, key = { it.name }) { source ->
                CreditSourceCard(
                    source = source,
                    onOpenUrl = { url -> openUrl(url) }
                )
            }

            item {
                Text(
                    text = "gdshowsdb — MIT License",
                    color = StealieGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = StealieCardDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gdshowsdb_mit_license")
                ) {
                    Text(
                        text = CatalogCredits.GDSHOWSDB_MIT_LICENSE,
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditSourceCard(
    source: CatalogCredits.Source,
    onOpenUrl: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StealieCardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("credit_source_${source.name.lowercase()}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = source.name,
                    color = TextPrimaryDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                source.licenseNote?.let { note ->
                    Text(
                        text = note,
                        color = StealieCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = source.role,
                color = TextSecondaryDark,
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
            source.url?.let { url ->
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 0.5.dp)
                TextButton(
                    onClick = { onOpenUrl(url) },
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("credit_link_${source.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        tint = StealieGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = url.removePrefix("https://").removePrefix("http://"),
                        color = StealieGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
