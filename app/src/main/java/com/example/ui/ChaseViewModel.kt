package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChaseSongEntity
import com.example.data.model.SetType
import com.example.data.repository.ShowRepository
import com.example.data.setlist.SetlistAnalytics
import com.example.data.setlist.predictNext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Chase list + live-tracker helpers (song names, append, predict) (6.2).
 * [predictNext] reads the shared catalog snapshot from [showsState].
 */
class ChaseViewModel(
    database: AppDatabase,
    private val repository: ShowRepository,
    private val showsState: () -> UiState
) : ViewModel() {

    private val chaseDao = database.chaseDao()

    val chaseSongs: StateFlow<List<ChaseSongEntity>> =
        chaseDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _songNames = MutableStateFlow<List<String>>(emptyList())
    val songNames: StateFlow<List<String>> = _songNames.asStateFlow()

    fun loadSongNames() {
        viewModelScope.launch { _songNames.value = repository.allSongNames() }
    }

    fun addChaseSong(name: String) {
        viewModelScope.launch {
            val song = repository.resolveSong(name) ?: return@launch
            chaseDao.add(
                ChaseSongEntity(song.songId, song.canonicalName, com.example.data.todayIsoDate())
            )
        }
    }

    fun removeChaseSong(songId: String) = viewModelScope.launch { chaseDao.remove(songId) }

    fun appendSongToShow(showId: String, name: String, setType: SetType) {
        viewModelScope.launch {
            repository.appendSong(showId, name, setType)
            loadSongNames()
        }
    }

    fun predictNext(showId: String, lastSong: String?, setType: SetType): List<String> {
        val state = showsState()
        return SetlistAnalytics.predictNext(
            lastSong, setType, state.shows, { state.songsByShow[it.id].orEmpty() },
            alreadyPlayed = state.songsByShow[showId].orEmpty().map { it.name }
        )
    }
}
