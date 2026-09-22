package com.locationjoystick.license

enum class LicenseStatus {
    UNKNOWN,
    CHECKING,
    ACTIVE,
    INACTIVE,
    ERROR,
}

sealed interface LicenseValidationResult {
    data class Valid(
        val licenseId: String,
        val expiry: String?,
        val detail: String,
    ) : LicenseValidationResult

    data class NeedsMachineActivation(
        val licenseId: String,
        val detail: String,
    ) : LicenseValidationResult

    data class Invalid(
        val code: String,
        val detail: String,
        val chineseMessage: String,
    ) : LicenseValidationResult

    data class NetworkError(
        val message: String,
    ) : LicenseValidationResult
}

sealed interface MachineActivationResult {
    data class Success(
        val machineId: String,
    ) : MachineActivationResult

    data class Failed(
        val code: String,
        val detail: String,
        val chineseMessage: String,
    ) : MachineActivationResult

    data class NetworkError(
        val message: String,
    ) : MachineActivationResult
}

data class LicenseState(
    val status: LicenseStatus = LicenseStatus.UNKNOWN,
    val savedLicenseKey: String? = null,
    val maskedLicenseKey: String? = null,
    val expiry: String? = null,
    val errorMessage: String? = null,
    val fingerprint: String = "",
)
