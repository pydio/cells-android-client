package com.pydio.android.cells.transfer
//
//import android.content.Context
//import android.util.Log
//import com.amazonaws.ClientConfiguration
//import com.amazonaws.auth.AWSCredentialsProviderChain
//import com.amazonaws.auth.SignerFactory
//import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
//import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
//import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtilityOptions
//import com.amazonaws.regions.Region
//import com.amazonaws.regions.Regions
//import com.amazonaws.services.s3.AmazonS3Client
//import com.amazonaws.services.s3.S3ClientOptions
//import com.pydio.android.cells.services.AccountService
//import com.pydio.cells.transport.CellsTransport
//import com.pydio.cells.transport.ServerURLImpl
//import com.pydio.cells.transport.StateID
//import com.pydio.cells.utils.Str
//
//private val transferUtilities: MutableMap<String, TransferUtility> = mutableMapOf()
//
//private const val logTag = "S3TransferService"
//
//fun getS3Client(transport: CellsTransport, accountID: StateID): AmazonS3Client {
//
//    val chain = AWSCredentialsProviderChain(CellsAuthProvider(transport, accountID))
//    // Register the Cells specific signers: we do not yet support the streaming signer on the server side
//    SignerFactory.registerSigner(CellsSigner.CELLS_SIGNER_ID, CellsSigner::class.java)
//    var conf = ClientConfiguration()
//        .withSignerOverride(CellsSigner.CELLS_SIGNER_ID)
//        .withUserAgentOverride(transport.userAgent) // default adds a prefix with the AWS SDK agent that we do not want to expose
//
//    if (transport.server.isSSLUnverified) {
//        conf = conf.withTrustManager(ServerURLImpl.SKIP_VERIFY_TRUST_MANAGER[0])
//    }
//
//    val region = Region.getRegion(Regions.fromName(Regions.US_EAST_1.getName()))
//    val s3Client = AmazonS3Client(chain, region, conf)
//    s3Client.endpoint = transport.server.url()
//    s3Client.setS3ClientOptions(
//        S3ClientOptions.builder()
//            .setPayloadSigningEnabled(false)
//            .disableChunkedEncoding()
//            .setPathStyleAccess(true)
//            // TODO fix and re-enable md5 checks..
//            .skipContentMd5Check(true)
//            .build()
//    )
//
//    return s3Client
//}
//
//fun getTransferUtility(
//    context: Context?,
//    accountService: AccountService,
//    accountID: StateID
//): TransferUtility? {
//
//    if (Str.notEmpty(accountID.path)) {
//        Log.e(logTag, "You must use an **account** ID to get the transfer utility")
//        return null
//    }
//    transferUtilities[accountID.id]?.let { return it }
//
//    Log.i(logTag, "Instantiating a new transfer utility for $accountID")
//
//    if (transferUtilities.isNotEmpty()) {
//        Log.e(logTag, "Listing defined transfer utilities:")
//        for (tu in transferUtilities) {
//            Log.e(logTag, "  - ${tu.key}")
//        }
//    }
//
//    TransferNetworkLossHandler.getInstance(context)
//    val config = TransferUtilityOptions()
//    config.transferThreadPoolSize = 3
//    config.minimumUploadPartSizeInMB = 10
//
//    val newTU = TransferUtility.builder()
//        .context(context)
//        .defaultBucket(DEFAULT_BUCKET_NAME)
//        .s3Client(
//            getS3Client(
//                accountService.getTransport(accountID, true) as CellsTransport,
//                accountID
//            )
//        )
//        .transferUtilityOptions(config)
//        .build()
//    transferUtilities[accountID.id] = newTU
//    return newTU
//}
