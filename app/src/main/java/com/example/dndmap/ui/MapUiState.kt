package com.example.dndmap.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.dndmap.data.Character
enum class ScrollMode {
    SCROLL,
    FOG,
    CHARACTER
}
data class MapUiState (
    // TODO: editGrid to GridUiState (+ pivot, scale, translation)
    var scrollMode: ScrollMode,
    var editGrid: Boolean,
    var backgroundImage: Uri,
    var characters: MutableList<Character>
    // TODO: characters, fog, (pivot, scale, translation)
)