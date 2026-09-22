package com.locationjoystick.license

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LicenseManager"

@Singleton
class LicenseManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val licenseStorage: LicenseStorage,
        private val keygenClient: KeygenClient,
    ) {
        private val _licenseState = MutableStateFlow(LicenseState())
        val licenseState: StateFlow<LicenseState> = _licenseState.asStateFlow()

        val fingerprint: String by lazy {
            DeviceFingerprint.getFingerprint(context)
        }

        /**
         * Checks the local saved license key on app startup.
         * First-version requirement: Must validate with Keygen online.
         */
        suspend fun checkSavedLicense(): Boolean =
            withContext(Dispatchers.IO) {
                _licenseState.value = _licenseState.value.copy(status = LicenseStatus.CHECKING, fingerprint = fingerprint)

                val savedKey = licenseStorage.getLicenseKey()
                if (savedKey.isNullOrEmpty()) {
                    Log.d(TAG, "No local license key saved")
                    _licenseState.value =
                        _licenseState.value.copy(
                            status = LicenseStatus.INACTIVE,
                            savedLicenseKey = null,
                            maskedLicenseKey = null,
                            errorMessage = null,
                            fingerprint = fingerprint,
                        )
                    return@withContext false
                }

                val nonNullKey: String = savedKey
                val maskedKey = if (nonNullKey.length <= 8) nonNullKey else "${nonNullKey.take(4)}...${nonNullKey.takeLast(4)}"
                Log.d(TAG, "Checking saved license key: $maskedKey")

                val result = keygenClient.validateKey(nonNullKey, fingerprint)
                when (result) {
                    is LicenseValidationResult.Valid -> {
                        _licenseState.value =
                            LicenseState(
                                status = LicenseStatus.ACTIVE,
                                savedLicenseKey = savedKey,
                                maskedLicenseKey = maskedKey,
                                expiry = result.expiry,
                                errorMessage = null,
                                fingerprint = fingerprint,
                            )
                        true
                    }

                    is LicenseValidationResult.NeedsMachineActivation -> {
                        // Attempt machine activation
                        val activation = keygenClient.activateMachine(savedKey, result.licenseId, fingerprint)
                        when (activation) {
                            is MachineActivationResult.Success -> {
                                // Re-validate after activation
                                val reval = keygenClient.validateKey(savedKey, fingerprint)
                                if (reval is LicenseValidationResult.Valid) {
                                    _licenseState.value =
                                        LicenseState(
                                            status = LicenseStatus.ACTIVE,
                                            savedLicenseKey = savedKey,
                                            maskedLicenseKey = maskedKey,
                                            expiry = reval.expiry,
                                            errorMessage = null,
                                            fingerprint = fingerprint,
                                        )
                                    true
                                } else {
                                    _licenseState.value =
                                        _licenseState.value.copy(
                                            status = LicenseStatus.ERROR,
                                            errorMessage = "设备绑定成功但重新验证失败，请重试",
                                        )
                                    false
                                }
                            }

                            is MachineActivationResult.Failed -> {
                                _licenseState.value =
                                    _licenseState.value.copy(
                                        status = LicenseStatus.ERROR,
                                        errorMessage = activation.chineseMessage,
                                    )
                                false
                            }

                            is MachineActivationResult.NetworkError -> {
                                _licenseState.value =
                                    _licenseState.value.copy(
                                        status = LicenseStatus.ERROR,
                                        errorMessage = activation.message,
                                    )
                                false
                            }
                        }
                    }

                    is LicenseValidationResult.Invalid -> {
                        // License expired, suspended, or invalid -> clear local key
                        licenseStorage.clearLicenseKey()
                        _licenseState.value =
                            _licenseState.value.copy(
                                status = LicenseStatus.ERROR,
                                savedLicenseKey = null,
                                maskedLicenseKey = null,
                                errorMessage = result.chineseMessage,
                                fingerprint = fingerprint,
                            )
                        false
                    }

                    is LicenseValidationResult.NetworkError -> {
                        _licenseState.value =
                            _licenseState.value.copy(
                                status = LicenseStatus.ERROR,
                                errorMessage = result.message,
                                fingerprint = fingerprint,
                            )
                        false
                    }
                }
            }

        /**
         * User-initiated license activation with [userEnteredKey].
         */
        suspend fun activateLicense(userEnteredKey: String): Boolean =
            withContext(Dispatchers.IO) {
                val cleanKey = userEnteredKey.trim()
                if (cleanKey.isEmpty()) {
                    _licenseState.value =
                        _licenseState.value.copy(
                            status = LicenseStatus.ERROR,
                            errorMessage = "请输入有效的授权卡密",
                        )
                    return@withContext false
                }

                _licenseState.value =
                    _licenseState.value.copy(
                        status = LicenseStatus.CHECKING,
                        errorMessage = null,
                        fingerprint = fingerprint,
                    )

                val valResult = keygenClient.validateKey(cleanKey, fingerprint)
                when (valResult) {
                    is LicenseValidationResult.Valid -> {
                        licenseStorage.saveLicenseKey(cleanKey)
                        val maskedKey = if (cleanKey.length <= 8) cleanKey else "${cleanKey.take(4)}...${cleanKey.takeLast(4)}"
                        _licenseState.value =
                            LicenseState(
                                status = LicenseStatus.ACTIVE,
                                savedLicenseKey = cleanKey,
                                maskedLicenseKey = maskedKey,
                                expiry = valResult.expiry,
                                errorMessage = null,
                                fingerprint = fingerprint,
                            )
                        true
                    }

                    is LicenseValidationResult.NeedsMachineActivation -> {
                        val activation = keygenClient.activateMachine(cleanKey, valResult.licenseId, fingerprint)
                        when (activation) {
                            is MachineActivationResult.Success -> {
                                val reval = keygenClient.validateKey(cleanKey, fingerprint)
                                if (reval is LicenseValidationResult.Valid) {
                                    licenseStorage.saveLicenseKey(cleanKey)
                                    val maskedKey = if (cleanKey.length <= 8) cleanKey else "${cleanKey.take(4)}...${cleanKey.takeLast(4)}"
                                    _licenseState.value =
                                        LicenseState(
                                            status = LicenseStatus.ACTIVE,
                                            savedLicenseKey = cleanKey,
                                            maskedLicenseKey = maskedKey,
                                            expiry = reval.expiry,
                                            errorMessage = null,
                                            fingerprint = fingerprint,
                                        )
                                    true
                                } else {
                                    _licenseState.value =
                                        _licenseState.value.copy(
                                            status = LicenseStatus.ERROR,
                                            errorMessage = "绑定设备成功但重新验证失败，请重新尝试",
                                        )
                                    false
                                }
                            }

                            is MachineActivationResult.Failed -> {
                                _licenseState.value =
                                    _licenseState.value.copy(
                                        status = LicenseStatus.ERROR,
                                        errorMessage = activation.chineseMessage,
                                    )
                                false
                            }

                            is MachineActivationResult.NetworkError -> {
                                _licenseState.value =
                                    _licenseState.value.copy(
                                        status = LicenseStatus.ERROR,
                                        errorMessage = activation.message,
                                    )
                                false
                            }
                        }
                    }

                    is LicenseValidationResult.Invalid -> {
                        _licenseState.value =
                            _licenseState.value.copy(
                                status = LicenseStatus.ERROR,
                                errorMessage = valResult.chineseMessage,
                            )
                        false
                    }

                    is LicenseValidationResult.NetworkError -> {
                        _licenseState.value =
                            _licenseState.value.copy(
                                status = LicenseStatus.ERROR,
                                errorMessage = valResult.message,
                            )
                        false
                    }
                }
            }

        /**
         * Clears the current license.
         */
        fun logoutLicense() {
            licenseStorage.clearLicenseKey()
            _licenseState.value =
                LicenseState(
                    status = LicenseStatus.INACTIVE,
                    fingerprint = fingerprint,
                )
        }
    }
