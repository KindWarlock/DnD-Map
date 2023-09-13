package com.example.dndmap.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MapViewModel : ViewModel() {
    val uri = Uri.parse("android.resource://com.example.dndmap/drawable/mapexample.png")
    private val _uiState = MutableStateFlow(MapUiState(editGrid = false, backgroundImage = Uri.parse("R.drawable.mapexample")))
    val uiState = _uiState.asStateFlow()

    fun toggleGridEdit() {
        _uiState.update{
            it.copy(editGrid = !_uiState.value.editGrid)
        }
    }

    fun changeBackgroundImage(uri: Uri) {
        _uiState.update{
            it.copy(backgroundImage = uri)
        }
    }
}