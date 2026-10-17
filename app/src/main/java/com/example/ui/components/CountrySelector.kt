package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

data class CountryCodeItem(
    val countryName: String,
    val dialCode: String,
    val flagEmoji: String,
    val minLength: Int = 7,
    val maxLength: Int = 12
)

object SupportedCountries {
    val list = listOf(
        CountryCodeItem("India", "+91", "🇮🇳", 10, 10),
        CountryCodeItem("United States / Canada", "+1", "🇺🇸", 10, 10),
        CountryCodeItem("United Kingdom", "+44", "🇬🇧", 10, 11),
        CountryCodeItem("United Arab Emirates", "+971", "🇦🇪", 9, 9),
        CountryCodeItem("Singapore", "+65", "🇸🇬", 8, 8),
        CountryCodeItem("Australia", "+61", "🇦🇺", 9, 9),
        CountryCodeItem("Saudi Arabia", "+966", "🇸🇦", 9, 9),
        CountryCodeItem("Qatar", "+974", "🇶🇦", 8, 8),
        CountryCodeItem("Germany", "+49", "🇩🇪", 10, 11),
        CountryCodeItem("France", "+33", "🇫🇷", 9, 10),
        CountryCodeItem("Nepal", "+977", "🇳🇵", 10, 10),
        CountryCodeItem("Bangladesh", "+880", "🇧🇩", 10, 10),
        CountryCodeItem("Sri Lanka", "+94", "🇱🇰", 9, 9),
        CountryCodeItem("Malaysia", "+60", "🇲🇾", 9, 10),
        CountryCodeItem("South Africa", "+27", "🇿🇦", 9, 9),
        CountryCodeItem("New Zealand", "+64", "🇳🇿", 9, 10),
        CountryCodeItem("Kuwait", "+965", "🇰🇼", 8, 8),
        CountryCodeItem("Oman", "+968", "🇴🇲", 8, 8),
        CountryCodeItem("Bahrain", "+973", "🇧🇭", 8, 8),
        CountryCodeItem("Philippines", "+63", "🇵🇭", 10, 10)
    )

    fun findByDialCode(dialCode: String): CountryCodeItem {
        return list.firstOrNull { it.dialCode == dialCode } ?: list.first()
    }
}

@Composable
fun CountryCodeSelector(
    selectedCountry: CountryCodeItem,
    onCountrySelected: (CountryCodeItem) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Slate100,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = modifier
            .height(56.dp)
            .clickable(enabled = enabled) { showDialog = true }
            .testTag("country_code_selector_button")
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(selectedCountry.flagEmoji, fontSize = 20.sp)
            Text(
                text = selectedCountry.dialCode,
                fontWeight = FontWeight.Bold,
                color = Slate800,
                fontSize = 15.sp
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Select country dial code",
                tint = Slate600,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    if (showDialog) {
        CountryPickerDialog(
            currentSelected = selectedCountry,
            onDismiss = { showDialog = false },
            onSelect = { country ->
                onCountrySelected(country)
                showDialog = false
            }
        )
    }
}

@Composable
fun CountryPickerDialog(
    currentSelected: CountryCodeItem,
    onDismiss: () -> Unit,
    onSelect: (CountryCodeItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            SupportedCountries.list
        } else {
            val q = searchQuery.trim().lowercase()
            SupportedCountries.list.filter {
                it.countryName.lowercase().contains(q) || it.dialCode.contains(q)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .testTag("country_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Select Country Dial Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country or code...", color = AppTextPlaceholder, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = AppTextFieldDefaults.outlinedColors(
                        containerColor = Slate50,
                        textColor = AppTextPrimary,
                        placeholderColor = AppTextPlaceholder,
                        unfocusedBorderColor = AppInputBorder,
                        focusedBorderColor = BlueAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("country_search_input")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredCountries, key = { it.dialCode + it.countryName }) { country ->
                        val isSelected = country.dialCode == currentSelected.dialCode && country.countryName == currentSelected.countryName
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) BlueSoft else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, BlueAccent) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(country) }
                                .testTag("country_item_${country.dialCode.replace("+", "")}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(country.flagEmoji, fontSize = 22.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = country.countryName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) BlueAccent else Slate800,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "E.164: ${country.dialCode} (standard ${country.minLength} digits)",
                                        color = Slate500,
                                        fontSize = 11.sp
                                    )
                                }
                                Text(
                                    text = country.dialCode,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) BlueAccent else Navy800,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
