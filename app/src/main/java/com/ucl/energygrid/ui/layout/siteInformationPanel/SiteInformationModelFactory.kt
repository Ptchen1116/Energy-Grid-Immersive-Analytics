package com.ucl.energygrid.ui.layout.siteInformationPanel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ucl.energygrid.data.remote.apis.PinApi

class SiteInformationViewModelFactory(
    private val userId: Int,
    private val mineId: Int,
    private val api: PinApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SiteInformationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SiteInformationViewModel(userId, mineId, api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}