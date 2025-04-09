package com.msb.purrytify.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.msb.purrytify.media.MediaPlayerManager

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    val mediaPlayerManager: MediaPlayerManager
) : ViewModel()