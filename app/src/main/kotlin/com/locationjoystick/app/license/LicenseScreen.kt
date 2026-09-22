package com.locationjoystick.app.license

import androidx.compose.ui.res.stringResource
import com.locationjoystick.app.R
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.locationjoystick.core.designsystem.LjIcons
import com.locationjoystick.core.designsystem.LjSpacing
import com.locationjoystick.core.designsystem.component.LjCard
import com.locationjoystick.core.designsystem.component.LjPrimaryButton
import com.locationjoystick.core.designsystem.component.LjScaffold
import com.locationjoystick.license.DeviceFingerprint
import com.locationjoystick.core.location.rememberSpoofToggleState
import com.locationjoystick.license.LicenseManager
import com.locationjoystick.license.LicenseState
import com.locationjoystick.license.LicenseStatus
import kotlinx.coroutines.launch

@Composable
fun LicenseScreen(
    licenseManager: LicenseManager,
    licenseState: LicenseState,
    onActivationSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val spoofToggle = rememberSpoofToggleState()
    var inputKey by remember { mutableStateOf("") }
    val isChecking = licenseState.status == LicenseStatus.CHECKING
    val successToastMsg = stringResource(R.string.license_activation_success_toast)

    LjScaffold(
        title = stringResource(R.string.license_screen_title),
        isSpoofing = spoofToggle.isSpoofing,
        onToggleSpoofing = spoofToggle.onToggle,
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = LjSpacing.page, vertical = LjSpacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            LjCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(LjSpacing.page),
                    verticalArrangement = Arrangement.spacedBy(LjSpacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Header Icon
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp),
                        )
                    }

                    Text(
                        text = stringResource(R.string.license_input_prompt),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Text(
                        text = stringResource(R.string.license_input_subprompt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(LjSpacing.xs))

                    // Input Text Field
                    OutlinedTextField(
                        value = inputKey,
                        onValueChange = { inputKey = it.trim() },
                        label = { Text(stringResource(R.string.license_key_label)) },
                        placeholder = { Text(stringResource(R.string.license_key_placeholder)) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(onDone = {
                                if (inputKey.isNotBlank() && !isChecking) {
                                    scope.launch {
                                        val ok = licenseManager.activateLicense(inputKey)
                                        if (ok) {
                                            Toast.makeText(context, successToastMsg, Toast.LENGTH_SHORT).show()
                                            onActivationSuccess()
                                        }
                                    }
                                }
                            }),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isChecking,
                    )

                    // Error or Status Message Display
                    val errMsg = licenseState.errorMessage
                    if (isChecking) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LjSpacing.sm),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text(
                                text = stringResource(R.string.license_verifying_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else if (!errMsg.isNullOrEmpty()) {
                        Text(
                            text = errMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                        )
                    } else if (licenseState.status == LicenseStatus.ACTIVE) {
                        Text(
                            text = stringResource(R.string.license_activated_success),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(LjSpacing.xs))

                    // Activate Button
                    val activeBtnText = if (isChecking) stringResource(R.string.license_verifying_btn) else stringResource(R.string.license_activate_btn)
                    LjPrimaryButton(
                        text = activeBtnText,
                        enabled = inputKey.isNotBlank() && !isChecking,
                        onClick = {
                            scope.launch {
                                val ok = licenseManager.activateLicense(inputKey)
                                if (ok) {
                                    Toast.makeText(context, successToastMsg, Toast.LENGTH_SHORT).show()
                                    onActivationSuccess()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(LjSpacing.sm))

                    // Device Fingerprint display + Copy Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.license_fingerprint_label, DeviceFingerprint.getMaskedFingerprint(licenseState.fingerprint)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        val copyToastMsg = stringResource(R.string.license_fingerprint_copied_toast)
                        IconButton(
                            onClick = {
                                DeviceFingerprint.copyToClipboard(context, licenseState.fingerprint)
                                Toast.makeText(context, copyToastMsg, Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.license_copy_fingerprint_cd),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
