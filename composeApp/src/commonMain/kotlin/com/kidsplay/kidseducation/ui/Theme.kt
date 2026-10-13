package com.kidsplay.kidseducation.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kidsplay.kidseducation.resources.Res
import com.kidsplay.kidseducation.resources.poppins_bold
import org.jetbrains.compose.resources.Font

/** Palette taken from the original app's colors_home_kids.xml. */
object KidsColors {
    val HeaderTop = Color(0xFF5B92F5)
    val HeaderBottom = Color(0xFF3A6FD8)
    val PageTop = Color(0xFFEAF4FF)
    val PageBottom = Color(0xFFFFF6E0)
    val Sun = Color(0xFFFFC107)
    val Good = Color(0xFF4CAF50)
    val Bad = Color(0xFFEF5350)
    val Ink = Color(0xFF1F2A44)
    val Tile = Color(0xFFFFFFFF)
    /** Reel progress bar (old colorAccent). */
    val Accent = Color(0xFF6EC6FF)

    val PageBrush = Brush.verticalGradient(listOf(PageTop, PageBottom))
    val HeaderBrush = Brush.verticalGradient(listOf(HeaderTop, HeaderBottom))

    /** Rainbow used for the colourful titles. */
    val Rainbow = listOf(
        Color(0xFFFF5252), Color(0xFFFF9800), Color(0xFFFFC107), Color(0xFF4CAF50),
        Color(0xFF29B6F6), Color(0xFF7E57C2), Color(0xFFEC407A),
    )
}

val LocalDisplayFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

@Composable
fun KidsTheme(content: @Composable () -> Unit) {
    // Poppins Bold: big, round, very easy for children to read (the old OhWhale font was too decorative).
    val display = FontFamily(Font(Res.font.poppins_bold, FontWeight.Bold))
    val base = Typography()
    val typography = Typography(
        displayLarge = base.displayLarge.copy(fontFamily = display),
        displayMedium = base.displayMedium.copy(fontFamily = display),
        displaySmall = base.displaySmall.copy(fontFamily = display),
        headlineLarge = base.headlineLarge.copy(fontFamily = display),
        headlineMedium = base.headlineMedium.copy(fontFamily = display),
        headlineSmall = base.headlineSmall.copy(fontFamily = display),
        titleLarge = base.titleLarge.copy(fontFamily = display),
        titleMedium = base.titleMedium.copy(fontFamily = display),
        titleSmall = base.titleSmall.copy(fontFamily = display),
        labelLarge = base.labelLarge.copy(fontFamily = display),
        labelMedium = base.labelMedium.copy(fontFamily = display),
    )
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = KidsColors.HeaderTop,
            secondary = KidsColors.Sun,
            background = KidsColors.PageTop,
            surface = KidsColors.Tile,
            onSurface = KidsColors.Ink,
            onBackground = KidsColors.Ink,
        ),
        typography = typography,
    ) {
        CompositionLocalProvider(LocalDisplayFont provides display, content = content)
    }
}
