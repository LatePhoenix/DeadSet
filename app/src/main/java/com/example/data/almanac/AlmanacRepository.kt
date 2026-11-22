package com.example.data.almanac

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class AlmanacRepository(private val context: Context) {

    fun loadArticles(): List<AlmanacArticle> {
        val names = context.assets.list(ASSET_DIR)?.filter { it.endsWith(".md") }.orEmpty()
        return names.mapNotNull { name ->
            val path = "$ASSET_DIR/$name"
            runCatching {
                val text = context.assets.open(path).use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                }
                val doc = AlmanacMarkdown.parseDocument(text)
                AlmanacMarkdown.toArticle(doc.meta, doc.body, path)
            }.getOrNull()
        }.sortedWith(compareBy({ it.type.ordinal }, { it.title }))
    }

    fun loadArticle(id: String): AlmanacArticle? =
        loadArticles().firstOrNull { it.id == id }

    companion object {
        const val ASSET_DIR = "almanac"
    }
}
