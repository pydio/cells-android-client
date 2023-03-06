package com.pydio.android.cells.ui.system.models

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.JobService

/**
 * Holds a list of recent jobs and provides various clean features (still to implement).
 */
class JobListVM(
    val jobService: JobService
) : ViewModel() {
    val jobs = jobService.listLiveJobs(true)
}
