package com.example.dndmap.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.media.Image
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.children
import com.example.dndmap.MapFragment
import com.example.dndmap.R
import com.example.dndmap.data.Pos
import com.example.dndmap.data.Character
import com.squareup.picasso.Picasso
import kotlin.math.ceil

class MapGridView @JvmOverloads constructor(context: Context,
                  attrs: AttributeSet? = null,
                  defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr)
{
    companion object {
        const val TAG = "GRID_VIEW"
    }
    private var mapSize = Pos(0, 0)
    private var squareSize: Int

    private val paint = Paint()
    private val paintFog = Paint()
    private val fog = arrayListOf<Pos>() //TODO to viewModel?
    private val characters = mutableListOf<Character>()
    private lateinit var viewCanvas: Canvas

    // istg it's the only way
    private var downX = 0F
    private var downY = 0F

    enum class FogMode {
        DRAWING,
        ERASING,
        NONE,
        INTERACTING
    }
    var fogMode = FogMode.NONE

    lateinit var mDetector: GestureDetectorCompat
    private lateinit var mParent : View

    inner class MyGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onDown(event: MotionEvent): Boolean {
//            Log.d(TAG, "onDown")
            downX = event.x
            downY = event.y

            if (fogMode == FogMode.INTERACTING) {
                fogMode = if (checkFog(event.x, event.y)) FogMode.ERASING
                    else FogMode.DRAWING
            }
            return true
        }

        override fun onScroll(
            event1: MotionEvent,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
//            Log.d(TAG, "onScroll")
//            Log.d(TAG, "$event1, $event2")
//            if (fogMode == FogMode.NONE) {
//                return false
//            }
            if (fogMode == FogMode.NONE) {
                (parent as MapView).wholeMode = true
                return false
            }
            if (fogMode == FogMode.DRAWING) {
                addFog(event2.x, event2.y)
            }
            else if (fogMode == FogMode.ERASING)
                removeFog(event2.x, event2.y)
            invalidate()
            return true
        }

        override fun onLongPress(event: MotionEvent) {
//            Log.d(TAG, "$event")
            fogMode = if (checkFog(event.x, event.y)) FogMode.ERASING
                else FogMode.DRAWING
        }
    }
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

    private fun getPaddings() : FloatArray {
        val leftPadding = pivotX * scaleX - pivotX
        val rightPadding = width * scaleX - (width + leftPadding) // Scaled width - width - border
        val topPadding = pivotY * scaleY - pivotY
        val bottomPadding = height * scaleY - (height + topPadding)
        return floatArrayOf(leftPadding, topPadding, rightPadding, bottomPadding)
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
        mDetector = GestureDetectorCompat(context, MyGestureListener())
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
//        Log.d(TAG, "$event")

        if (event.action == MotionEvent.ACTION_MOVE && (fogMode != FogMode.NONE)) {
            val cancel = MotionEvent.obtain(event)
            cancel.action = MotionEvent.ACTION_CANCEL
            mDetector.onTouchEvent(cancel)
        }
        if (event.action == MotionEvent.ACTION_UP && event.pointerCount == 1) {
            fogMode = FogMode.NONE
        }
        return if (mDetector.onTouchEvent(event))
            true
        else
            super.onTouchEvent(event)
//        mDetector.onTouchEvent(event)
//        return super.onTouchEvent(event)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

//        Log.d(TAG, "$fog")
    }

    override fun onDrawForeground(canvas: Canvas?) {
        // TODO: drawgrid
        if (canvas == null) {
            return
        }
        super.onDrawForeground(canvas)
        viewCanvas = canvas
        for (pos in fog) {
            drawFog(pos)
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
        pivotX = 0F
        pivotY = 0F
        translationY = -(height / 2).toFloat()
        translationX = -(width / 2).toFloat()
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
        if (!checkFog(x, y))
            fog.add(coords2pos(x, y))
    }

    fun removeFog(x: Float, y: Float) {
        fog.remove(coords2pos(x, y))
    }

    private fun coords2pos(x: Float, y: Float) : Pos {
        val paddings = getPaddings()

        val posX = ((x - mParent.translationX) / squareSize ).toInt()
        val posY = ((y - mParent.translationY) / squareSize ).toInt()
        return Pos(posX, posY)
    }

    fun checkFog(x: Float, y: Float): Boolean {
        fog.forEach {
            val pos = coords2pos(x, y)
            if (it == pos ){
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
        translationX -= distanceX
        translationY -= distanceY
    }

    fun editCharacters(newCharacters : MutableList<Character>) {
        if (newCharacters == characters) {
            Log.d(TAG, "No new characters")
            return
        }
        // TODO: find removed changed and added characters
        val removed = characters.minus(newCharacters.toSet()).toMutableList()
        val added = newCharacters.minus(characters.toSet()).toMutableList()
        addCharacters(added)
        removeCharacters(removed)
    }

    private fun removeCharacters(removed: MutableList<Character>) {
        characters.removeAll(removed.toSet())
        for (view in children) {
            for (r in removed) {
                if (view.id == r.id)
                    removeView(view)
            }
        }
    }

    private fun addCharacters(added: MutableList<Character>) {
        for (a in added) {
            a.pos = coords2pos((-mParent.translationX + mParent.pivotX * mParent.scaleX - mParent.pivotX) / mParent.scaleX,
                (-mParent.translationY + mParent.pivotY * mParent.scaleY - mParent.pivotY) / mParent.scaleY)
            val characterView = ImageView(context)
            characterView.layoutParams = LayoutParams(squareSize, squareSize)
            characterView.id = a.id
            characterView.x = (a.pos.x * squareSize).toFloat()
            characterView.y = (a.pos.y * squareSize).toFloat()
            characterView.pivotX = width / 2F
            characterView.pivotY = height / 2F
            this.addView(characterView)
            Picasso.get().load(a.image).fit().centerCrop().into(characterView)
            invalidate()
            Log.d(TAG, "Added view")
        }
        characters.addAll(added)
        Log.d(TAG, "$characters")
    }
    fun getCharacterIndex (x: Float, y: Float) : Int {
        val pos = coords2pos(x, y)
        for (c in characters) {
            if (c.pos == pos) {
                return characters.indexOf(c)
            }
        }
        return -1
    }
    fun moveCharacter(idx: Int, distanceX: Float, distanceY: Float) {
        val newPos = coords2pos(x, y)
        characters[idx].pos = newPos
    }
}