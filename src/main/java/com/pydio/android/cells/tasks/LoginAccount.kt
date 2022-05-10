package com.pydio.android.cells.tasks

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import com.pydio.cells.transport.ServerURLImpl
import kotlinx.coroutines.launch
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.AuthActivity
import com.pydio.android.cells.CellsApp
import com.pydio.android.cells.db.accounts.RSessionView
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.SessionFactory

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
            val toAuthIntent = Intent(context, AuthActivity::class.java)
            toAuthIntent.putExtra(AppNames.EXTRA_SERVER_URL, serverURL.toJson())
            toAuthIntent.putExtra(AppNames.EXTRA_SERVER_IS_LEGACY, true)
            toAuthIntent.putExtra(AppNames.EXTRA_AFTER_AUTH_ACTION, next)
            startActivity(context, toAuthIntent, null)

        } else {
            authService.createOAuthIntent(
                sessionFactory, serverURL, next
            )?.let {
                startActivity(context, it, null)
            } ?: run {
                Log.e("LoginAccount()", "Could not create OAuth intent for ${serverURL.url}")
            }
        }
    }

    return true
}
