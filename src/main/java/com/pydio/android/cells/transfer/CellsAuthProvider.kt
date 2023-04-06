package com.pydio.android.cells.transfer

import android.util.Log
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.StateID

// private final static String logTag = "S3Client";

private const val DEFAULT_GATEWAY_SECRET = "gatewaysecret"

class CellsAuthProvider(
    private val transport: CellsTransport,
    private val accountID: StateID
) : AWSCredentialsProvider {

    private val logTag = "CellsAuthProvider"

    override fun getCredentials(): AWSCredentials {
        Log.e(logTag, "Retrieving credentials for $accountID")
        val token = transport.accessToken
        return BasicAWSCredentials(token, DEFAULT_GATEWAY_SECRET)
    }

    override fun refresh() {
        Log.e(logTag, "######################################## ")
        Log.e(logTag, "######################################## ")
        Log.e(logTag, "######################################## ")
        Log.e(logTag, "######################################## ")
        Log.e(logTag, "######################################## ")
        Log.e(logTag, "Refreshing credentials for $accountID")
        TODO("Not yet implemented")
    }
}

class MyTransferListener() : TransferListener {

    private val logTag = "MyTransferListener"
    override fun onStateChanged(id: Int, state: TransferState?) {
        Log.e(logTag, "... #$id - State changed: $state")
    }

    override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
        Log.e(logTag, "... #$id - Progress: $bytesCurrent / $bytesTotal")
    }

    override fun onError(id: Int, e: java.lang.Exception) {
        Log.e(logTag, "... #$id - Error: ${e.message}")
        e.printStackTrace()
        // Do something in the callback.
    }
}
