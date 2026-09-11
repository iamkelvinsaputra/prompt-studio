package com.kelvinsaputra.promptstudio.feature.studio

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import promptstudio.shared.generated.resources.Res
import promptstudio.shared.generated.resources.libre_baskerville

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    val display = FontFamily(Font(Res.font.libre_baskerville))
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF904B35), onPrimary = Color.White,
            primaryContainer = Color(0xFFF2DED3), onPrimaryContainer = Color(0xFF512B20),
            secondary = Color(0xFF626A56), secondaryContainer = Color(0xFFE5E9DC), onSecondaryContainer = Color(0xFF303A27),
            background = Color(0xFFF6F3ED), onBackground = Color(0xFF292C29),
            surface = Color(0xFFFFFDF8), onSurface = Color(0xFF292C29),
            surfaceContainerLow = Color(0xFFF0EDE6), surfaceContainer = Color(0xFFECE8E0),
            surfaceContainerHigh = Color(0xFFE5E1D9), onSurfaceVariant = Color(0xFF666860),
            outline = Color(0xFF85877D), outlineVariant = Color(0xFFD8D8CE),
        ),
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = display, fontSize = 38.sp, lineHeight = 44.sp),
            headlineMedium = TextStyle(fontFamily = display, fontSize = 30.sp, lineHeight = 36.sp),
            headlineSmall = TextStyle(fontFamily = display, fontSize = 25.sp, lineHeight = 31.sp),
            titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
            titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
            labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
        ),
        shapes = Shapes(small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(18.dp)),
        content = content,
    )
}
