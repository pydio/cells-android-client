package com.pydio.android.cells.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.pydio.cells.utils.Log
import org.koin.core.component.KoinComponent
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.utils.asFormattedString
import com.pydio.android.cells.utils.getCurrentDateTime

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

        val startTS = getCurrentDateTime().asFormattedString("yyyy-MM-dd HH:mm")
        Log.i(logTag, "... Launching full re-sync in background at $startTS")

        for (session in accountService.listLiveSessions(false)) {
            if (session.lifecycleState != AppNames.LIFECYCLE_STATE_PAUSED
                && session.authStatus == AppNames.AUTH_STATUS_CONNECTED
            ) {
                Log.i(logTag, "... Launching sync for ${session.getStateID()}")
                nodeService.syncAll(session.getStateID())
            }
        }
        val endTS = getCurrentDateTime().asFormattedString("yyyy-MM-dd HH:mm")
        Log.i(logTag, "... Full re-sync terminated at $endTS")

        val d = Data.Builder().putString("yes", "no").build()
        return Result.success(d)
    }
}
