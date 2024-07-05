package com.rickyslash.batiknitikclassifier.presentation.main

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.rickyslash.batiknitikclassifier.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
            finishAfterTransition() // avoid memory leak in Android Q
        else super.onBackPressed()
    }
}