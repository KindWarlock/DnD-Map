package com.example.dndmap.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.HorizontalScrollView

class MapHScrollView @JvmOverloads constructor(context: Context,
                                               attrs: AttributeSet? = null,
                                               defStyleAttr: Int = 0) : HorizontalScrollView(context, attrs, defStyleAttr) {
    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        return false
    }

}