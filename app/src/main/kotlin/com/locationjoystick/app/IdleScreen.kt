package com.locationjoystick.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.locationjoystick.app.R
import com.locationjoystick.core.common.util.isMockLocationEnabled
import com.locationjoystick.core.common.util.isOverlayPermissionGranted
import com.locationjoystick.core.designsystem.LjIcons
import com.locationjoystick.core.designsystem.LjSpacing
import com.locationjoystick.core.designsystem.LjSuccess
import com.locationjoystick.core.designsystem.LjWarning
import com.locationjoystick.core.designsystem.component.LjCard
import com.locationjoystick.core.designsystem.component.LjFeatureShortcut
import com.locationjoystick.core.designsystem.component.LjLocationItem
import com.locationjoystick.core.designsystem.component.LjPrimaryButton
import com.locationjoystick.core.designsystem.component.LjScaffold
import com.locationjoystick.core.designsystem.component.LjSectionLabel
import com.locationjoystick.core.designsystem.component.LjStatusBadge
import com.locationjoystick.core.designsystem.component.LjStatusTone
import com.locationjoystick.core.designsystem.component.LjTextButton
import com.locationjoystick.core.location.rememberSpoofToggleState

internal const val IDLE_ROUTE = "idle"

@Composable
internal fun IdleScreen(
    onOpenDrawer: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToRoutes: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
) {
    val spoofToggle = rememberSpoofToggleState()
    val context = LocalContext.current
    val locationGranted =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    val permissionsReady =
        locationGranted && isMockLocationEnabled(context) && isOverlayPermissionGranted(context)

    LjScaffold(
        title = stringResource(R.string.drawer_home),
        isSpoofing = spoofToggle.isSpoofing,
        onToggleSpoofing = spoofToggle.onToggle,
        locationLabel = spoofToggle.locationLabel,
        onNavigationClick = onOpenDrawer,
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = LjSpacing.page, vertical = LjSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LjSpacing.md),
        ) {
            StatusCard(
                isSpoofing = spoofToggle.isSpoofing,
                locationLabel = spoofToggle.locationLabel,
                onToggle = spoofToggle.onToggle,
            )

            LjSectionLabel(text = stringResource(R.string.idle_quick_actions))
            Column(verticalArrangement = Arrangement.spacedBy(LjSpacing.sm + 4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(LjSpacing.sm + 4.dp)) {
                    LjFeatureShortcut(
                        icon = LjIcons.Map,
                        label = stringResource(R.string.drawer_map),
                        onClick = onNavigateToMap,
                        modifier = Modifier.weight(1f),
                        iconContentDescription = stringResource(R.string.drawer_map_cd),
                    )
                    LjFeatureShortcut(
                        icon = LjIcons.Route,
                        label = stringResource(R.string.drawer_routes),
                        onClick = onNavigateToRoutes,
                        modifier = Modifier.weight(1f),
                        iconContentDescription = stringResource(R.string.drawer_routes_cd),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(LjSpacing.sm + 4.dp)) {
                    LjFeatureShortcut(
                        icon = LjIcons.Favorite,
                        label = stringResource(R.string.drawer_favorites),
                        onClick = onNavigateToFavorites,
                        modifier = Modifier.weight(1f),
                        iconContentDescription = stringResource(R.string.drawer_favorites_cd),
                    )
                    LjFeatureShortcut(
                        icon = LjIcons.Settings,
                        label = stringResource(R.string.drawer_settings),
                        onClick = onNavigateToSettings,
                        modifier = Modifier.weight(1f),
                        iconContentDescription = stringResource(R.string.drawer_settings_cd),
                    )
                }
            }

            if (spoofToggle.locationLabel != null) {
                LjSectionLabel(text = stringResource(R.string.idle_last_location))
                LjCard {
                    LjLocationItem(
                        name = spoofToggle.locationLabel.orEmpty(),
                        onClick = onNavigateToMap,
                    )
                }
            }

            PermissionStatusBar(
                permissionsReady = permissionsReady,
                onFix = onNavigateToOnboarding,
            )
        }
    }
}

@Composable
private fun StatusCard(
    isSpoofing: Boolean,
    locationLabel: String?,
    onToggle: () -> Unit,
) {
    LjCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LjSpacing.page),
            verticalArrangement = Arrangement.spacedBy(LjSpacing.md),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LjSpacing.md),
            ) {
                val accent = MaterialTheme.colorScheme.primary
                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSpoofing) {
                                    LjSuccess.copy(alpha = 0.16f)
                                } else {
                                    accent.copy(alpha = 0.14f)
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = LjIcons.LocationOn,
                        contentDescription = stringResource(R.string.idle_status_card_cd),
                        tint = if (isSpoofing) LjSuccess else accent,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(LjSpacing.xs)) {
                    LjStatusBadge(
                        text =
                            stringResource(
                                if (isSpoofing) {
                                    R.string.idle_status_spoofing
                                } else {
                                    R.string.idle_status_idle
                                },
                            ),
                        tone = if (isSpoofing) LjStatusTone.Success else LjStatusTone.Neutral,
                    )
                    Text(
                        text = locationLabel ?: stringResource(R.string.idle_status_no_location),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            LjPrimaryButton(
                text =
                    stringResource(
                        if (isSpoofing) {
                            com.locationjoystick.core.designsystem.R.string.top_bar_stop
                        } else {
                            com.locationjoystick.core.designsystem.R.string.common_start
                        },
                    ),
                onClick = onToggle,
            )
        }
    }
}

@Composable
private fun PermissionStatusBar(
    permissionsReady: Boolean,
    onFix: () -> Unit,
) {
    val toneColor = if (permissionsReady) LjSuccess else LjWarning
    LjCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LjSpacing.md, vertical = LjSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LjSpacing.sm + 4.dp),
        ) {
            Icon(
                imageVector = if (permissionsReady) LjIcons.CheckCircle else LjIcons.Warning,
                contentDescription = null,
                tint = toneColor,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text =
                    stringResource(
                        if (permissionsReady) {
                            R.string.idle_permissions_ok
                        } else {
                            R.string.idle_permissions_missing
                        },
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (!permissionsReady) {
                LjTextButton(onClick = onFix) {
                    Text(
                        text = stringResource(R.string.idle_permissions_fix),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
