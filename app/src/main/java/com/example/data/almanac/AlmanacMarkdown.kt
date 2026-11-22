package com.example.data.almanac

/**
 * Tiny front-matter + Markdown helpers (no YAML/Markdown libraries).
 * Supports: `---` key: value front matter, `#`/`##`/`###`, paragraphs, `- ` lists,
 * `**bold**`, and `[label](url)` links.
 */
object AlmanacMarkdown {

    data class ParsedDocument(
        val meta: Map<String, String>,
        val body: String
    )

    sealed class Block {
        data class Heading(val level: Int, val text: String) : Block()
        data class Paragraph(val text: String) : Block()
        data class Bullet(val text: String) : Block()
    }

    fun parseDocument(raw: String): ParsedDocument {
        val trimmed = raw.trimStart('\uFEFF').trim()
        if (!trimmed.startsWith("---")) {
            return ParsedDocument(emptyMap(), trimmed)
        }
        val afterOpen = trimmed.removePrefix("---").trimStart('\r', '\n')
        val end = afterOpen.indexOf("\n---")
        if (end < 0) return ParsedDocument(emptyMap(), trimmed)
        val fm = afterOpen.substring(0, end).trim()
        val body = afterOpen.substring(end + 4).trimStart('\r', '\n')
        val meta = linkedMapOf<String, String>()
        for (line in fm.lines()) {
            val idx = line.indexOf(':')
            if (idx <= 0) continue
            val key = line.substring(0, idx).trim()
            val value = line.substring(idx + 1).trim().removeSurrounding("\"")
            if (key.isNotEmpty()) meta[key] = value
        }
        return ParsedDocument(meta, body.trim())
    }

    fun parseBlocks(body: String): List<Block> {
        val out = mutableListOf<Block>()
        val para = StringBuilder()
        fun flushPara() {
            val t = para.toString().trim()
            if (t.isNotEmpty()) out += Block.Paragraph(t)
            para.clear()
        }
        for (rawLine in body.lines()) {
            val line = rawLine.trimEnd()
            when {
                line.isBlank() -> flushPara()
                line.startsWith("### ") -> {
                    flushPara()
                    out += Block.Heading(3, line.removePrefix("### ").trim())
                }
                line.startsWith("## ") -> {
                    flushPara()
                    out += Block.Heading(2, line.removePrefix("## ").trim())
                }
                line.startsWith("# ") -> {
                    flushPara()
                    out += Block.Heading(1, line.removePrefix("# ").trim())
                }
                line.startsWith("- ") || line.startsWith("* ") -> {
                    flushPara()
                    out += Block.Bullet(line.drop(2).trim())
                }
                else -> {
                    if (para.isNotEmpty()) para.append(' ')
                    para.append(line.trim())
                }
            }
        }
        flushPara()
        return out
    }

    fun csvList(raw: String?): List<String> =
        raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun toArticle(meta: Map<String, String>, body: String, assetPath: String): AlmanacArticle? {
        val id = meta["id"]?.trim().orEmpty()
        val title = meta["title"]?.trim().orEmpty()
        if (id.isEmpty() || title.isEmpty()) return null
        return AlmanacArticle(
            id = id,
            type = AlmanacArticleType.fromId(meta["type"].orEmpty()),
            title = title,
            summary = meta["summary"].orEmpty().trim(),
            relatedSongs = csvList(meta["relatedSongs"]),
            relatedShows = csvList(meta["relatedShows"]),
            sources = csvList(meta["sources"]),
            bodyMarkdown = body,
            assetPath = assetPath
        )
    }
}
