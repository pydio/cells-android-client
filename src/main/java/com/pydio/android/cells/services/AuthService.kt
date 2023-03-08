package com.pydio.android.cells.services

import android.net.Uri
import android.util.Log
import com.pydio.android.cells.db.auth.AuthDB
import com.pydio.android.cells.db.auth.ROAuthState
import com.pydio.android.cells.utils.AndroidCustomEncoder
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.api.CustomEncoder
import com.pydio.cells.api.SDKException
import com.pydio.cells.api.ServerURL
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.ClientData
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.Token
import com.pydio.cells.transport.auth.credentials.JWTCredentials
import com.pydio.cells.transport.auth.jwt.IdToken
import com.pydio.cells.transport.auth.jwt.OAuthConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class AuthService(authDB: AuthDB) {

    private val tokenDao = authDB.tokenDao()
    private val legacyCredentialsDao = authDB.legacyCredentialsDao()
    private val authStateDao = authDB.authStateDao()

    private val logTag = "AuthService"
    private val encoder: CustomEncoder = AndroidCustomEncoder()

    companion object {
        const val NEXT_ACTION_BROWSE = "browse_account"
        const val NEXT_ACTION_SHARE = "share"
        const val NEXT_ACTION_ACCOUNTS = "account_list"
        const val NEXT_ACTION_TERMINATE = "terminate"
    }

    fun forgetCredentials(accountID: StateID, isLegacy: Boolean) {
        tokenDao.deleteToken(accountID.id)
        if (isLegacy) {
            legacyCredentialsDao.forgetPassword(accountID.id)
        }
    }

    /** Cells' Credentials flow management */
    suspend fun generateOAuthFlowUri(
        sessionFactory: SessionFactory,
        url: ServerURL,
        next: String
    ): Uri? =
        withContext(Dispatchers.IO) {
            try {
                val serverID = StateID(url.id).id

                val server = sessionFactory.getServer(serverID)
                // TODO Do we want to try to re-register the server when it is unknown from the SessionFactory
                    ?: let {
                        Log.e(
                            logTag,
                            "could not get server $serverID for SessionFactory with url ${url.id}"
                        )
                        return@withContext null
                    }

                val oAuthState = generateOAuthState()
                val uri: Uri = generateUriData(server.oAuthConfig, oAuthState)
                // Register the state to enable the callback
                val rOAuthState = ROAuthState(
                    state = oAuthState,
                    serverURL = url,
                    next = next,
                    startTimestamp = currentTimestamp()
                )
                Log.d(logTag, "About to store OAuth state: $rOAuthState")
                authStateDao.insert(rOAuthState)
                return@withContext uri

//                val intent = Intent(Intent.ACTION_VIEW)
//                intent.data = uri
//                intent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY
//                intent
            } catch (e: SDKException) {
                Log.e(
                    logTag,
                    "could not create intent for ${url.url.host}," +
                            " cause: ${e.code} - ${e.message}"
                )
                return@withContext null
            }
        }

    suspend fun handleOAuthResponse(
        accountService: AccountService,
        sessionFactory: SessionFactory,
        oauthState: String,
        code: String
    ): Pair<StateID, String?>? =
        withContext(Dispatchers.IO) {
            var accountID: StateID? = null

            val rState = authStateDao.get(oauthState)
            if (rState == null) {
                Log.w(logTag, "Ignored callback with unknown state: $oauthState")
                return@withContext null
            }
            try {
                val transport = sessionFactory
                    .getAnonymousTransport(rState.serverURL.id) as CellsTransport
                val token = transport.getTokenFromCode(code, encoder)
                Log.d(logTag, "... Matched the token")
                accountID = manageRetrievedToken(accountService, transport, token)
                Log.d(logTag, "... Token managed, post clean. Next action: ${rState.next}")

                // Leave OAuth state cacheDB clean
                authStateDao.delete(oauthState)
                // When creating a new account, we want to put its session on the foreground
                if (rState.next == NEXT_ACTION_BROWSE) {
                    accountService.openSession(accountID)
                }
            } catch (e: Exception) {
                Log.e(logTag, "Could not finalize credential auth flow")
                e.printStackTrace()
            }
            if (accountID == null) {
                return@withContext null
            } else {
                return@withContext Pair(accountID, rState.next)
            }
        }

    @Throws(SDKException::class)
    private suspend fun manageRetrievedToken(
        accountService: AccountService,
        transport: CellsTransport,
        token: Token
    ): StateID {

        val idToken = IdToken.parse(encoder, token.idToken)
        val accountID = StateID(idToken.claims.name, transport.server.url())
        val jwtCredentials = JWTCredentials(accountID.username, token)

        accountService.signUp(transport.server.serverURL, jwtCredentials)

        // TODO: also launch:
        //   - workspace refresh
        //   - offline check and update (in case of configuration change)

        // This will directly try to use the newly registered session to get a Client
//        val client: Client = sf.getUnlockedClient(accountID.id)
//        val workspaces: MutableMap<String, WorkspaceNode> = HashMap()
//        client.workspaceList { ws: Node ->
//            workspaces[(ws as WorkspaceNode).slug] = ws
//        }
//        val account: AccountRecord = sf.getSession(accountID.id).getAccount()
//        account.setWorkspaces(workspaces)
//        App.getAccountService().updateAccount(account)
//        App.getSessionFactory().loadKnownAccounts()


        // TODO ?
        // Set the session as current in the app
        // Adapt poll and tasks
        // check if it was a background thread
        // redirectToCallerWithNewState(State.fromAccountId(accountID.id), oauthState)
        return accountID
    }

    private fun generateUriData(cfg: OAuthConfig, state: String): Uri {
        var uriBuilder = Uri.parse(cfg.authorizeEndpoint).buildUpon()
        uriBuilder = uriBuilder.appendQueryParameter("state", state)
            .appendQueryParameter("scope", cfg.scope)
            .appendQueryParameter("client_id", ClientData.getInstance().clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", cfg.redirectURI)
        if (cfg.audience != null && "" != cfg.audience) {
            uriBuilder.appendQueryParameter("audience_id", cfg.audience)
        }
        return uriBuilder.build()
    }

    private val seedChars = "abcdef1234567890"

    private fun generateOAuthState(): String {
        val sb = StringBuilder()
        val rand = Random()
        for (i in 0..12) {
            sb.append(seedChars[rand.nextInt(seedChars.length)])
        }
        return sb.toString()
    }
}
