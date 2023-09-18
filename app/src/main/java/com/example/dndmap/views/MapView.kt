package com.example.dndmap.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat
import kotlin.math.abs
import kotlin.math.min


class MapView @JvmOverloads constructor(context: Context,
                                        attrs: AttributeSet? = null,
                                        defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr)
{
    lateinit var mDetector: GestureDetectorCompat
    var wholeMode = false
    private var scaling = false
    private var prevScaleFactor = 0F

    inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(event: MotionEvent): Boolean {
            return true
        }

        override fun onScroll(
            event1: MotionEvent,
            event2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            wholeMode = true
            translationX += event2.x - event1.x
            translationY += event2.y - event1.y
            return true
        }
    }

    private val scaleGestureDetector by lazy {
        ScaleGestureDetector(context, object : ScaleGestureDetector.OnScaleGestureListener {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
//                    Log.d("TAG", "${abs(prevScaleFactor - detector.scaleFactor)}")
//                    prevScaleFactor = detector.scaleFactor
//
//                    scaleX *= detector.scaleFactor + abs(prevScaleFactor - detector.scaleFactor)
//                    scaleY *= detector.scaleFactor + abs(prevScaleFactor - detector.scaleFactor)
                    scaleX *= detector.scaleFactor
                    scaleX = 0.1f.coerceAtLeast(min(scaleX, 10.0f));
                    scaleY = scaleX

                    invalidate()
                    return true
                }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                Log.d("TAG", "Scaling")
                pivotX = detector.focusX
                pivotY = detector.focusY
                scaling = true

                return true
            }

            override fun onScaleEnd(p0: ScaleGestureDetector) {
                return
            }

        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mDetector = GestureDetectorCompat(context, MyGestureListener())
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
//        if ((getChildAt(1) as MapGridView).fogMode == MapGridView.FogMode.NONE && (event.action == MotionEvent.ACTION_MOVE)) {
//            Log.d("TAG", "is it event triggered")
////            return true
//        }
        if (wholeMode) {
            event.action = MotionEvent.ACTION_DOWN
            mDetector.onTouchEvent(event)
            return true
        }
//        return false
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
//        Log.d("TAG", "$event")
        if (event.action == MotionEvent.ACTION_UP) {
            wholeMode = false
            scaling = false
        }
        if (scaling || event.pointerCount > 1)
                return scaleGestureDetector.onTouchEvent(event)
        return if (mDetector.onTouchEvent(event))
            true
        else
            super.onTouchEvent(event)
    }

}