package com.example.dndmap.data

import android.media.Image

data class Character (
    var name: String,
    var pos: Pos,
    //var image: Image,
    var maxHp: Int,
    val isEnemy: Boolean,
    //val description: CharacterDescription? = null
) {
    var currHp: Int = 0
    init {
        // TODO: maxHp > 0
        currHp = maxHp
    }
}
