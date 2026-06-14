package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.BibleApp
import com.example.viewmodel.BibleViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: BibleViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
       BibleApp(viewModel = viewModel)
    }
  }
}
