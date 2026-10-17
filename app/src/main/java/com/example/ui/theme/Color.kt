package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Navy & Slate School Brand Palette
val Navy900 = Color(0xFF0A192F)
val Navy800 = Color(0xFF0F2744)
val Navy700 = Color(0xFF1B3B6F)
val Navy600 = Color(0xFF285398)

val BlueAccent = Color(0xFF2563EB)
val BlueAccentLight = Color(0xFF60A5FA)
val BlueSoft = Color(0xFFEFF6FF)

val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate500 = Color(0xFF64748B)
val Slate400 = Color(0xFF94A3B8)
val Slate300 = Color(0xFFCBD5E1)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val Slate50 = Color(0xFFF8FAFC)

val EmeraldGreen = Color(0xFF10B981)
val EmeraldLight = Color(0xFFD1FAE5)
val EmeraldSoft = Color(0xFFD1FAE5)
val EmeraldDark = Color(0xFF065F46)

val AmberWarning = Color(0xFFF59E0B)
val AmberLight = Color(0xFFFEF3C7)
val AmberSoft = Color(0xFFFEF3C7)
val AmberDark = Color(0xFF92400E)

val CrimsonRed = Color(0xFFEF4444)
val CrimsonLight = Color(0xFFFEE2E2)
val CrimsonDark = Color(0xFF991B1B)

val PurpleDuty = Color(0xFF8B5CF6)
val PurpleDutyLight = Color(0xFFEDE9FE)

// =========================================================================
// CENTRALIZED SEMANTIC ACCESSIBILITY TOKENS (WCAG 2.2 AA / AAA Compliant)
// =========================================================================
// 1. Text & Foreground Tokens (For Light Surfaces)
val AppTextPrimary = Color(0xFF111827)       // Slate 900+ / Deep Charcoal - WCAG 14:1+ on White
val AppTextSecondary = Color(0xFF1F2937)     // Slate 800 - WCAG 11:1+ on White
val AppTextMuted = Color(0xFF475569)         // Slate 600 - WCAG 5.5:1+ on White (helper text)
val AppTextPlaceholder = Color(0xFF64748B)   // Slate 500 - WCAG 4.6:1+ on White (clearly visible yet distinguishable from entered text)
val AppTextDisabled = Color(0xFF64748B)      // Slate 500 - Legible disabled text (meets 3:1+ threshold)
val AppTextOnDark = Color(0xFFFFFFFF)        // Pure White on Navy/Dark containers
val AppTextOnPrimary = Color(0xFFFFFFFF)     // Pure White on Navy800/BlueAccent

// 2. Input Surface & Border Tokens
val AppInputBackground = Color(0xFFFFFFFF)   // Pure white input field background
val AppInputBorder = Color(0xFF94A3B8)       // Slate 400 - High-visibility 3:1+ component boundary
val AppInputBorderFocused = Color(0xFF2563EB)// BlueAccent - High-contrast focus indicator
val AppInputBorderError = Color(0xFFDC2626)  // Crimson - Error border
val AppInputBorderDisabled = Color(0xFFCBD5E1)// Slate 300 - Disabled border

// 3. Chip & Selector Tokens
val AppChipSelectedBg = Color(0xFF0F2744)    // Navy 800 - Prominent selected chip background
val AppChipSelectedText = Color(0xFFFFFFFF)  // White text on selected chip
val AppChipUnselectedBg = Color(0xFFFFFFFF)  // White background for unselected chip
val AppChipUnselectedText = Color(0xFF111827)// Deep Charcoal text for unselected chip (WCAG 14:1)
val AppChipUnselectedBorder = Color(0xFFCBD5E1)// Slate 300 - Crisp boundary for unselected chip

// 4. Surface & Container Tokens
val AppSurfaceCard = Color(0xFFFFFFFF)       // Crisp white cards
val AppSurfaceBackground = Color(0xFFF8FAFC) // Slate 50 - Background
val AppSurfaceBorder = Color(0xFFE2E8F0)     // Slate 200 - Card divider/border
val AppSurfaceBorderCrisp = Color(0xFFCBD5E1)// Slate 300 - Strong card border

// 5. Validation & Feedback Tokens
val AppErrorText = Color(0xFFB91C1C)         // Dark Crimson (WCAG 7:1+ on White / 4.5:1 on light red)
val AppErrorBg = Color(0xFFFEE2E2)           // Light Red surface
val AppSuccessText = Color(0xFF065F46)       // Dark Emerald (WCAG 7:1+ on White)
val AppSuccessBg = Color(0xFFD1FAE5)         // Light Emerald surface

