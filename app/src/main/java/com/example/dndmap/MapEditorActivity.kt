package com.example.dndmap

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.dndmap.databinding.ActivityMapEditorBinding
import com.example.dndmap.ui.MapViewModel


class MapEditorActivity : AppCompatActivity() {

    private var _binding: ActivityMapEditorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()

    val getImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        viewModel.changeBackgroundImage(uri!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMapEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        Log.d("TAG", "$uri")

        viewModel.changeBackgroundImage(Uri.parse("android.resource://com.example.dndmap/${R.drawable.mapexample}"))
        binding.btnEditGrid.setOnClickListener {
            viewModel.toggleGridEdit()
        }
        binding.btnChangeBackground.setOnClickListener {
            getImage.launch("image/*")
        }
    }
}