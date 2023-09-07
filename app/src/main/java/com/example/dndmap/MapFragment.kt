package com.example.dndmap


import android.annotation.SuppressLint
import android.graphics.PointF
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import com.example.dndmap.databinding.FragmentMapBinding
import com.squareup.picasso.Picasso


class MapFragment : Fragment(),
    GestureDetector.OnGestureListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    lateinit var mDetector: GestureDetectorCompat

    var mScaleFactor = 1F
    var mPivotX = 0F
    var mPivotY = 0F


    private var fogMode = FogMode.NONE
    var scaling = false

    private enum class FogMode {
        DRAWING,
        ERASING,
        NONE
    }
    private val scaleGestureDetector by lazy {
        activity?.let {
            ScaleGestureDetector(it, object : ScaleGestureDetector.OnScaleGestureListener {

                override fun onScale(detector: ScaleGestureDetector): Boolean {
//                    Log.d("TAG", "Scale ")

                    if (mScaleFactor * detector.scaleFactor < 1) {
                        return true
                    }
                    mScaleFactor *= detector.scaleFactor
                    binding.trueRoot.scaleY = mScaleFactor
                    binding.trueRoot.scaleX = mScaleFactor
                    binding.trueRoot.invalidate()
                    return true
                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
//                    Log.d("TAG", "Scale begin")
                    scaling = true
                    binding.trueRoot.run {
                        val actualPivot = PointF(
                            (detector.focusX - translationX + pivotX * (mScaleFactor - 1)) / mScaleFactor,
                            (detector.focusY - translationY + pivotY * (mScaleFactor - 1)) / mScaleFactor,
                        )
                        translationX -= (pivotX - actualPivot.x) * (mScaleFactor - 1)
                        translationY -= (pivotY - actualPivot.y) * (mScaleFactor - 1)
                        pivotX = actualPivot.x
                        pivotY = actualPivot.y
                        mPivotX = pivotX
                        mPivotY = pivotY

                    }
                    return true
                }

                override fun onScaleEnd(detector: ScaleGestureDetector) = Unit
            })
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
//        binding.backgroundImage.setImageResource(R.drawable.mapexample)
        binding.trueRoot.addOnLayoutChangeListener{ v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if ((right - oldRight != 0) && (bottom - oldBottom != 0)) {
                binding.grid.requestLayout()
            }
        }
        mDetector = GestureDetectorCompat(requireContext(), this)
        Picasso.get().load(R.drawable.mapexample).fit().centerInside().into(binding.backgroundImage)

        binding.root.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_MOVE && (fogMode != FogMode.NONE)) {
                val cancel = MotionEvent.obtain(event)
                cancel.action = MotionEvent.ACTION_CANCEL
                mDetector.onTouchEvent(cancel)
            }

            if (event.action == MotionEvent.ACTION_UP) {
                fogMode = FogMode.NONE
                scaling = false
            }
            if (scaling || event.pointerCount > 1) {
                scaleGestureDetector!!.onTouchEvent(event)
            } else
            if (mDetector.onTouchEvent(event)) {
                true
            } else {
                binding.root.onTouchEvent(event)
            }
        }
        return binding.root
    }


    override fun onDown(event: MotionEvent): Boolean {
        return true
    }

    override fun onLongPress(event: MotionEvent) {
        // TODO
        val paddings = getPaddings()
        Log.d("TAG", "Long press")
        fogMode = if (binding.grid.checkFog(paddings[0] + event.x - binding.trueRoot.translationX,
                event.y + paddings[1] - binding.trueRoot.translationY, mScaleFactor))
            FogMode.ERASING
        else{
            Log.d("TAG", "Drawing")
            FogMode.DRAWING
        }
//        drawFog(event.x, event.y)
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
//        if (scaling)
//            return true
//        scroll(distanceX, distanceY)

        if (fogMode == FogMode.NONE) {
            scroll(distanceX, distanceY)
        } else {
            drawFog(event1.x, event1.y)
        }
        return true
    }

    override fun onFling(event0: MotionEvent,
                         event1: MotionEvent,
                         distanceX: Float,
                         distanceY: Float): Boolean{
        return true
    }

    @SuppressLint("InternalInsetResource")
    @RequiresApi(Build.VERSION_CODES.R)
    fun scroll(distanceX: Float, distanceY: Float) {
        binding.trueRoot.run {
            val imageWidth = binding.backgroundImage.drawable.intrinsicWidth
            val imageHeight = binding.backgroundImage.drawable.intrinsicHeight

            var navigationBarHeight = 0
            var resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
            if (resourceId > 0) {
                navigationBarHeight = resources.getDimensionPixelSize(resourceId)
            }

            val leftBorder = mPivotX * mScaleFactor - mPivotX
            val rightBorder = imageWidth * mScaleFactor - (imageWidth + leftBorder) // Scaled width - width - border
            val topBorder = mPivotY * mScaleFactor - mPivotY
            val bottomBorder = imageHeight * mScaleFactor - (imageHeight + topBorder)

            if (translationX - leftBorder <= distanceX
                && -translationX + distanceX <= imageWidth - (requireActivity().window.decorView.width) + rightBorder)
                translationX -= distanceX
            if (translationY - topBorder <= distanceY
                && -translationY + distanceY <= imageHeight - (requireActivity().windowManager.defaultDisplay.height) + navigationBarHeight + bottomBorder)
                translationY -= distanceY
        }
//        Log.d("tag", "Scrolling")
    }

    fun drawFog(x: Float, y: Float) {
//        binding.grid.addFog((x + binding.hScroll.scrollX) / mScaleFactor,
//            (y + binding.vScroll.scrollY) / mScaleFactor,
//            mScaleFactor)
        val paddings = getPaddings()
        if (fogMode == FogMode.DRAWING){
            binding.grid.addFog(x - binding.trueRoot.translationX + paddings[0], y - binding.trueRoot.translationY + paddings[1], mScaleFactor)}
        else
            binding.grid.removeFog(x - binding.trueRoot.translationX + paddings[0], y - binding.trueRoot.translationY + paddings[1], mScaleFactor)
        binding.grid.invalidate()
    }

    private fun getPaddings() : FloatArray {
        val imageWidth = binding.backgroundImage.drawable.intrinsicWidth
        val imageHeight = binding.backgroundImage.drawable.intrinsicHeight

        val leftPadding = mPivotX * mScaleFactor - mPivotX
        val rightPadding = imageWidth * mScaleFactor - (imageWidth + leftPadding) // Scaled width - width - border
        val topPadding = mPivotY * mScaleFactor - mPivotY
        val bottomPadding = imageHeight * mScaleFactor - (imageHeight + topPadding)
        return floatArrayOf(leftPadding, topPadding, rightPadding, bottomPadding)
    }
}

