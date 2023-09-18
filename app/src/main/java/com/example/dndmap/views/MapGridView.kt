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
    val paintFog = Paint()
    private val fog = arrayListOf<Pos>() //TODO to viewModel?
    private val characters = mutableListOf<Character>()
    private lateinit var viewCanvas: Canvas

    var characterDrag = -1
    enum class FogMode {
        DRAWING,
        ERASING,
        NONE,
        INTERACTING
    }
    var fogMode = FogMode.DRAWING
    // TODO: scale as view method
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
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mapSize.x = ceil(width/ squareSize.toDouble()).toInt()
        mapSize.y = ceil(height/ squareSize.toFloat()).toInt()


        for (x in 0 .. mapSize.x) {
            val posX = squareSize * x.toFloat()
            val posY = mapSize.y * squareSize.toFloat()
            canvas?.drawLine(posX, 0F, posX, posY, paint)
        }
        for (y in 0 .. mapSize.y) {
            val posY = squareSize * y.toFloat()
            val posX = mapSize.x * squareSize.toFloat()
            canvas?.drawLine(0F, posY, posX, posY, paint)
        }
        Log.d(TAG, "$fog")
    }

    override fun onDrawForeground(canvas: Canvas?) {
        if (canvas == null) {
            return
        }
        super.onDrawForeground(canvas)
        viewCanvas = canvas
        for (pos in fog) {
            Log.d(TAG, "$fog")
            drawFog(pos)
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (parent as View).width * 4
        val height = (parent as View).height * 4
        setMeasuredDimension(width, height)
        for (child in children) {
            widthMeasureSpec
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
        }
        translationY = if (translationY == 0F) -(height / 2).toFloat() else translationY
        translationX = if (translationX == 0F) -(width / 2).toFloat() else translationX
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        for (child in children) {
            child.layout(0,
                0,
                child.measuredWidth,
                child.measuredHeight)
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
        val posX = ((x - translationX + paddings[0]) / scaleX / squareSize ).toInt()
        val posY = ((y - translationY + paddings[1]) / scaleY / squareSize ).toInt()
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
        val actualPivot = PointF(
            (focusX - translationX + pivotX * (scaleX - 1)) / scaleX,
            (focusY - translationY + pivotY * (scaleY - 1)) / scaleY,
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

    fun editCharacters(newCharacters : MutableList<Character>, x: Float = 0F, y: Float = 0F) {
        if (newCharacters == characters) {
            Log.d(TAG, "No new characters")
            return
        }
        val removed = characters.minus(newCharacters.toSet())
        val added = newCharacters.minus(characters.toSet())
        Log.d(TAG, "$added")
        if (added.isNotEmpty()) {
            addCharacter(added[0], x, y)
        }
        if (removed.isNotEmpty()) {
            removeCharacter(removed[0])
        }
    }

    private fun removeCharacter(removed: Character) {
        characters.remove(removed)
        for (view in children) {
            if (view.id == removed.id)
                removeView(view)
        }
    }

    private fun addCharacter(c: Character, x: Float, y: Float) {
        c.pos = coords2pos(x, y)

        val characterView = ImageView(context)
        characterView.layoutParams = LayoutParams(squareSize - paint.strokeWidth.toInt(),
            squareSize - paint.strokeWidth.toInt())
        characterView.id = c.id
        characterView.x = (c.pos.x * squareSize).toFloat() + (paint.strokeWidth / 2)
        characterView.y = (c.pos.y * squareSize).toFloat() + (paint.strokeWidth / 2)
        characterView.pivotX = squareSize / 2F
        characterView.pivotY = squareSize / 2F
        this.addView(characterView)

        Picasso.get().load(c.image).fit().centerCrop().into(characterView)
        invalidate()

        characters.add(c)
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
    fun grabCharacer (idx: Int) {
        if (idx == -1)
            return
        characterDrag = idx
        val c = children.find {it.id == characters[characterDrag].id}!!
        c.scaleY = 1.2F
        c.scaleX = 1.2F
    }
    fun moveCharacter(distanceX: Float, distanceY: Float) {
//        characters[characterDrag] += distanceX
        val c = children.find {it.id == characters[characterDrag].id}!!
        c.x -= distanceX / scaleX
        c.y -= distanceY / scaleY
    }
    fun placeCharacter(x: Float, y: Float) {
        val c = children.find {it.id == characters[characterDrag].id}!!
        c.scaleY = 1F
        c.scaleX = 1F
        characters[characterDrag].pos = coords2pos(x, y)
        c.x = (coords2pos(x, y).x * squareSize).toFloat() + (paint.strokeWidth / 2)
        c.y = (coords2pos(x, y).y * squareSize).toFloat() + (paint.strokeWidth / 2)
        characterDrag = -1
    }
}