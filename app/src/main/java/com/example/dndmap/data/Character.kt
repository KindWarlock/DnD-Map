package com.example.dndmap.data

import android.media.Image
import android.net.Uri

data class Character (
    var id: Int,
    var image: Uri,
    var name: String = "None",
    var pos: Pos = Pos(0, 0)
//    var visible: Boolean
//    var maxHp: Int,
//    val isEnemy: Boolean
)
//) {
//    var currHp: Int = 0
//    init {
//        // TODO: maxHp > 0
//        currHp = maxHp
//    }
//}
