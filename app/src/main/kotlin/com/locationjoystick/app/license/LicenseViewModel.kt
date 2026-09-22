package com.locationjoystick.app.license

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationjoystick.license.LicenseManager
import com.locationjoystick.license.LicenseState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LicenseViewModel
    @Inject
    constructor(
        val licenseManager: LicenseManager,
    ) : ViewModel() {
        val licenseState: StateFlow<LicenseState> = licenseManager.licenseState

        init {
            viewModelScope.launch {
                licenseManager.checkSavedLicense()
            }
        }
    }
