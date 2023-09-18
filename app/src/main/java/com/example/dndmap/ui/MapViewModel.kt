package com.example.dndmap.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.example.dndmap.data.Character
import com.example.dndmap.data.Pos
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MapViewModel : ViewModel() {
    private val uri = Uri.parse("android.resource://com.example.dndmap/drawable/" +
            "mapexample.png")
    private val _uiState = MutableStateFlow(MapUiState(editGrid = false,
        scrollMode = ScrollMode.SCROLL,
        backgroundImage = Uri.parse("R.drawable.mapexample"),
        characters = ArrayList()))

    val uiState = _uiState.asStateFlow()

    fun toggleEditGrid() {
        _uiState.update{
            it.copy(editGrid = !it.editGrid)
        }
    }
    fun toggleScrollMode() {
        _uiState.update{
            val next: ScrollMode
            if (it.scrollMode == ScrollMode.SCROLL) {
                next = ScrollMode.FOG
            } else if (it.scrollMode == ScrollMode.FOG) {
                next = ScrollMode.CHARACTER
            } else {
                next = ScrollMode.SCROLL
            }
            it.copy(scrollMode = next)
        }
    }

    fun changeBackgroundImage(uri: Uri) {
        _uiState.update{
            it.copy(backgroundImage = uri)
        }
    }

    fun addCharacter(uri: Uri, name: String = "None") {
        _uiState.update{
            val newCharacter = Character(it.characters.lastIndex + 1, uri, name)
            val temp = it.characters.toMutableList()
            temp.add(newCharacter)
            it.copy(characters = temp)
        }
    }
}