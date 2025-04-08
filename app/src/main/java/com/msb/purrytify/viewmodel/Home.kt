package com.msb.purrytify.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.msb.purrytify.data.model.Song
import com.msb.purrytify.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {

    private val _recentlyPlayedSongs = mutableStateListOf<Song>()
    val recentlyPlayedSongs: List<Song> = _recentlyPlayedSongs

    private val _newSongs = mutableStateListOf<Song>()
    val newSongs: List<Song> = _newSongs

    init {
        refreshData()
    }

    fun refreshData() {
        // TODO: REFRESH DATA
    }
}