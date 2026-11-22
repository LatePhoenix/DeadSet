package com.example.ui.navigation

/**
 * Navigation-Compose route ids for DeadSet (step 6.1).
 * Tab roots stay in the bottom bar; detail routes hide it.
 */
object DeadSetRoutes {
    const val SHOWS = "shows"
    const val LIVE = "live"
    const val SEGUES = "segues"
    const val ALMANAC = "almanac"
    const val ME = "me"

    const val SHOW_DETAIL = "show/{showId}"
    const val EDIT_SHOW = "edit_show?showId={showId}"
    const val ABOUT = "about"
    const val ALMANAC_ARTICLE = "almanac/article/{articleId}"

    val tabRoutes = setOf(SHOWS, LIVE, SEGUES, ALMANAC, ME)

    fun showDetail(showId: String) = "show/${encode(showId)}"
    fun editShow(showId: String?) =
        if (showId == null) "edit_show?showId=" else "edit_show?showId=${encode(showId)}"
    fun almanacArticle(articleId: String) = "almanac/article/${encode(articleId)}"

    private fun encode(raw: String): String =
        java.net.URLEncoder.encode(raw, Charsets.UTF_8.name())

    fun decode(raw: String): String =
        java.net.URLDecoder.decode(raw, Charsets.UTF_8.name())
}

/** One-shot navigation requests from [com.example.ui.DeadSetViewModel]. */
sealed class NavCommand {
    data class ToShow(val showId: String) : NavCommand()
    data class ToEditShow(val showId: String?) : NavCommand()
    data object ToAbout : NavCommand()
    data class ToAlmanacArticle(val articleId: String) : NavCommand()
    /** Switch to Almanac tab articles pane (no stack push). */
    data object ToAlmanacArticles : NavCommand()
    /** Switch to Shows tab (e.g. after a segue filter). */
    data object ToShowsTab : NavCommand()
    data object Back : NavCommand()
}
