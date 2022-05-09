package com.pydio.android.legacy.v2

import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.nodes.RTreeNode
import com.pydio.android.cells.services.AccountService
import com.pydio.android.cells.services.NodeService
import com.pydio.android.cells.services.SessionFactory
import com.pydio.cells.api.SdkNames
import com.pydio.cells.legacy.P8Credentials
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import com.pydio.cells.transport.auth.credentials.JWTCredentials
import com.pydio.cells.utils.IoHelpers
import com.pydio.cells.utils.Log
import com.pydio.cells.utils.Str
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Centralize migration process from v2 to v3.
 * We only migrate accounts, credentials and offline-roots.
 * Everything else must be downloaded again.
 */
class MigrationServiceV2 : KoinComponent {

    private val logTag = MigrationServiceV2::class.simpleName

    private val accountService by inject<AccountService>()
    private val sessionFactory by inject<SessionFactory>()
    private val nodeService by inject<NodeService>()

    suspend fun migrateOneCellsAccount(
        record: AccountRecord,
        mainDB: MainDB,
        syncDB: SyncDB
    ) {
        val currState = StateID.fromId(record.id())
        Log.i(logTag, "About to migrate: $currState")

        // Main account and credentials
        val session = accountService.getSession(currState)
        if (session == null) {
            Log.i(logTag, "No session found in room, creating.")
            val serverURL = ServerURLImpl.fromAddress(record.url(), record.skipVerify())

            val token = mainDB.getToken(record.id())
            if (token == null) {
                val server = sessionFactory.registerServer(serverURL)
                accountService.registerAccount(
                    record.username,
                    server,
                    AppNames.AUTH_STATUS_NO_CREDS
                )
            } else {
                // TODO better handling of expired and error tokens
                val jwtCredentials = JWTCredentials(record.username, token)
                accountService.signUp(serverURL, jwtCredentials)
            }
        }

        // Refresh workspace list and check credentials
        var client = try {
            sessionFactory.getUnlockedClient(currState.accountId)
        } catch (e: Exception) {
            null
        }

        val result = accountService.refreshWorkspaceList(currState.accountId)
        if (result.second != null) { // Non-Null response is an error message
            Log.i(logTag, "could not list workspaces for $currState: ${result.second}")
            client = null
        }

        // Migrate offline roots
        val offlineRoots = syncDB.getWatches(record.id())
        if (offlineRoots.isEmpty()) {
            return
        }

        for (currRoot in offlineRoots) {
            var storedFileNode = currRoot.node
            val state = currState.withPath("/" + storedFileNode.workspace + storedFileNode.path)
            val newNode = if (client == null) {
                if (Str.empty(storedFileNode.mimeType)) {
                    storedFileNode.setProperty(
                        SdkNames.NODE_PROPERTY_MIME,
                        SdkNames.NODE_MIME_DEFAULT
                    )
                }
                RTreeNode.fromFileNode(state, storedFileNode)
            } else {
                val fn = client.nodeInfo(storedFileNode.workspace, storedFileNode.path)
                RTreeNode.fromFileNode(state, fn)
            }
            nodeService.updateOfflineRoot(newNode)
        }
    }

    suspend fun migrateOneP8Account(record: AccountRecord, mainDB: MainDB) {
        val currState = StateID.fromId(record.id())
        Log.i(logTag, "About to migrate: $currState")

        val session = accountService.getSession(currState)
        if (session == null) {
            Log.i(logTag, "No session found in room, creating.")
            val serverURL = ServerURLImpl.fromAddress(record.url(), record.skipVerify())

            val pwd = mainDB.getPassword(record.id())
            if (pwd == null) {
                val server = sessionFactory.registerServer(serverURL)
                accountService.registerAccount(
                    record.username,
                    server,
                    AppNames.AUTH_STATUS_NO_CREDS
                )
            } else {
                // TODO better handling of expired and error tokens
                val jwtCredentials = P8Credentials(record.username, pwd)
                accountService.signUp(serverURL, jwtCredentials)
            }
        }

        // Refresh workspace list and check credentials
        var client = try {
            sessionFactory.getUnlockedClient(currState.accountId)
        } catch (e: Exception) {
            null
        }

        val result = accountService.refreshWorkspaceList(currState.accountId)
        if (result.second != null) { // Non-Null response is an error message
            Log.i(logTag, "could not list workspaces for $currState: ${result.second}")
            client = null
        }
    }

    suspend fun doUpload(stateID: StateID, file: File, mime: String) {
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(file)
            accountService.getClient(stateID).upload(
                inputStream, file.length(),
                mime, stateID.workspace, stateID.file, file.name,
                true, null
            )
        } catch (e: Exception) {
            // TODO manage errors correctly
            Log.e(logTag, "!! could not upload ${file.name}: ${e.message}")
            e.printStackTrace()
        } finally {
            IoHelpers.closeQuietly(inputStream)
        }
    }
}
