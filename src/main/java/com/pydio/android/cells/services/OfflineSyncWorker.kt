package com.pydio.android.cells.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.pydio.android.cells.AppNames
import com.pydio.cells.transport.ClientData
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OfflineSyncWorker(
    private val accountService: AccountService,
    private val nodeService: NodeService,
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val logTag = OfflineSyncWorker::class.simpleName

    private val prefs: CellsPreferences by inject()

    companion object {
        const val WORK_NAME = "OfflineSyncWorker"
    }

    override suspend fun doWork(): Result {

        if (hasBeenMigrated()){
            nodeService.runFullSync("Worker")
        }

        val d = Data.Builder().putString("yes", "no").build()
        return Result.success(d)
    }

    // Dirty tweak to prevent the first sync to be launched before the migration from v2 has correctly run
    private fun hasBeenMigrated(): Boolean{
        return prefs.getInt(AppNames.PREF_KEY_INSTALLED_VERSION_CODE) > 107
    }

}
