package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StaffStatus
import com.example.model.UserRole
import com.example.ui.theme.*

@Composable
fun StatusBadge(
    status: StaffStatus,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        StaffStatus.FREE -> Triple(EmeraldLight, EmeraldDark, Icons.Default.CheckCircle)
        StaffStatus.TEACHING -> Triple(BlueSoft, BlueAccent, Icons.Default.School)
        StaffStatus.REPLACEMENT -> Triple(PurpleDutyLight, PurpleDuty, Icons.Default.SwapHoriz)
        StaffStatus.IN_MEETING -> Triple(AmberLight, AmberDark, Icons.Default.Groups)
        StaffStatus.ON_LEAVE -> Triple(CrimsonLight, CrimsonDark, Icons.Default.Cancel)
        StaffStatus.SPECIAL_DUTY -> Triple(Slate200, Slate800, Icons.AutoMirrored.Filled.Assignment)
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status.label,
            tint = textColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = status.label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun RoleBadge(
    role: String,
    modifier: Modifier = Modifier
) {
    val uRole = UserRole.fromString(role)
    val (bg, fg) = when (uRole) {
        UserRole.SUPER_ADMIN -> Pair(Navy900, Color.White)
        UserRole.PRINCIPAL -> Pair(Navy800, Color.White)
        UserRole.ADMIN -> Pair(Navy700, Color.White)
        UserRole.TEACHER -> Pair(BlueSoft, Navy800)
        UserRole.STAFF -> Pair(Slate200, Slate800)
        UserRole.STUDENT -> Pair(EmeraldLight, EmeraldDark)
    }

    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = uRole.displayName.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .border(1.dp, Slate200, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate500,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Slate900
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CampusHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Navy900
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

// =========================================================================
// ACCESSIBLE SHARED UI COMPONENTS (Guaranteed Contrast & WCAG Compliance)
// =========================================================================

/**
 * Standard Accessible Outlined Text Field
 * Guarantees visible dark characters (#111827), visible placeholder (#64748B),
 * crisp visible border (#94A3B8), persistent label above, and solid white background.
 */
@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorMessage: String? = null,
    isError: Boolean = false,
    isRequired: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 4,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
    testTag: String? = null
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (label != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppTextSecondary,
                    fontSize = 13.sp
                )
                if (isRequired) {
                    Text(
                        text = "*",
                        color = AppErrorText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (helperText != null) {
            Text(
                text = helperText,
                style = MaterialTheme.typography.bodySmall,
                color = AppTextMuted,
                fontSize = 11.sp
            )
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
            placeholder = if (placeholder != null) {
                {
                    Text(
                        text = placeholder,
                        color = AppTextPlaceholder,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            maxLines = maxLines,
            enabled = enabled,
            readOnly = readOnly,
            isError = isError,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = shape,
            colors = AppTextFieldDefaults.outlinedColors()
        )

        if (isError && errorMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = null,
                    tint = AppErrorText,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = errorMessage,
                    color = AppErrorText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Standard Accessible Filter Chip
 * Guarantees visible contrast in BOTH selected (Navy800 bg, White text)
 * and unselected (White bg, Slate300 border, #111827 text) states.
 */
@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(10.dp),
    testTag: String? = null
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (selected) AppChipSelectedText else AppChipUnselectedText
            )
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) AppChipSelectedText else AppTextSecondary
                )
            }
        } else null,
        shape = shape,
        colors = AppFilterChipDefaults.colors(),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = AppChipUnselectedBorder,
            selectedBorderColor = Navy800,
            borderWidth = 1.dp,
            selectedBorderWidth = 1.5.dp
        ),
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    )
}

/**
 * Standard Accessible Dropdown Menu Item
 * Prevents dropdown options from displaying invisible or low-contrast text.
 */
@Composable
fun AppDropdownItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    DropdownMenuItem(
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = AppTextPrimary,
                    fontSize = 13.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = AppTextMuted
                    )
                }
            }
        },
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = BlueAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        onClick = onClick,
        modifier = modifier
    )
}

