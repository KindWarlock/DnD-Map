package com.example.dndmap

import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.dndmap.databinding.ActivityMapEditorBinding


class MapEditorActivity : AppCompatActivity() {

    private var _binding: ActivityMapEditorBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_editor)
    }
}