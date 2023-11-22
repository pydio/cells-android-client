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

    private val defaultGatewaySecret = "gatewaysecret"
    private val logTag = "CellsAuthProvider"

    override fun getCredentials(): AWSCredentials {
        return BasicAWSCredentials(transport.accessToken, defaultGatewaySecret)
    }

    override fun refresh() {
        Log.i(logTag, "Explicit token refresh request for $accountID")
        try {
            transport.requestTokenRefresh()
        } catch (se: SDKException) {
            Log.e(logTag, "Unexpected error while requesting token refresh for $accountID")
            Log.e(logTag, "#${se.code}: ${se.message}")
            se.printStackTrace()
        }
    }
}
