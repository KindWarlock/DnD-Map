package com.example.dndmap


import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModel
import com.example.dndmap.databinding.FragmentMapBinding
import com.example.dndmap.ui.MapViewModel
import android.view.WindowMetrics
import com.squareup.picasso.Picasso

class MapFragment : Fragment(),
    GestureDetector.OnGestureListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    lateinit var mDetector: GestureDetectorCompat
    lateinit var mScaleDetector: ScaleGestureDetector

    var mScaleFactor = 1F

    var fogMode = false
    var scaling = false
//    val viewModel: MapViewModel by activityViewModels()

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
        mScaleDetector = ScaleGestureDetector(requireContext(), scaleListener)
        Picasso.get().load(R.drawable.mapexample).resize(5000, 0).into(binding.backgroundImage)
        Log.d("TAG", "Image loading")

        binding.root.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_MOVE && fogMode) {
                val cancel = MotionEvent.obtain(event)
                cancel.action = MotionEvent.ACTION_CANCEL
                mDetector.onTouchEvent(cancel)
            }

            if (event.action == MotionEvent.ACTION_UP) {
                fogMode = false
                scaling = false
            }

            if (mDetector.onTouchEvent(event) || mScaleDetector.onTouchEvent(event)) {
                true
            } else {
                binding.root.onTouchEvent(event)
            }
        }
        return binding.root
    }


    override fun onDown(event: MotionEvent): Boolean {
//        mx = event.x
//          my = event.y
          return true
    }

    override fun onLongPress(event: MotionEvent) {
//        Log.d("TAG", "Long press? $event")
//        binding.grid.dispatchTouchEvent(event)
//        Log.d("TAG", "Long press in fragment!") // first this, in view second
        fogMode = true
    }

    override fun onShowPress(event: MotionEvent) {
        return
    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
//        Log.d("TAG", "x: ${event.x}, y: ${event.y}, left: ${binding.hScroll.scrollX}")
        return true
    }

    override fun onScroll(event0: MotionEvent,
                          event1: MotionEvent,
                          distanceX: Float,
                          distanceY: Float): Boolean {
//        if (scaling)
//            return true
        if (!fogMode) {
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


    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(sDetector: ScaleGestureDetector): Boolean {
//            Log.d("tag", "Scaling")
            mScaleFactor *= sDetector.scaleFactor

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f))
            binding.trueRoot.scaleY = mScaleFactor
            binding.trueRoot.scaleX = mScaleFactor
            binding.trueRoot.invalidate()

            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            Log.d("tag", "Scaling begin")
            Log.d("tag", "ScrollX: ${binding.hScroll.scrollX}, scrollY:${binding.vScroll.scrollY}")

            binding.trueRoot.pivotY = detector.focusY + binding.vScroll.scrollY
            binding.trueRoot.pivotX = detector.focusX + binding.hScroll.scrollX
            scaling = true

            return true
        }
    }


    fun scroll(distanceX: Float, distanceY: Float) {
        binding.vScroll.scrollBy((distanceX).toInt(), (distanceY).toInt())
        binding.hScroll.scrollBy((distanceX).toInt(), (distanceY).toInt())
    }

    fun drawFog(x: Float, y: Float) {
        Log.d("TAG", "mScaleFactor: $mScaleFactor, " +
                "scrollX: ${binding.hScroll.scrollX}, " +
                "scrollY: ${binding.vScroll.scrollY}, " +
                "x: $x, y: $y")
        binding.grid.addFog((x + binding.hScroll.scrollX) / mScaleFactor,
            (y + binding.vScroll.scrollY) / mScaleFactor,
            mScaleFactor)
        binding.grid.invalidate()
    }
}

