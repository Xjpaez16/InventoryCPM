package com.example.inventorycpm.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras

/**
 * Factory para FacturaReviewViewModel que pasa el facturaId al SavedStateHandle.
 */
class FacturaReviewViewModelFactory(
    private val facturaId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        val savedStateHandle = extras.createSavedStateHandle()
        savedStateHandle["facturaId"] = facturaId
        return FacturaReviewViewModel(application, savedStateHandle) as T
    }
}
