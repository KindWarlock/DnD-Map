package com.example.dndmap.ui

import android.net.Uri
import androidx.lifecycle.ViewModel

data class MapUiState (
    var editGrid: Boolean,
    var backgroundImage: Uri
)