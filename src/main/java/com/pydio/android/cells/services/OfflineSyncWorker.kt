package com.pydio.android.cells.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
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

        // TODO add all possible params
        nodeService.runFullSync("Worker")
        val d = Data.Builder().putString("yes", "no").build()
        return Result.success(d)
    }
}
