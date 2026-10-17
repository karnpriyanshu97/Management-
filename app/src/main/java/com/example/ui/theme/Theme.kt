package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import com.example.ui.components.SafeTextToolbarHost
import com.example.ui.components.rememberSafeTextToolbar

// Accessible Light Color Scheme with WCAG 2.2 AA / AAA Compliance
private val LightColorScheme = lightColorScheme(
    primary = Navy800,
    onPrimary = Color.White,
    primaryContainer = BlueSoft,
    onPrimaryContainer = Navy900,
    secondary = BlueAccent,
    onSecondary = Color.White,
    secondaryContainer = BlueSoft,
    onSecondaryContainer = Navy900,
    background = AppSurfaceBackground,
    surface = AppSurfaceCard,
    onBackground = AppTextPrimary,
    onSurface = AppTextPrimary,            // Always crisp #111827 on light surfaces (WCAG 14:1)
    surfaceVariant = Slate100,
    onSurfaceVariant = AppTextSecondary,    // Crisp #1F2937 (WCAG 11:1)
    outline = AppInputBorder,             // Slate 400 (#94A3B8) visible 3:1+ input & card borders
    outlineVariant = AppSurfaceBorderCrisp,// Slate 300 (#CBD5E1)
    error = CrimsonRed,
    onError = Color.White
)

// Dark Theme Scheme with matching high-contrast dark-surface semantics
private val DarkColorScheme = darkColorScheme(
    primary = BlueAccentLight,
    onPrimary = Navy900,
    primaryContainer = Navy700,
    onPrimaryContainer = Color.White,
    secondary = BlueAccent,
    onSecondary = Color.White,
    secondaryContainer = Navy700,
    onSecondaryContainer = Color.White,
    background = Navy900,
    surface = Slate800,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Slate700,
    onSurfaceVariant = Color(0xFFE2E8F0),
    outline = Slate400,
    outlineVariant = Slate600,
    error = CrimsonRed,
    onError = Color.White
)

/**
 * Standardized High-Contrast Text Field Colors
 * Enforces explicit foreground, background, border, and placeholder contrast
 * preventing any invisible white-on-white or low-contrast characters.
 */
object AppTextFieldDefaults {
    @Composable
    fun outlinedColors(
        containerColor: Color = AppInputBackground,
        textColor: Color = AppTextPrimary,
        placeholderColor: Color = AppTextPlaceholder,
        focusedBorderColor: Color = AppInputBorderFocused,
        unfocusedBorderColor: Color = AppInputBorder,
        focusedLabelColor: Color = AppInputBorderFocused,
        unfocusedLabelColor: Color = AppTextSecondary,
        errorTextColor: Color = AppErrorText,
        errorBorderColor: Color = AppInputBorderError,
        disabledTextColor: Color = AppTextDisabled,
        disabledBorderColor: Color = AppInputBorderDisabled,
        disabledContainerColor: Color = Slate100
    ): TextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = disabledTextColor,
        errorTextColor = errorTextColor,
        focusedContainerColor = containerColor,
        unfocusedContainerColor = containerColor,
        disabledContainerColor = disabledContainerColor,
        errorContainerColor = containerColor,
        focusedBorderColor = focusedBorderColor,
        unfocusedBorderColor = unfocusedBorderColor,
        disabledBorderColor = disabledBorderColor,
        errorBorderColor = errorBorderColor,
        focusedLabelColor = focusedLabelColor,
        unfocusedLabelColor = unfocusedLabelColor,
        disabledLabelColor = disabledTextColor,
        errorLabelColor = errorTextColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        disabledPlaceholderColor = disabledTextColor.copy(alpha = 0.6f),
        cursorColor = focusedBorderColor,
        errorCursorColor = errorBorderColor
    )
}

/**
 * Standardized High-Contrast Filter Chip Colors
 * Guarantees that BOTH selected and unselected chip states have clearly readable labels,
 * prominent borders, and strong visual feedback.
 */
object AppFilterChipDefaults {
    @Composable
    fun colors(
        selectedContainerColor: Color = AppChipSelectedBg,
        selectedLabelColor: Color = AppChipSelectedText,
        unselectedContainerColor: Color = AppChipUnselectedBg,
        unselectedLabelColor: Color = AppChipUnselectedText,
        selectedIconColor: Color = AppChipSelectedText,
        unselectedIconColor: Color = AppTextSecondary
    ): SelectableChipColors = FilterChipDefaults.filterChipColors(
        containerColor = unselectedContainerColor,
        labelColor = unselectedLabelColor,
        iconColor = unselectedIconColor,
        selectedContainerColor = selectedContainerColor,
        selectedLabelColor = selectedLabelColor,
        selectedLeadingIconColor = selectedIconColor,
        selectedTrailingIconColor = selectedIconColor
    )
}

object AppCheckboxDefaults {

    @Composable
    fun colors(
        checkedColor: Color = Navy800,
        uncheckedColor: Color = AppInputBorder,
        checkmarkColor: Color = Color.White
    ): CheckboxColors = CheckboxDefaults.colors(
        checkedColor = checkedColor,
        uncheckedColor = uncheckedColor,
        checkmarkColor = checkmarkColor
    )
}


@Composable
fun MyApplicationTheme(
    // The EduPulse / 1campus institutional operations platform uses an intentional,
    // crisp institutional light design system with high-contrast surfaces.
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val safeTextToolbar = rememberSafeTextToolbar()

    SafeTextToolbarHost(toolbar = safeTextToolbar) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

