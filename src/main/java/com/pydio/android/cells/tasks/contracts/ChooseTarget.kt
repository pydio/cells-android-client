package com.pydio.android.cells.tasks.contracts

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.SelectTargetActivity
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log

class SelectTargetContract : ActivityResultContract<Pair<StateID, String>, StateID?>() {

    private val logTag = SelectTargetContract::class.simpleName

    // TODO rather start with a _parent_ State
    override fun createIntent(context: Context, input: Pair<StateID, String>): Intent {
        Log.e(logTag, "Creating intent")
        val intent = Intent(context, SelectTargetActivity::class.java)
        intent.action = AppNames.ACTION_CHOOSE_TARGET
        intent.putExtra(AppKeys.EXTRA_STATE, input.first.id)
        intent.putExtra(AppKeys.EXTRA_ACTION_CONTEXT, input.second)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): StateID? {
        Log.e(logTag, "parseResult, code: $resultCode")
        return when (resultCode) {
            RESULT_OK -> StateID.fromId(intent?.getStringExtra(AppKeys.EXTRA_STATE))
            else -> null
        }
    }
}
