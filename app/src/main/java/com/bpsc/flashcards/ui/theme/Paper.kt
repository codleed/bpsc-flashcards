package com.bpsc.flashcards.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bpsc.flashcards.R

/**
 * The home design's material: warm paper, near-black ink, one deep green, one
 * marigold. Values come straight from the Home-html reference artboard; treat
 * that file as the source of truth when touching this screen.
 */

val FrauncesDisplay = FontFamily(Font(R.font.fraunces_display, FontWeight.Medium))
val FrauncesTitle = FontFamily(Font(R.font.fraunces_title, FontWeight.Medium))
val FrauncesText = FontFamily(Font(R.font.fraunces_text, FontWeight.Medium))

val Instrument = FontFamily(
    Font(R.font.instrument_regular, FontWeight.Normal),
    Font(R.font.instrument_medium, FontWeight.Medium),
    Font(R.font.instrument_semibold, FontWeight.SemiBold),
)

@Immutable
data class PaperColors(
    val paper: Color,
    val ink: Color,
    val muted: Color,
    val green: Color,
    val yellow: Color,
    val sand: Color,
    val cream: Color,
    /** The solid dark tile in the two-up row; stays dark in both themes. */
    val inkCard: Color,
    /** Label green: deep on paper, lifted for dark surfaces. */
    val greenText: Color,
    /** Text on the marigold surfaces; always dark, in both themes. */
    val onYellow: Color,
    val navBg: Color,
    val navActive: Color,
    val navActiveInk: Color,
    val navIcon: Color,
) {
    /** Secondary text on the green card. */
    val onGreenMuted: Color get() = cream.copy(alpha = 0.72f)
    val onGreenLine: Color get() = cream.copy(alpha = 0.20f)
    val onGreenBorder: Color get() = cream.copy(alpha = 0.50f)

    /** Secondary text on the marigold card. */
    val onYellowMuted: Color get() = onYellow.copy(alpha = 0.72f)
    val onYellowLine: Color get() = onYellow.copy(alpha = 0.20f)

    /** Secondary text on the ink card. */
    val onInkMuted: Color get() = cream.copy(alpha = 0.70f)
    val onInkLine: Color get() = cream.copy(alpha = 0.20f)
}

val LightPaper = PaperColors(
    paper = Color(0xFFF4F0E6),
    ink = Color(0xFF181816),
    muted = Color(0xFF6E6A5E),
    green = Color(0xFF1E4D3B),
    yellow = Color(0xFFF0B93A),
    sand = Color(0xFFE6E0CF),
    cream = Color(0xFFF4F0E6),
    inkCard = Color(0xFF181816),
    greenText = Color(0xFF1E4D3B),
    onYellow = Color(0xFF181816),
    navBg = Color(0xFF181816),
    navActive = Color(0xFFF4F0E6),
    navActiveInk = Color(0xFF181816),
    navIcon = Color(0xFFCFCABB),
)

val DarkPaper = PaperColors(
    paper = Color(0xFF12140F),
    ink = Color(0xFFEDE8DB),
    muted = Color(0xFFA6A18F),
    green = Color(0xFF23523D),
    yellow = Color(0xFFF0B93A),
    sand = Color(0xFF23261E),
    cream = Color(0xFFF4F0E6),
    inkCard = Color(0xFF2C2F26),
    greenText = Color(0xFF8CC3A6),
    onYellow = Color(0xFF181816),
    navBg = Color(0xFF23261E),
    navActive = Color(0xFFEDE8DB),
    navActiveInk = Color(0xFF12140F),
    navIcon = Color(0xFF8F8A7B),
)

@Composable
fun rememberPaperColors(dark: Boolean): PaperColors = if (dark) DarkPaper else LightPaper

val LocalPaperColors = staticCompositionLocalOf { LightPaper }

object PaperType {
    val eyebrow = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    )
    val tag = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    )
    val title = TextStyle(
        fontFamily = FrauncesTitle,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp,
    )
    val heroTitle = TextStyle(
        fontFamily = FrauncesText,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 22.sp,
    )
    val heroNumber = TextStyle(
        fontFamily = FrauncesDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 76.sp,
        lineHeight = 68.sp,
        letterSpacing = (-3).sp,
        fontFeatureSettings = "tnum",
    )
    val featuredTitle = TextStyle(
        fontFamily = FrauncesText,
        fontWeight = FontWeight.Medium,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    )
    val cardTitle = TextStyle(
        fontFamily = FrauncesText,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 23.sp,
    )
    val bigCount = TextStyle(
        fontFamily = FrauncesText,
        fontWeight = FontWeight.Medium,
        fontSize = 26.sp,
        lineHeight = 28.sp,
        fontFeatureSettings = "tnum",
    )
    val small = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    val smallMuted = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    val count = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontFeatureSettings = "tnum",
    )
    val micro = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontFeatureSettings = "tnum",
    )
    val button = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    )
    val buttonMedium = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    )
    val search = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    )
    val question = TextStyle(
        fontFamily = FrauncesText,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    )
    val body = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    )
    val nav = TextStyle(
        fontFamily = Instrument,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp,
    )
}
