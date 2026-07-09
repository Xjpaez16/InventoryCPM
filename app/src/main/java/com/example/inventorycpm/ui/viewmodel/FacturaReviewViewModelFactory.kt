package com.example.inventorycpm.ui.viewmodel

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory para FacturaReviewViewModel que pasa el facturaId al SavedStateHandle.
 */
class FacturaReviewViewModelFactory(
    private val facturaId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Usar SavedStateHandle con facturaId
        val application = throw UnsupportedOperationException(
            "Use ViewModelProvider with SavedStateHandle instead."
        )
    }
}
