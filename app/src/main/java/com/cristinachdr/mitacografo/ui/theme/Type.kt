package com.cristinachdr.mitacografo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Tipografía más grande que la de Material por defecto: se lee de un vistazo en cabina.
private val base = Typography()

val Typography = Typography(
    displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontSize = 20.sp, lineHeight = 26.sp),
    bodyLarge = base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontSize = 14.sp),
)
