package com.kelvinsaputra.promptstudio.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import com.kelvinsaputra.promptstudio.generation.model.GeneratedImage

expect fun decodeGeneratedImage(bytes: ByteArray): ImageBitmap
@Composable expect fun rememberImageSaver(onMessage: (String) -> Unit): (GeneratedImage) -> Unit
