package com.pydio.android.cells.services

import android.content.Context
import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.SignerFactory
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtilityOptions
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.pydio.android.cells.JobStatus
import com.pydio.android.cells.db.nodes.RTransfer
import com.pydio.android.cells.db.nodes.TransferDao
import com.pydio.android.cells.db.nodes.TreeNodeDB
import com.pydio.android.cells.transfer.CellsAuthProvider
import com.pydio.android.cells.transfer.CellsS3Client
import com.pydio.android.cells.transfer.CellsSigner
import com.pydio.android.cells.transfer.CellsTransferListener
import com.pydio.android.cells.transfer.DEFAULT_BUCKET_NAME
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.android.cells.utils.currentTimestampAsString
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.ServerURLImpl
import com.pydio.cells.transport.StateID
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class S3TransferService(
    private val androidApplicationContext: Context,
    coroutineService: CoroutineService,
    private val accountService: AccountService,
    private val treeNodeRepository: TreeNodeRepository,
) {

    private val logTag = "S3TransferService"

    private val ioScope = coroutineService.cellsIoScope
    private val ioDispatcher = coroutineService.ioDispatcher

    private val transferUtilities: MutableMap<String, TransferUtility> = mutableMapOf()

    @Throws(SDKException::class)
    suspend fun doDownload(
        stateID: StateID,
        targetFile: File,
        dao: TransferDao,
        transferRecord: RTransfer,
        parentJobProgress: Channel<Long>?
    ) = withContext(ioDispatcher) {
        Log.d(logTag, "TU download for ${targetFile.absolutePath}")
        val key = CellsS3Client.getCleanPath(stateID)

        val observer = getTransferUtility(stateID).download(key, targetFile)
        transferRecord.externalID = observer.id
        transferRecord.status = JobStatus.PROCESSING.id
        transferRecord.startTimestamp = currentTimestamp()
        dao.update(transferRecord)

        observer.setTransferListener(
            CellsTransferListener(
                observer.id,
                dao,
                parentJobProgress,
            )
        )
    }

    suspend fun doUpload(
        stateID: StateID,
        file: File,
        dao: TransferDao,
        transferRecord: RTransfer
    ) = withContext(ioDispatcher) {

        Log.d(logTag, "TU upload for ${file.absolutePath} (size: ${file.length()}")
        // TODO double check if a transfer for the same state ID already exists.
        val key = CellsS3Client.getCleanPath(stateID)
        Log.i(
            logTag, "... About to put\n" +
                    "- key: [$key] \n" +
                    "- file: ${file.absolutePath}"
        )

        val observer = getTransferUtility(stateID).upload(key, file)
        transferRecord.externalID = observer.id
        transferRecord.status = JobStatus.PROCESSING.id
        transferRecord.startTimestamp = currentTimestamp()
        dao.update(transferRecord)

        // TODO improve: we loose the listener if the app is restarted during the upload
        observer.setTransferListener(CellsTransferListener(observer.id, dao))
    }

    suspend fun cancelTransfer(stateID: StateID, transferID: Long, owner: String) =
        withContext(ioDispatcher) {
            val dao = nodeDB(stateID).transferDao()
            val rTransfer = dao.getById(transferID) ?: run {
                throw IllegalStateException("Cannot cancel an unknown transfer")
            }

            if (!getTransferUtility(stateID).cancel(rTransfer.externalID)) {
                throw SDKException("Could not cancel transfer for $stateID")
            }
            rTransfer.status = JobStatus.CANCELLED.id
            rTransfer.doneTimestamp = currentTimestamp()
            rTransfer.error = "Cancelled by $owner"
            dao.update(rTransfer)
        }

    suspend fun pauseTransfer(stateID: StateID, transferID: Long, owner: String) =
        withContext(ioDispatcher) {
            val dao = nodeDB(stateID).transferDao()
            val rTransfer = dao.getById(transferID) ?: run {
                throw IllegalStateException("Cannot pause an unknown transfer")
            }
            if (!getTransferUtility(stateID).pause(rTransfer.externalID)) {
                throw SDKException("Could not pause transfer for $stateID")
            }
            rTransfer.status = JobStatus.PAUSED.id
            rTransfer.startTimestamp = -1
            rTransfer.error = "Paused by $owner since ${currentTimestampAsString()}"
            dao.update(rTransfer)
        }

    suspend fun resumeTransfer(stateID: StateID, transferID: Long) =
        withContext(ioDispatcher) {
            val dao = nodeDB(stateID).transferDao()
            val rTransfer = dao.getById(transferID) ?: run {
                throw IllegalStateException("Cannot resume an unknown transfer")
            }
            val tu = getTransferUtility(stateID)
            ioScope.launch {

                val observer = tu.resume(rTransfer.externalID)
                rTransfer.externalID = observer.id
                rTransfer.status = JobStatus.PROCESSING.id
                rTransfer.error = null
                rTransfer.startTimestamp = currentTimestamp()
                dao.update(rTransfer)
                observer.setTransferListener(CellsTransferListener(observer.id, dao))

            }
        }

    suspend fun forgetTransfer(stateID: StateID, transferID: Long) =
        withContext(ioDispatcher) {
            val dao = nodeDB(stateID).transferDao()
            val rTransfer = dao.getById(transferID) ?: run {
                Log.w(logTag, "No transfer found to delete for $stateID, ignoring")
                return@withContext
            }
            getTransferUtility(stateID).deleteTransferRecord(rTransfer.externalID)
            dao.deleteTransfer(transferID)
        }

    private fun getS3Client(transport: CellsTransport, accountID: StateID): AmazonS3Client {

        val chain = AWSCredentialsProviderChain(CellsAuthProvider(transport, accountID))
        // Register the Cells specific signers: we do not yet support the streaming signer on the server side
        SignerFactory.registerSigner(CellsSigner.CELLS_SIGNER_ID, CellsSigner::class.java)
        var conf = ClientConfiguration()
            .withSignerOverride(CellsSigner.CELLS_SIGNER_ID)
            .withUserAgentOverride(transport.userAgent) // default adds a prefix with the AWS SDK agent that we do not want to expose

        if (transport.server.isSSLUnverified) {
            conf = conf.withTrustManager(ServerURLImpl.SKIP_VERIFY_TRUST_MANAGER[0])
        }

        val region = Region.getRegion(Regions.fromName(Regions.US_EAST_1.getName()))
        val s3Client = AmazonS3Client(chain, region, conf)
        s3Client.endpoint = transport.server.url()
        s3Client.setS3ClientOptions(
            S3ClientOptions.builder()
                .setPayloadSigningEnabled(false)
                .disableChunkedEncoding()
                .setPathStyleAccess(true)
                // TODO fix and re-enable md5 checks..
                .skipContentMd5Check(true)
                .build()
        )

        return s3Client
    }

    private fun getTransferUtility(stateID: StateID): TransferUtility {
        val accountID = stateID.account()

        transferUtilities[accountID.id]?.let { return it }

        Log.i(logTag, "Instantiating a new transfer utility for $accountID")

        if (transferUtilities.isNotEmpty()) {
            Log.e(logTag, "Listing defined transfer utilities:")
            for (tu in transferUtilities) {
                Log.e(logTag, "  - ${tu.key}")
            }
        }

        TransferNetworkLossHandler.getInstance(androidApplicationContext)
        val config = TransferUtilityOptions()
        config.transferThreadPoolSize = 3
        config.minimumUploadPartSizeInMB = 10

        val ct = accountService.getTransport(accountID, true)
        if (ct !is CellsTransport)
            throw SDKException("Could not get Cells transport for $accountID")
        val newTU = TransferUtility.builder()
            .context(androidApplicationContext)
            .defaultBucket(DEFAULT_BUCKET_NAME)
            .s3Client(getS3Client(ct, accountID))
            .transferUtilityOptions(config)
            .build()
        transferUtilities[accountID.id] = newTU
        return newTU
    }

    private fun nodeDB(stateID: StateID): TreeNodeDB {
        return treeNodeRepository.nodeDB(stateID)
    }
}

