package com.example.dndmap.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.MotionEventCompat
import androidx.core.view.children
import com.example.dndmap.MapFragment
import com.example.dndmap.R
import com.example.dndmap.data.Pos
import com.example.dndmap.ui.MapViewModel
import kotlin.math.ceil

class MapGridView @JvmOverloads constructor(context: Context,
                  attrs: AttributeSet? = null,
                  defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr),
GestureDetector.OnGestureListener {

    var mapSize = Pos(0, 0)
    var squareSize: Int

    private val paint = Paint()
    private val paintFog = Paint()
    val fog = arrayListOf<Pos>() //TODO to viewModel?
    lateinit var viewCanvas: Canvas

    var scaleFactor = 1F
    lateinit var mDetector: GestureDetectorCompat

    init {
        setWillNotDraw(false)
        context.obtainStyledAttributes(
            attrs,
            R.styleable.MapGridView,
            0, 0).apply {
            try {
                squareSize = getInteger(R.styleable.MapGridView_squareSize, 300)
            } finally {
                recycle()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mDetector = GestureDetectorCompat(context, this)
        paint.style = Paint.Style.STROKE
        paint.color = Color.GRAY
        paint.isAntiAlias = true
        paint.strokeWidth = 10F

        paintFog.style = Paint.Style.FILL
        paintFog.color = Color.BLACK
        paintFog.isAntiAlias = true
        paintFog.strokeWidth = 0F
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        viewCanvas = canvas!!
        mapSize.x = ceil(width / squareSize.toDouble()).toInt()
        mapSize.y = ceil(height / squareSize.toFloat()).toInt()


        for (x in 0 .. mapSize.x) {
            val posX = squareSize * x.toFloat()
            val posY = mapSize.y * squareSize.toFloat()
            canvas.drawLine(posX, 0F, posX, posY, paint)
        }
        for (y in 0 .. mapSize.y) {
            val posY = squareSize * y.toFloat()
            val posX = mapSize.x * squareSize.toFloat()
            canvas.drawLine(0F, posY, posX, posY, paint)
        }
        for (pos in fog) {
            drawFog(pos)
        }
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (parent as View).width
        val height = (parent as View).height
        setMeasuredDimension(width, height)
        for (child in children) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (child in children) {
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        }
    }

//    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
//
//        override fun onScale(sDetector: ScaleGestureDetector): Boolean {
//            scaleFactor *= sDetector.scaleFactor
//            // Don't let the object get too small or too large.
//            scaleFactor = Math.max(0.1f, Math.min(scaleFactor, 5.0f))
//
//            invalidate()
//            return true
//        }
//    }
//    private val mScaleDetector = ScaleGestureDetector(context, scaleListener)

    override fun onTouchEvent(event: MotionEvent?): Boolean {
////        Log.d("TAG", "$event")
//        if (event?.action == MotionEvent.ACTION_CANCEL) {
//            val move = MotionEvent.obtain(event)
//            move.action = MotionEvent.ACTION_MOVE
//            super.onTouchEvent(move)
//        }

//        return if ((event != null) && mScaleDetector.onTouchEvent(event)) {
//            true
//        } else {
//            super.onTouchEvent(event)
//        }

        return super.onTouchEvent(event)

    }

    override fun onDown(event: MotionEvent): Boolean {
        return false
    }

    override fun onLongPress(event: MotionEvent) {
        return
    }

    override fun onShowPress(event: MotionEvent) {
        return
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        return true
    }

    override fun onScroll(event0: MotionEvent,
                          event1: MotionEvent,
                          distanceX: Float,
                          distanceY: Float): Boolean {
        return false
    }

    override fun onFling(event0: MotionEvent,
                         event1: MotionEvent,
                         distanceX: Float,
                         distanceY: Float): Boolean{
        return false
    }

    private fun drawFog(pos: Pos) {
        val left = pos.x * squareSize + paint.strokeWidth / 2
        val top = pos.y * squareSize + paint.strokeWidth / 2
        val right = (pos.x + 1) * squareSize - paint.strokeWidth / 2
        val bottom = (pos.y + 1) * squareSize - paint.strokeWidth / 2
        viewCanvas.drawRect(left, top, right, bottom, paintFog)
    }

    fun addFog(x: Float, y: Float, scaleFactor: Float) {
        //TODO: check if exists. make a set?
        fog.add(coords2pos(x, y, scaleFactor))
    }

    fun coords2pos(x: Float, y: Float, scaleFactor: Float) : Pos {
        this.scaleFactor = scaleFactor

//        Log.d("TAG", "$squareSize, $scaleFactor, x: ${x /( squareSize / scaleFactor)}")
        return Pos((x / (squareSize / scaleFactor)).toInt(), (y / (squareSize / scaleFactor)).toInt())
    }
}