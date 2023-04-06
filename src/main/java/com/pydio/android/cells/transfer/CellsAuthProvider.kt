package com.pydio.android.cells.transfer

import android.util.Log
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.StateID

class CellsAuthProvider(
    private val transport: CellsTransport,
    private val accountID: StateID
) : AWSCredentialsProvider {

    private val DEFAULT_GATEWAY_SECRET = "gatewaysecret"
    private val logTag = "CellsAuthProvider"

    override fun getCredentials(): AWSCredentials {
//        Log.e(logTag, "Retrieving credentials for $accountID")
        val token = transport.accessToken
        return BasicAWSCredentials(token, DEFAULT_GATEWAY_SECRET)
    }

    override fun refresh() {
        Log.e(logTag, "Got a token refresh request for $accountID")
        try {
            transport.requestTokenRefresh()
        } catch (se: SDKException) {
            Log.e(logTag, "Unexpected error while requesting token refresh for $accountID")
            Log.e(logTag, "#${se.code}: ${se.message}")
            se.printStackTrace()
        }
    }
}
