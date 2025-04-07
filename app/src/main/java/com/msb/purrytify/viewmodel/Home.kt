package com.msb.purrytify.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.msb.purrytify.data.model.Song

class HomeViewModel : ViewModel() {

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