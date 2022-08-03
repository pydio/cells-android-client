package com.pydio.android.cells

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import com.pydio.android.cells.databinding.ActivityAuthBinding
import com.pydio.android.cells.services.AuthService
import com.pydio.android.cells.ui.auth.ServerUrlFragmentDirections
import com.pydio.cells.utils.Str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

/** Centralizes authentication processes. */
class AuthActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private val logTag = AuthActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityAuthBinding>(this, R.layout.activity_auth)
    }

    override fun onResume() {
        Log.d(logTag, "onResume, intent: $intent")
        super.onResume()
        handleIntent(intent)
    }

    override fun onPause() {
        Log.d(logTag, "onPause, intent: $intent")
        super.onPause()
    }

    private fun handleIntent(inIntent: Intent) {
        if (Intent.ACTION_VIEW == intent.action) {
            val uri = inIntent.data ?: return
            val code = uri.getQueryParameter(AppNames.QUERY_KEY_CODE)
            val state = uri.getQueryParameter(AppNames.QUERY_KEY_STATE)

            if (code != null && state != null) {
                val action = ServerUrlFragmentDirections.actionServerUrlToOauthFlow(null)
                findNavController(R.id.auth_fragment_host).navigate(action)
                return
            }
        }

        if (intent.hasExtra(AppKeys.EXTRA_SERVER_URL) && Str.notEmpty(intent.getStringExtra(AppKeys.EXTRA_SERVER_URL))) {
            val urlStr: String = intent.getStringExtra(AppKeys.EXTRA_SERVER_URL)!!
            if (intent.getBooleanExtra(AppKeys.EXTRA_SERVER_IS_LEGACY, false)) {
                // TODO double check that listing accounts is the relevant default here
                val next: String = intent.getStringExtra(AppKeys.EXTRA_AFTER_AUTH_ACTION)
                    ?: AuthService.NEXT_ACTION_ACCOUNTS
                val action = ServerUrlFragmentDirections.actionServerUrlToP8Creds(urlStr, next)
                findNavController(R.id.auth_fragment_host).navigate(action)
            } else {
                val action = ServerUrlFragmentDirections.actionServerUrlToOauthFlow(urlStr)
                findNavController(R.id.auth_fragment_host).navigate(action)
            }
        }
    }
}
