package com.pydio.android.cells.tasks

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.pydio.android.cells.AppKeys
import com.pydio.cells.transport.ServerURLImpl
import kotlinx.coroutines.launch
import com.pydio.android.cells.LoginActivity
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.SessionFactory

@Deprecated("Rather use AuthVM.startAuthProcess()")
fun loginAccount(
    context: Context,
    authService: AuthService,
    sessionFactory: SessionFactory,
    sessionView: RSessionView,
    next: String,
): Boolean {

    CellsApp.instance.appScope.launch {

        // TODO clean this when implementing custom certificate acceptance.
        val serverURL = ServerURLImpl.fromAddress(sessionView.url, sessionView.tlsMode == 1)

        if (sessionView.isLegacy) {
            val toAuthIntent = Intent(context, LoginActivity::class.java)
            toAuthIntent.putExtra(AppKeys.EXTRA_SERVER_URL, serverURL.toJson())
            toAuthIntent.putExtra(AppKeys.EXTRA_SERVER_IS_LEGACY, true)
            toAuthIntent.putExtra(AppKeys.EXTRA_AFTER_AUTH_ACTION, next)
            startActivity(context, toAuthIntent, null)

        } else {
            // TODO this might throw an SDK error. handle it or crash.
            authService.generateOAuthFlowUri(
                sessionFactory, serverURL, next
            )?.let {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = it
                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
                startActivity(context, intent, null)
            } ?: run {
                Log.e("LoginAccount()", "Could not create OAuth intent for ${serverURL.url}")
            }
        }
    }

    return true
}
