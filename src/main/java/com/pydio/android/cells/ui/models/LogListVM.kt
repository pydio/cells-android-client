package com.pydio.android.cells.ui.models

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.JobService

/**
 * Holds a list of recent jobs and provides various clean features (still to implement).
 */
class LogListVM(
    val jobService: JobService
) : ViewModel() {
    val logs = jobService.listLogs()
}
