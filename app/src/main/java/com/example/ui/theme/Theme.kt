package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SocietyLightColorScheme = lightColorScheme(
  primary = Color(0xFFC62828), // Rich Society Crimson / Vermilion
  onPrimary = Color(0xFFFFFFFF),
  primaryContainer = Color(0xFFFFEBEE),
  onPrimaryContainer = Color(0xFFB71C1C),
  secondary = Color(0xFFE65100), // Auspicious Hanuman Saffron / Orange
  onSecondary = Color(0xFFFFFFFF),
  secondaryContainer = Color(0xFFFFF3E0),
  onSecondaryContainer = Color(0xFFE65100),
  tertiary = Color(0xFF2E7D32), // Verified Green
  onTertiary = Color(0xFFFFFFFF),
  tertiaryContainer = Color(0xFFE8F5E9),
  onTertiaryContainer = Color(0xFF1B5E20),
  background = Color(0xFFFFFFFF), // Pure bright crisp light background
  onBackground = Color(0xFF212121),
  surface = Color(0xFFFFFFFF), // Pure bright surface
  onSurface = Color(0xFF212121),
  surfaceVariant = Color(0xFFF5F5F5), // Soft subtle light grey
  onSurfaceVariant = Color(0xFF424242),
  outline = Color(0xFFBDBDBD),
  outlineVariant = Color(0xFFEEEEEE),
  error = Color(0xFFD32F2F),
  onError = Color(0xFFFFFFFF),
  errorContainer = Color(0xFFFFEBEE),
  onErrorContainer = Color(0xFFC62828)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Always force pristine, clean Light Theme per user request
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = SocietyLightColorScheme,
    typography = Typography,
    content = content
  )
}
