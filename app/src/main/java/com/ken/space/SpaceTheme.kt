package com.ken.space

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object SpaceTheme {
    private val colors = lightColors(
        primary = Color.Black,
        secondary = Color.Blue,
    )

    @Composable
    fun Scaffold(title: String, content: @Composable (PaddingValues)->Unit) {
        MaterialTheme(colors = colors) {
            Scaffold(
                topBar = {
                    TopAppBar() {
                        Text(title, style = MaterialTheme.typography.h6)
                    }
                },
                content = content
            )
        }
    }
}