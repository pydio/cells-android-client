package com.pydio.android.cells.ui.home

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.JobService

/**
 * Holds a list of recent jobs and provides various clean features (still to implement).
 */
class JobListViewModel(
    jobService: JobService
) : ViewModel() {
    val jobs = jobService.listRootJobs()
}
