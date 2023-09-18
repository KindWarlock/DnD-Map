package com.example.dndmap

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dndmap.data.Pos
import com.example.dndmap.databinding.ActivityMapEditorBinding
import com.example.dndmap.ui.MapViewModel
import com.example.dndmap.ui.ScrollMode
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch


class MapEditorActivity : AppCompatActivity() {

    private var _binding: ActivityMapEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()

    // TODO: GALLERY_REQUEST
    private val getBackgroundImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        viewModel.changeBackgroundImage(uri!!)
    }

    private val getCharacterImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        viewModel.addCharacter(uri!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMapEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        Log.d("TAG", "$uri")

        viewModel.changeBackgroundImage(Uri.parse("android.resource://com.example.dndmap/${R.drawable.mapexample}"))
        binding.btnEditGrid.setOnClickListener {
            viewModel.toggleEditGrid()
        }
        binding.btnScrollMode.setOnClickListener {
            viewModel.toggleScrollMode()
        }
        binding.btnChangeBackground.setOnClickListener {
            getBackgroundImage.launch("image/*")
        }
        binding.btnAddCharacter.setOnClickListener {
            getCharacterImage.launch("image/*")
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {data ->
//                    Log.d("TAG", "data collected")
//                    binding.btnEditGrid.text = if (data.editGrid) "Grid mode" else "Whole"
//                    binding.btnScrollMode.text = data.scrollMode.toString()
                    if (data.editGrid) {
                        binding.btnEditGrid.background = getDrawable(R.drawable.grid)
                    } else {
                        binding.btnEditGrid.background = getDrawable(R.drawable.background)
                    }

                    val dip = if (data.scrollMode == ScrollMode.SCROLL) 25F else 40F
                    val r = resources
                    val px = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dip,
                        r.displayMetrics
                    )
                    binding.btnScrollMode.layoutParams.height = px.toInt()


                    binding.btnScrollMode.background = when (data.scrollMode) {
                        ScrollMode.SCROLL -> getDrawable(R.drawable.scroll)
                        ScrollMode.FOG -> getDrawable(R.drawable.fog)
                        ScrollMode.CHARACTER -> getDrawable(R.drawable.edit_character)

                    }
                }
            }
        }
    }
}