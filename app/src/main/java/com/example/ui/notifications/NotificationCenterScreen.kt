package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.SchoolApplication
import com.example.data.local.entity.StaffUserEntity
import com.example.ui.components.CampusHeader
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationCenterScreen(
    app: SchoolApplication,
    currentUser: StaffUserEntity,
    modifier: Modifier = Modifier
) {
    val notifications by app.notificationRepository.getNotificationsForStaff(currentUser.organizationId, currentUser.id).collectAsState(initial = emptyList())
    var showOnlyUnread by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val filteredList = remember(notifications, showOnlyUnread) {
        if (showOnlyUnread) notifications.filter { !it.isRead }
        else notifications
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate50)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CampusHeader(
            title = "Notifications & Alerts",
            subtitle = "Operational broadcasts, duty alerts and invitations.",
            actions = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            app.notificationRepository.markAllAsRead(currentUser.organizationId, currentUser.id)
                        }
                    },
                    modifier = Modifier.testTag("mark_all_read_btn")
                ) {
                    Text("Mark all as read", fontSize = 12.sp, color = BlueAccent)
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !showOnlyUnread,
                onClick = { showOnlyUnread = false },
                label = {
                    Text(
                        "All (${notifications.size})",
                        fontWeight = if (!showOnlyUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (!showOnlyUnread) AppChipSelectedText else AppChipUnselectedText
                    )
                },
                colors = AppFilterChipDefaults.colors(),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = !showOnlyUnread,
                    borderColor = AppChipUnselectedBorder,
                    selectedBorderColor = Navy800,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.5.dp
                ),
                shape = RoundedCornerShape(10.dp)
            )
            FilterChip(
                selected = showOnlyUnread,
                onClick = { showOnlyUnread = true },
                label = {
                    Text(
                        "Unread (${notifications.count { !it.isRead }})",
                        fontWeight = if (showOnlyUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (showOnlyUnread) AppChipSelectedText else AppChipUnselectedText
                    )
                },
                colors = AppFilterChipDefaults.colors(),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = showOnlyUnread,
                    borderColor = AppChipUnselectedBorder,
                    selectedBorderColor = Navy800,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 1.5.dp
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Slate400, modifier = Modifier.size(48.dp))
                    Text("No Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Navy900)
                    Text("You're all caught up with school operational updates.", color = Slate500, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList) { item ->
                    val timeStr = SimpleDateFormat("h:mm a, d MMM", Locale.getDefault()).format(Date(item.timestamp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (!item.isRead) BlueSoft.copy(alpha = 0.4f) else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (!item.isRead) BlueAccent.copy(alpha = 0.4f) else Slate200, RoundedCornerShape(14.dp))
                            .clickable {
                                if (!item.isRead) {
                                    coroutineScope.launch {
                                        app.notificationRepository.markAsRead(item.id, currentUser.organizationId)
                                    }
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (item.type) {
                                            "REPLACEMENT_ASSIGNMENT" -> PurpleDutyLight
                                            "MEETING_INVITATION" -> BlueSoft
                                            "CLASSROOM_VERIFICATION_ALERT" -> AmberLight
                                            else -> Slate100
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (item.type) {
                                        "REPLACEMENT_ASSIGNMENT" -> Icons.Default.SwapHoriz
                                        "MEETING_INVITATION" -> Icons.Default.Groups
                                        "CLASSROOM_VERIFICATION_ALERT" -> Icons.Default.Warning
                                        else -> Icons.Default.Campaign
                                    },
                                    contentDescription = null,
                                    tint = when (item.type) {
                                        "REPLACEMENT_ASSIGNMENT" -> PurpleDuty
                                        "MEETING_INVITATION" -> BlueAccent
                                        "CLASSROOM_VERIFICATION_ALERT" -> AmberDark
                                        else -> Navy800
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                                        color = Navy900
                                    )
                                    if (!item.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(BlueAccent)
                                        )
                                    }
                                }

                                Text(
                                    text = item.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate700
                                )

                                Text(
                                    text = timeStr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
