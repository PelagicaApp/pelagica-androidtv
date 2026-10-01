package app.pelagica.androidtv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Typography
import app.pelagica.androidtv.R

val Geist = FontFamily(
    Font(R.font.geist_thin, FontWeight.Thin),
    Font(R.font.geist_thin_italic, FontWeight.Thin, FontStyle.Italic),
    Font(R.font.geist_extra_light, FontWeight.ExtraLight),
    Font(R.font.geist_extra_light_italic, FontWeight.ExtraLight, FontStyle.Italic),
    Font(R.font.geist_light, FontWeight.Light),
    Font(R.font.geist_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.geist_semi_bold, FontWeight.SemiBold),
    Font(R.font.geist_semi_bold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.geist_bold, FontWeight.Bold),
    Font(R.font.geist_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.geist_extra_bold, FontWeight.ExtraBold),
    Font(R.font.geist_extra_bold_italic, FontWeight.ExtraBold, FontStyle.Italic),
    Font(R.font.geist_black, FontWeight.Black),
    Font(R.font.geist_black_italic, FontWeight.Black, FontStyle.Italic),
)

private fun TextStyle.geist() = copy(fontFamily = Geist)

@OptIn(ExperimentalTvMaterial3Api::class)
val Typography = Typography().run {
    copy(
        displayLarge = displayLarge.geist(),
        displayMedium = displayMedium.geist(),
        displaySmall = displaySmall.geist(),
        headlineLarge = headlineLarge.geist(),
        headlineMedium = headlineMedium.geist(),
        headlineSmall = headlineSmall.geist(),
        titleLarge = titleLarge.geist(),
        titleMedium = titleMedium.geist(),
        titleSmall = titleSmall.geist(),
        bodyLarge = bodyLarge.geist(),
        bodyMedium = bodyMedium.geist(),
        bodySmall = bodySmall.geist(),
        labelLarge = labelLarge.geist(),
        labelMedium = labelMedium.geist(),
        labelSmall = labelSmall.geist(),
    )
}
