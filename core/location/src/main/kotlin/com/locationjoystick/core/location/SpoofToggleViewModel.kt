package com.locationjoystick.core.location

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.locationjoystick.core.common.constants.AppConstants
import com.locationjoystick.core.common.util.wgs84ToGcj02
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject

private const val TAG = "SpoofToggleViewModel"

/**
 * Thin Hilt ViewModel wrapping [MapController.isSpoofing] / [MapController.toggleSpoofing] so any
 * screen's top bar can drive the global start/stop spoofing state without each feature's own
 * ViewModel needing to depend on [MapController] directly.
 *
 * Also reverse-geocodes the current position via Nominatim and exposes [locationLabel] so the
 * "Start" button can show a hint like "Start · Paris, France".
 */
@HiltViewModel
class SpoofToggleViewModel
    @Inject
    constructor(
        private val mapController: MapController,
    ) : ViewModel() {
        val isSpoofing: StateFlow<Boolean> = mapController.isSpoofing

        private val _locationLabel = MutableStateFlow<String?>(null)
        val locationLabel: StateFlow<String?> = _locationLabel.asStateFlow()

        init {
            viewModelScope.launch {
                mapController.sharedState
                    .map { it.currentPosition }
                    .distinctUntilChanged { old, new ->
                        if (old == null && new == null) {
                            true
                        } else if (old == null || new == null) {
                            false
                        } else {
                            Math.round(old.latitude * 100) == Math.round(new.latitude * 100) &&
                                Math.round(old.longitude * 100) == Math.round(new.longitude * 100)
                        }
                    }.collect { pos ->
                        if (pos != null) {
                            val label = reverseGeocode(pos.latitude, pos.longitude)
                            _locationLabel.value = label ?: String.format(Locale.US, "%.4f, %.4f", pos.latitude, pos.longitude)
                        } else {
                            _locationLabel.value = null
                        }
                    }
            }
        }

        fun toggle() {
            mapController.toggleSpoofing()
        }

        private suspend fun reverseGeocode(
            lat: Double,
            lon: Double,
        ): String? =
            withContext(Dispatchers.IO) {
                // Tier 1: Amap ReGeo API
                try {
                    val gcj = wgs84ToGcj02(lat, lon)
                    val key = "cfc8fa33f036d939984b414aaa1400bd"
                    val url = URL("https://restapi.amap.com/v3/geocode/regeo?location=${gcj.longitude},${gcj.latitude}&key=$key")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "coco/1.0 (com.locationjoystick.app)")
                    conn.connectTimeout = 3000
                    conn.readTimeout = 3000
                    try {
                        if (conn.responseCode == 200) {
                            val json = JSONObject(conn.inputStream.bufferedReader().readText())
                            if (json.optString("status") == "1") {
                                val regeo = json.optJSONObject("regeocode")
                                val formatted = regeo?.optString("formatted_address")
                                if (!formatted.isNullOrEmpty() && formatted != "[]") {
                                    return@withContext formatted
                                }
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Amap reverse geocode failed", e)
                }

                // Tier 2: Nominatim Reverse API
                try {
                    val url = URL("${AppConstants.NominatimConstants.REVERSE_URL}?lat=$lat&lon=$lon&format=json&accept-language=zh-CN,zh")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestProperty("User-Agent", "coco/1.0 (com.locationjoystick.app)")
                    conn.connectTimeout = AppConstants.NominatimConstants.CONNECT_TIMEOUT_MS
                    conn.readTimeout = AppConstants.NominatimConstants.READ_TIMEOUT_MS
                    try {
                        if (conn.responseCode == 200) {
                            val json = JSONObject(conn.inputStream.bufferedReader().readText())
                            val address = json.optJSONObject("address")
                            if (address != null) {
                                val locality =
                                    address.optString("city").takeIf { it.isNotEmpty() }
                                        ?: address.optString("town").takeIf { it.isNotEmpty() }
                                        ?: address.optString("village").takeIf { it.isNotEmpty() }
                                        ?: address.optString("municipality").takeIf { it.isNotEmpty() }
                                val country = address.optString("country").takeIf { it.isNotEmpty() }
                                if (locality != null) {
                                    return@withContext if (country != null) "$locality, $country" else locality
                                }
                            }
                        }
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Nominatim reverse geocode failed", e)
                }

                null
            }
    }

data class SpoofToggleState(
    val isSpoofing: Boolean,
    val onToggle: () -> Unit,
    val locationLabel: String? = null,
)

/**
 * Collects the global start/stop spoofing state from [SpoofToggleViewModel] so every screen's
 * `LjScaffold` call site doesn't need to repeat `hiltViewModel()` + `collectAsStateWithLifecycle()`.
 */
@Composable
fun rememberSpoofToggleState(): SpoofToggleState {
    val viewModel: SpoofToggleViewModel = hiltViewModel()
    val isSpoofing by viewModel.isSpoofing.collectAsStateWithLifecycle()
    val locationLabel by viewModel.locationLabel.collectAsStateWithLifecycle()
    return SpoofToggleState(isSpoofing, viewModel::toggle, locationLabel)
}
