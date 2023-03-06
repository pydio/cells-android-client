package com.pydio.android.cells.ui.system.models

import androidx.lifecycle.ViewModel
import com.pydio.android.cells.services.JobService

/**
 * Holds a list of recent logs and provides various clean features (still to implement).
 */
class LogListVM(
    val jobService: JobService
) : ViewModel() {
    val logs = jobService.listLogs()
}
