package com.bpsc.flashcards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bpsc.flashcards.R

/** Jameel Noori Nastaleeq — the proper Nastaliq face for Urdu (subset build, bundled). */
val NooriNastaliq = FontFamily(Font(R.font.jameel_noori))

// Ink & paper: warm paper, near-black ink, one ballpoint blue. No defaults, no purple.
val Ballpen = Color(0xFF2B4ECF)
val BallpenDark = Color(0xFF9DB4FF)
val Umber = Color(0xFF9A5A2B)
val UmberDark = Color(0xFFD9A06B)

val BpscLightColors = lightColorScheme(
    primary = Ballpen,
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFAF6EF),
    onBackground = Color(0xFF1E211D),
    surface = Color(0xFFFFFDF8),
    onSurface = Color(0xFF1E211D),
    surfaceVariant = Color(0xFFF1EADC),
    onSurfaceVariant = Color(0xFF5A5F55),
    outline = Color(0xFF8C9084),
    outlineVariant = Color(0xFFE0D7C2),
)

val BpscDarkColors = darkColorScheme(
    primary = BallpenDark,
    onPrimary = Color(0xFF101426),
    background = Color(0xFF141610),
    onBackground = Color(0xFFEDE8DB),
    surface = Color(0xFF1C1F17),
    onSurface = Color(0xFFEDE8DB),
    surfaceVariant = Color(0xFF23261E),
    onSurfaceVariant = Color(0xFFADB0A2),
    outline = Color(0xFF7E8275),
    outlineVariant = Color(0xFF333827),
)

object InkType {
    val eyebrow = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 2.4.sp,
        fontWeight = FontWeight.Medium,
    )
    val counter = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.8.sp,
        fontWeight = FontWeight.Medium,
    )
    val appTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 30.sp,
        lineHeight = 37.sp,
    )
    val deckTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 21.sp,
        lineHeight = 27.sp,
    )
    val question = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 23.sp,
        lineHeight = 33.sp,
    )
    val answer = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 19.sp,
        lineHeight = 28.sp,
    )
    val body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )

    // Card text in Urdu — Jameel Noori Nastaleeq. Nastaliq needs deep leading.
    val questionUrdu = question.copy(fontFamily = NooriNastaliq, fontSize = 23.sp, lineHeight = 48.sp)
    val answerUrdu = answer.copy(fontFamily = NooriNastaliq, fontSize = 20.sp, lineHeight = 42.sp)
}

val BpscShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
)

@Composable
fun warmAccent(): Color = if (isSystemInDarkTheme()) UmberDark else Umber

@Composable
fun BpscFlashTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) BpscDarkColors else BpscLightColors,
        typography = Typography(),
        shapes = BpscShapes,
        content = content,
    )
}