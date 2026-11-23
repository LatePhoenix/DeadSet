package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.ui.navigation.NavCommand
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** Tab + almanac overlay + one-shot [NavCommand]s (6.2). */
class NavigationViewModel(
    private val ui: MutableStateFlow<UiState>
) : ViewModel() {

    private val navCommands = Channel<NavCommand>(Channel.BUFFERED)
    val navigation: Flow<NavCommand> = navCommands.receiveAsFlow()

    fun send(command: NavCommand) {
        navCommands.trySend(command)
    }

    fun setTab(tab: MainTab) {
        ui.update {
            it.copy(
                currentTab = tab,
                almanacOverlayArticleId = null,
                preferAlmanacArticles = tab == MainTab.ALMANAC && it.preferAlmanacArticles
            )
        }
    }

    fun openShowDetail(showIdOrDate: String) {
        val matchedShowId = ui.value.shows.firstOrNull {
            it.id.equals(showIdOrDate, ignoreCase = true) ||
                it.date.equals(showIdOrDate, ignoreCase = true) ||
                it.id.startsWith(showIdOrDate, ignoreCase = true)
        }?.id ?: showIdOrDate
        navCommands.trySend(NavCommand.ToShow(matchedShowId))
    }

    fun openAddShow() {
        navCommands.trySend(NavCommand.ToEditShow(null))
    }

    fun openEditShow(showId: String) {
        navCommands.trySend(NavCommand.ToEditShow(showId))
    }

    fun openAboutCredits() {
        navCommands.trySend(NavCommand.ToAbout)
    }

    fun openAlmanac() {
        ui.update {
            it.copy(currentTab = MainTab.ALMANAC, preferAlmanacArticles = true, almanacOverlayArticleId = null)
        }
        navCommands.trySend(NavCommand.ToAlmanacArticles)
    }

    fun openAlmanacArticle(articleId: String, asOverlay: Boolean = true) {
        if (asOverlay) {
            ui.update { it.copy(almanacOverlayArticleId = articleId) }
        } else {
            navCommands.trySend(NavCommand.ToAlmanacArticle(articleId))
        }
    }

    fun clearAlmanacArticlesPrefer() {
        ui.update { it.copy(preferAlmanacArticles = false) }
    }

    fun clearAlmanacOverlay() {
        ui.update { it.copy(almanacOverlayArticleId = null) }
    }

    fun navigateBack(): Boolean {
        if (ui.value.almanacOverlayArticleId != null) {
            clearAlmanacOverlay()
            return true
        }
        navCommands.trySend(NavCommand.Back)
        return true
    }
}
