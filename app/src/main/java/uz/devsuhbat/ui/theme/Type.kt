package uz.devsuhbat.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import uz.devsuhbat.R

/** One weight of a bundled variable font. */
private fun variable(resId: Int, weight: Int) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

internal val BricolageGrotesque = FontFamily(
    variable(R.font.bricolage_grotesque, 500),
    variable(R.font.bricolage_grotesque, 700),
    variable(R.font.bricolage_grotesque, 800),
)

internal val Figtree = FontFamily(
    variable(R.font.figtree, 400),
    variable(R.font.figtree, 600),
    variable(R.font.figtree, 700),
)

val JetBrainsMono = FontFamily(
    variable(R.font.jetbrains_mono, 400),
    variable(R.font.jetbrains_mono, 500),
)

private fun style(
    family: FontFamily,
    size: Int,
    line: Int,
    weight: Int,
    tracking: TextUnit = 0.sp,
) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking,
)

internal val DevSuhbatTypography = Typography(
    displayLarge = style(BricolageGrotesque, 56, 64, 800, (-0.5).sp),
    displayMedium = style(BricolageGrotesque, 44, 52, 800, (-0.5).sp),
    displaySmall = style(BricolageGrotesque, 36, 44, 800, (-0.5).sp),
    headlineLarge = style(BricolageGrotesque, 32, 40, 800),
    headlineMedium = style(BricolageGrotesque, 28, 36, 800),
    headlineSmall = style(BricolageGrotesque, 22, 30, 700),
    titleLarge = style(BricolageGrotesque, 22, 28, 700),
    titleMedium = style(BricolageGrotesque, 18, 24, 700),
    titleSmall = style(BricolageGrotesque, 15, 20, 700),
    bodyLarge = style(Figtree, 16, 24, 400),
    bodyMedium = style(Figtree, 14, 20, 400),
    bodySmall = style(Figtree, 12, 16, 400),
    labelLarge = style(Figtree, 15, 20, 700),
    labelMedium = style(Figtree, 13, 18, 600),
    labelSmall = style(Figtree, 12, 16, 600),
)
