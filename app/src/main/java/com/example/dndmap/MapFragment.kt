package com.example.dndmap


import android.annotation.SuppressLint
import android.graphics.PointF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.view.GestureDetectorCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainer
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dndmap.data.Pos
import com.example.dndmap.databinding.FragmentMapBinding
import com.example.dndmap.ui.MapViewModel
import com.example.dndmap.ui.ScrollMode
import com.example.dndmap.views.MapGridView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.sign


class MapFragment : Fragment(),
    GestureDetector.OnGestureListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by activityViewModels()

    lateinit var mDetector: GestureDetectorCompat

//    var mScaleFactor = 1F


    private var fogMode = FogMode.NONE
    var scaling = false

    private enum class FogMode {
        DRAWING,
        ERASING,
        NONE
    }

    var mBackgroundImage : Uri? = null
    var editGrid = false
    var scrollMode = ScrollMode.SCROLL
    private val scaleGestureDetector by lazy {
        activity?.let {
            ScaleGestureDetector(it, object : ScaleGestureDetector.OnScaleGestureListener {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
//                    Log.d("TAG", "Scale ")
                    if (editGrid) {
                        binding.grid.scale(detector.scaleFactor)
                        return true
                    }
                    // TODO: fix white borders
                    binding.trueRoot.run {
                        val paddings = getPaddings()
                        val newScale = scaleX * detector.scaleFactor

                        val viewHeight = requireActivity().findViewById<FragmentContainerView>(R.id.map).height
                        val viewWidth = requireActivity().window.decorView.width

                        val imageWidth = binding.backgroundImage.drawable.intrinsicWidth
                        val imageHeight = binding.backgroundImage.drawable.intrinsicHeight

                        if (viewHeight >= newScale * imageHeight
                            || viewWidth >= newScale * imageWidth) {
                            return true
                        }

                        val leftPadding = pivotX * newScale - pivotX
                        val topPadding = pivotY * newScale - pivotY
                        val rightPadding = imageWidth * newScale - (imageWidth + leftPadding)
                        val bottomPadding = imageHeight * newScale - (imageHeight + topPadding)

                        if (leftPadding < translationX) {
                            translationX -= leftPadding
                            pivotX = 0F
                        } else if (imageWidth + rightPadding - viewWidth + translationX < 0){
                            translationX += rightPadding
                            pivotX = imageWidth.toFloat()
                        }
//                        Log.d("TAG", "${imageHeight + topPadding - requireActivity().findViewById<FragmentContainerView>(R.id.map).height + translationY}")
                        if (topPadding < translationY) {
                            translationY -= topPadding
                            pivotY = 0F
                        } else if (imageHeight + bottomPadding - viewHeight + translationY < 0){
                            translationY += bottomPadding
                            pivotY = imageHeight.toFloat()
                        }
                        changeScales(newScale)
                        invalidate()
                        return true
                    }

                }

                override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
//                    Log.d("TAG", "Scale begin")
                    scaling = true
                    if (editGrid) {
                        binding.grid.scaleBegin(detector.focusX, detector.focusY)
                        return true
                    }
                    binding.trueRoot.run {
                        val actualPivot = PointF(
                            (detector.focusX - translationX + pivotX * (scaleX - 1)) / scaleX,
                            (detector.focusY - translationY + pivotY * (scaleY - 1)) / scaleY,
                        )
                        translationX -= (pivotX - actualPivot.x) * (scaleX - 1)
                        translationY -= (pivotY - actualPivot.y) * (scaleY - 1)
                        pivotX = actualPivot.x
                        pivotY = actualPivot.y
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
//        Picasso.get().load(R.drawable.mapexample).fit().centerInside().into(binding.backgroundImage)

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
//        viewModel.changeBackgroundImage(R.drawable.mapexample)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {data ->
                    Log.d("TAG", "data collected")
                    editGrid = data.editGrid
                    if (data.backgroundImage != mBackgroundImage) {
                        Picasso.get().load(data.backgroundImage).fit().centerInside().into(binding.backgroundImage)
                        // TODO: without variable
                        mBackgroundImage = data.backgroundImage
                        binding.trueRoot.translationX = 0F
                        binding.trueRoot.translationY = 0F
                        changeScales(1F)
                    }
                    scrollMode = data.scrollMode
//                    Log.d("TAG", "${data}")
                    binding.grid.editCharacters(data.characters)
                }
            }
        }
        return binding.root
    }

    override fun onDown(event: MotionEvent): Boolean {
        return true
    }

    override fun onLongPress(event: MotionEvent) {
        val paddings = getPaddings()
        // TODO: fun getTrueXY
        val newX = (event.x - binding.trueRoot.translationX + paddings[0]) / binding.trueRoot.scaleX
        val newY = (event.y - binding.trueRoot.translationY + paddings[1]) / binding.trueRoot.scaleY
        if (scrollMode == ScrollMode.FOG) {
            fogMode = if (binding.grid.checkFog(newX, newY))
                FogMode.ERASING
            else {
                FogMode.DRAWING
            }
        } else if (scrollMode == ScrollMode.CHARACTER) {

        }
//        drawFog(event.x, event.y)
    }

    override fun onShowPress(event: MotionEvent) {
        return
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onScroll(event0: MotionEvent,
                          event1: MotionEvent,
                          distanceX: Float,
                          distanceY: Float): Boolean {
//        if (scaling)
//            return true
//        scroll(distanceX, distanceY)
        if (editGrid) {
            binding.grid.scroll(distanceX / binding.trueRoot.scaleX,
                distanceY / binding.trueRoot.scaleY)
            return true
        }
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

            val leftBorder = pivotX * scaleX - pivotX
            val rightBorder = imageWidth * scaleX - (imageWidth + leftBorder) // Scaled width - width - border
            val topBorder = pivotY * scaleY - pivotY
            val bottomBorder = imageHeight * scaleY - (imageHeight + topBorder)

            if (translationX - leftBorder <= distanceX
                && -translationX + distanceX <= imageWidth - (requireActivity().window.decorView.width) + rightBorder)
                translationX -= distanceX
            if (translationY - topBorder <= distanceY
                && -translationY + distanceY <= imageHeight - (requireActivity().findViewById<FragmentContainerView>(R.id.map).height) + bottomBorder)
                translationY -= distanceY
        }
    }

    fun drawFog(x: Float, y: Float) {
//        binding.grid.addFog((x + binding.hScroll.scrollX) / mScaleFactor,
//            (y + binding.vScroll.scrollY) / mScaleFactor,
//            mScaleFactor)
        val paddings = getPaddings()
        val newX = (x - binding.trueRoot.translationX + paddings[0]) / binding.trueRoot.scaleX
        val newY = (y - binding.trueRoot.translationY + paddings[1]) / binding.trueRoot.scaleY
        if (fogMode == FogMode.DRAWING){
            binding.grid.addFog(newX, newY)}
        else
            binding.grid.removeFog(newX, newY)
        binding.grid.invalidate()
    }

    private fun getPaddings() : FloatArray {
        val imageWidth = binding.backgroundImage.drawable.intrinsicWidth
        val imageHeight = binding.backgroundImage.drawable.intrinsicHeight
        binding.trueRoot.run {
            val leftPadding = pivotX * scaleX - pivotX
            val rightPadding = imageWidth * scaleX - (imageWidth + leftPadding) // Scaled width - width - border
            val topPadding = pivotY * scaleY - pivotY
            val bottomPadding = imageHeight * scaleY - (imageHeight + topPadding)
            return floatArrayOf(leftPadding, topPadding, rightPadding, bottomPadding)
        }
    }
    private fun changeScales(newScale : Float) {
        binding.trueRoot.run {
            scaleX = newScale
            scaleY = newScale
        }
    }
    // TODO: when to create an object?
    fun addCharacter() {

//        binding.grid.addCharacter("test", Pos(1,1), )
    }
}

