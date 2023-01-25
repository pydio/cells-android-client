package com.pydio.android.cells.ui.models

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.pydio.android.cells.AppKeys
import com.pydio.android.cells.AuthActivity
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.cells.transport.ServerURLImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AuthVM(
    private val authService: AuthService,
    private val sessionFactory: SessionFactory,
) : ViewModel() {

    fun startAuthProcess(
        context: Context,
        scope: CoroutineScope,
        // sessionView: RSessionView,
        isLegacy: Boolean,
        url: String,
        tlsMode: Int,
        next: String,
    ) {
        scope.launch {
            // TODO clean this when implementing custom certificate acceptance.
            val serverURL = ServerURLImpl.fromAddress(url, tlsMode == 1)
            if (isLegacy) {
                val toAuthIntent = Intent(context, AuthActivity::class.java)
                toAuthIntent.putExtra(AppKeys.EXTRA_SERVER_URL, serverURL.toJson())
                toAuthIntent.putExtra(AppKeys.EXTRA_SERVER_IS_LEGACY, true)
                toAuthIntent.putExtra(AppKeys.EXTRA_AFTER_AUTH_ACTION, next)
                ContextCompat.startActivity(context, toAuthIntent, null)
            } else {
                authService.createOAuthIntent(
                    sessionFactory, serverURL, next
                )?.let {
                    ContextCompat.startActivity(context, it, null)
                } ?: run {
                    Log.e("LoginAccount()", "Could not create OAuth intent for ${serverURL.url}")
                }
            }
        }
    }
}
