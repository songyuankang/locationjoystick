package com.locationjoystick.core.designsystem.component

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.locationjoystick.core.common.constants.AppConstants
import com.locationjoystick.core.common.util.parseRawLatLng
import com.locationjoystick.core.designsystem.LjSpacing
import com.locationjoystick.core.designsystem.R
import com.locationjoystick.core.model.RecentSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private const val TAG = "NominatimSearchBar"

@Composable
fun NominatimSearchBar(
    onLocationSelected: (lat: Double, lon: Double, displayName: String) -> Unit,
    modifier: Modifier = Modifier,
    recentSearches: List<RecentSearch> = emptyList(),
    onSearchCommitted: ((displayName: String, lat: Double, lon: Double) -> Unit)? = null,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<NominatimResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    LaunchedEffect(query) {
        val rawCoords = parseRawLatLng(query)
        if (rawCoords != null) {
            results =
                listOf(
                    NominatimResult(
                        lat = rawCoords.latitude,
                        lon = rawCoords.longitude,
                        displayName = "Go to ${rawCoords.latitude}, ${rawCoords.longitude}",
                    ),
                )
            isLoading = false
            return@LaunchedEffect
        }
        if (query.length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        delay(AppConstants.NominatimConstants.SEARCH_DEBOUNCE_MS)
        isLoading = true
        withContext(Dispatchers.IO) {
            var parsed = querySystemGeocoder(context, query)
            if (parsed.isEmpty()) {
                parsed = queryNominatim(query)
                if (parsed.isEmpty() && (query.contains("县") || query.contains("市") || query.contains("区"))) {
                    val shortTerm = query.substringAfter("县").substringAfter("市").substringAfter("区").trim()
                    if (shortTerm.length >= 2) {
                        parsed = queryNominatim(shortTerm)
                    }
                }
            }
            results = parsed
            isLoading = false
        }
    }

    val showRecent = query.isEmpty() && recentSearches.isNotEmpty()
    val showResults = results.isNotEmpty() || (query.length >= 2) || showRecent

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
            placeholder = { Text(stringResource(R.string.search_bar_search_location)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        results = emptyList()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions =
                KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                    },
                ),
            shape =
                if (showResults) {
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                } else {
                    RoundedCornerShape(12.dp)
                },
        )

        if (showResults) {
            HorizontalDivider()
            if (isLoading && query.length >= 2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LjSpacing.md, vertical = 14.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.search_bar_searching),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (showRecent) {
                Text(
                    text = stringResource(R.string.search_bar_recent),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = LjSpacing.md, vertical = 6.dp),
                )
                recentSearches.forEach { recent ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    onLocationSelected(recent.lat, recent.lon, recent.displayName)
                                    onSearchCommitted?.invoke(recent.displayName, recent.lat, recent.lon)
                                    query = ""
                                }.padding(horizontal = LjSpacing.md, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = recent.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider()
                }
            }
            if (results.isEmpty() && query.length >= 2 && !isLoading) {
                Text(
                    text = stringResource(R.string.search_bar_no_results_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LjSpacing.md, vertical = 12.dp),
                )
            }
            results.forEach { result ->
                Text(
                    text = result.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                keyboardController?.hide()
                                onLocationSelected(result.lat, result.lon, result.displayName)
                                onSearchCommitted?.invoke(result.displayName, result.lat, result.lon)
                                query = ""
                                results = emptyList()
                            }.padding(horizontal = LjSpacing.md, vertical = 14.dp),
                )
                HorizontalDivider()
            }
        }
    }
}

private data class NominatimResult(
    val lat: Double,
    val lon: Double,
    val displayName: String,
)

private val NOMINATIM_MIRRORS =
    listOf(
        "https://nominatim.openstreetmap.org/search",
        "https://nominatim.kumi.systems/search",
        "https://nominatim.openstreetmap.de/search",
    )

private fun queryNominatim(searchTerm: String): List<NominatimResult> {
    val encoded = URLEncoder.encode(searchTerm, "UTF-8")
    for (baseUrl in NOMINATIM_MIRRORS) {
        try {
            val url = URL("$baseUrl?q=$encoded&format=json&limit=5&accept-language=zh-CN,zh;q=0.9,en;q=0.8")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "coco/1.0 (com.locationjoystick.app)")
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            try {
                if (conn.responseCode == 200) {
                    val responseText = conn.inputStream.bufferedReader().readText()
                    val array = JSONArray(responseText)
                    val parsed =
                        (0 until minOf(array.length(), 5)).mapNotNull { i ->
                            try {
                                val obj = array.getJSONObject(i)
                                NominatimResult(
                                    lat = obj.getDouble("lat"),
                                    lon = obj.getDouble("lon"),
                                    displayName = obj.getString("display_name"),
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }
                    if (parsed.isNotEmpty()) return parsed
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.e("NominatimSearchBar", "Search mirror $baseUrl failed for $searchTerm", e)
        }
    }
    return emptyList()
}

private suspend fun querySystemGeocoder(context: Context, query: String): List<NominatimResult> =
    withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext emptyList()
        try {
            val geocoder = Geocoder(context, Locale.SIMPLIFIED_CHINESE)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, 5) ?: emptyList()
            addresses.mapNotNull { addr ->
                if (addr.hasLatitude() && addr.hasLongitude()) {
                    val parts =
                        listOfNotNull(
                            addr.featureName,
                            addr.thoroughfare,
                            addr.subLocality,
                            addr.locality,
                            addr.subAdminArea,
                            addr.adminArea,
                            addr.countryName,
                        ).filter { it.isNotBlank() }.distinct()
                    val name = if (parts.isNotEmpty()) parts.joinToString(", ") else query
                    NominatimResult(
                        lat = addr.latitude,
                        lon = addr.longitude,
                        displayName = name,
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "System Geocoder failed for $query", e)
            emptyList()
        }
    }
