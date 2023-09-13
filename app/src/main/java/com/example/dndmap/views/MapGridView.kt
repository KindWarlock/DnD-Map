package com.example.dndmap.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.children
import com.example.dndmap.R
import com.example.dndmap.data.Pos
import kotlin.math.ceil

class MapGridView @JvmOverloads constructor(context: Context,
                  attrs: AttributeSet? = null,
                  defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {

    var mapSize = Pos(0, 0)
    var squareSize: Int

    private val paint = Paint()
    private val paintFog = Paint()
    val fog = arrayListOf<Pos>() //TODO to viewModel?
    lateinit var viewCanvas: Canvas

//    var mScaleFactor = 1F
    lateinit var mDetector: GestureDetectorCompat
    private lateinit var mParent : View

    var scaleMode = false
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
        mParent = (parent as View)
        paint.style = Paint.Style.STROKE
        paint.color = Color.GRAY
        paint.isAntiAlias = true
        paint.strokeWidth = 3F

        paintFog.style = Paint.Style.FILL
        paintFog.color = Color.BLACK
        paintFog.isAntiAlias = true
        paintFog.strokeWidth = 0F
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
//        viewCanvas = canvas!!
        // TODO: oh, really?
        if (canvas == null) {
            return
        }
        mapSize.x = ceil(width/ squareSize.toDouble()).toInt()
        mapSize.y = ceil(height/ squareSize.toFloat()).toInt()


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

        Log.d("TAG", "$fog")
    }

    override fun onDrawForeground(canvas: Canvas?) {
        super.onDrawForeground(canvas)
        viewCanvas = canvas!!
        for (pos in fog) {
            drawFog(pos)
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (parent as View).width * 4
        val height = (parent as View).height * 4
//        val width = 100
//        val height = 100
        setMeasuredDimension(width, height)
        for (child in children) {
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
//        scaleX = 0.7F
//        scaleY = 0.7F
        pivotX = 0F
        pivotY = 0F
        translationY = -(height / 2).toFloat()
        translationX = -(width / 2).toFloat()
//        translationX = 0F
//        translationY = 0F
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (child in children) {
            child.layout(0, 0, child.measuredWidth, child.measuredHeight)
        }
    }

    private fun drawFog(pos: Pos) {
        // TODO: перенести расчет ширины сетки в ondraw
        val left = pos.x * squareSize + paint.strokeWidth / 2
        val top = pos.y * squareSize + paint.strokeWidth / 2
        val right = (pos.x + 1) * squareSize - paint.strokeWidth / 2
        val bottom = (pos.y + 1) * squareSize - paint.strokeWidth / 2
        viewCanvas.drawRect(left, top, right, bottom, paintFog)
    }

    fun addFog(x: Float, y: Float) {
        //TODO: make a set?

        if (!checkFog(x, y))
            fog.add(coords2pos(x, y))
    }

    fun removeFog(x: Float, y: Float) {
        fog.remove(coords2pos(x, y))
    }

    private fun coords2pos(x: Float, y: Float) : Pos {
//        this.scaleFactor = scaleFactor

//        Log.d("TAG", "x: $x, " +
//                "y: $y, " +
//                "translationX: ${translationX}, " +
//                "translationY: ${translationY}, " +
//                "scaleX: $scaleX, " +
//                "squareSize: $squareSize")
        val paddings = getPaddings()
        val posX = ((x - translationX + paddings[0]) / scaleX / squareSize ).toInt()
        val posY = ((y - translationY + paddings[1]) / scaleY / squareSize ).toInt()
        Log.d("TAG", "posX: $posX, posY: $posY")
        return Pos(posX, posY)
    }

    fun checkFog(x: Float, y: Float): Boolean {
        fog.forEach {
            val pos = coords2pos(x, y)
            if (it.x == pos.x && it.y == pos.y){
                return true
            }
        }
        return false
    }
    fun scaleBegin(focusX: Float, focusY: Float) {
        // TODO: fix pivot on scaled parent
        val actualPivot = PointF(
            (focusX - mParent.translationX - translationX + pivotX * (scaleX - 1)) / scaleX,
            (focusY - mParent.translationY - translationY + pivotY * (scaleY - 1)) / scaleY,
        )
        translationX -= (pivotX - actualPivot.x) * (scaleX - 1)
        translationY -= (pivotY - actualPivot.y) * (scaleY - 1)
        pivotX = actualPivot.x
        pivotY = actualPivot.y
    }
    fun scale(scaleFactor: Float) {
        scaleY *= scaleFactor
        scaleX *= scaleFactor
        invalidate()
    }
    fun scroll(distanceX: Float, distanceY: Float) {
        val leftBorder = pivotX * scaleX - pivotX
        val rightBorder = width * scaleX - (width + leftBorder) // Scaled width - width - border
        val topBorder = pivotY * scaleY - pivotY
        val bottomBorder = height * scaleY - (height + topBorder)
        translationX -= distanceX
        translationY -= distanceY
//
//        if (translationX - leftBorder <= distanceX
//            && -translationX + distanceX <= width + rightBorder)
//            translationX -= distanceX
//        if (translationY - topBorder <= distanceY
//            && -translationY + distanceY <= height + bottomBorder)
//            translationY -= distanceY
    }
    private fun getPaddings() : FloatArray {
        val leftPadding = pivotX * scaleX - pivotX
        val rightPadding = width * scaleX - (width + leftPadding) // Scaled width - width - border
        val topPadding = pivotY * scaleY - pivotY
        val bottomPadding = height * scaleY - (height + topPadding)
        return floatArrayOf(leftPadding, topPadding, rightPadding, bottomPadding)
    }
}