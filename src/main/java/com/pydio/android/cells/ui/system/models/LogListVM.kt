package com.pydio.android.cells.ui.system.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.JobService
import kotlinx.coroutines.launch

/**
 * Holds a list of recent logs and provides various clean features (still to implement).
 */
class LogListVM(
    private val jobService: JobService
) : ViewModel() {

    val logs = jobService.listLogs()

    fun clearAllLogs() {
        viewModelScope.launch {
            jobService.clearAllLogs()
        }
    }
}
