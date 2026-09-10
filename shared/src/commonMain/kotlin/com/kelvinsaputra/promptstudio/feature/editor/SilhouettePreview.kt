package com.kelvinsaputra.promptstudio.feature.editor

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kelvinsaputra.promptstudio.domain.VisualAssemblyState
import com.kelvinsaputra.promptstudio.guide.drawSilhouette

@Composable
fun SilhouettePreview(state: VisualAssemblyState, modifier: Modifier = Modifier, aspectRatio: String? = "4:5", thumbnail: Boolean = false) {
    Canvas(modifier.semantics { contentDescription = "Structural silhouette: ${state.summary}" }) {
        drawSilhouette(state, aspectRatio, thumbnail)
    }
}
