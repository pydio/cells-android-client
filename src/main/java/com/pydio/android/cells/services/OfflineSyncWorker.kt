package com.pydio.android.cells.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.utils.asFormattedString
import com.pydio.android.cells.utils.getCurrentDateTime
import com.pydio.cells.utils.Log
import org.koin.core.component.KoinComponent

class OfflineSyncWorker(
    private val accountService: AccountService,
    private val nodeService: NodeService,
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val logTag = OfflineSyncWorker::class.simpleName

    companion object {
        const val WORK_NAME = "OfflineSyncWorker"
    }

    override suspend fun doWork(): Result {

      nodeService.runFullSync()
        val d = Data.Builder().putString("yes", "no").build()
        return Result.success(d)
    }
}
