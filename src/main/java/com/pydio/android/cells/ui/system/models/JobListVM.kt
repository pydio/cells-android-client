package com.pydio.android.cells.ui.system.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pydio.android.cells.services.JobService
import kotlinx.coroutines.launch

/**
 * Holds a list of recent jobs and provides cleaning features.
 */
class JobListVM(
    val jobService: JobService
) : ViewModel() {

    val jobs = jobService.listLiveJobs(true)

    fun clearTerminated() {
        viewModelScope.launch {
            jobService.clearTerminated()
        }
    }
}
