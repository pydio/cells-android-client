package com.pydio.android.cells.transfer

import android.content.Context
import com.amazonaws.ClientConfiguration
import com.amazonaws.HttpMethod
import com.amazonaws.auth.AWSCredentialsProviderChain
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.auth.SignerFactory
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtilityOptions
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.S3ClientOptions
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.pydio.android.cells.services.AccountService
import com.pydio.cells.api.S3Client
import com.pydio.cells.api.S3Names
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Log
import java.net.URL


private const val logTag = "CellsS3Client"

// private const val DEFAULT_BUCKET_NAME = "io"
private const val DEFAULT_BUCKET_NAME = "data"
private const val DEFAULT_GATEWAY_SECRET = "gatewaysecret"

private val DEFAULT_S3_REGION_NAME = "us-east-1"
// Region.getRegion(Regions.US_EAST_1.name)

private val transferUtilities: MutableMap<String, TransferUtility> = mutableMapOf()

/* Main entry point to communicate with a S3 store */
class CellsS3Client(private val transport: CellsTransport) : S3Client {

    @Throws(SDKException::class)
    override fun getUploadPreSignedURL(ws: String, folder: String, name: String): URL {
        val filename = getCleanPath(ws, folder, name)
        val request = GeneratePresignedUrlRequest(DEFAULT_BUCKET_NAME, filename)
        request.method = HttpMethod.PUT
        request.contentType = S3Names.S3_CONTENT_TYPE_OCTET_STREAM
        request.addRequestParameter(S3Names.S3_TOKEN_KEY, transport.accessToken)
        return s3Client().generatePresignedUrl(request)
    }

    @Throws(SDKException::class)
    override fun getDownloadPreSignedURL(ws: String, file: String): URL {
        val filename = getCleanPath(ws, file)
        val request = GeneratePresignedUrlRequest("io", filename)
        request.method = HttpMethod.GET
        request.addRequestParameter(S3Names.S3_TOKEN_KEY, transport.accessToken)
        return s3Client().generatePresignedUrl(request)
    }

    @Throws(SDKException::class)
    override fun getStreamingPreSignedURL(slug: String, file: String, contentType: String): URL {
        val filename = getCleanPath(slug, file)
        val request = GeneratePresignedUrlRequest(DEFAULT_BUCKET_NAME, filename)
        request.addRequestParameter(S3Names.S3_TOKEN_KEY, transport.accessToken)
        request.addRequestParameter(S3Names.RESPONSE_CONTENT_TYPE, contentType)
        return s3Client().generatePresignedUrl(request)
    }

    // TODO improve this to enable refresh when necessary
    @Throws(SDKException::class)
    private fun s3Client(): AmazonS3 {
        val awsCredentials = BasicAWSCredentials(transport.accessToken, DEFAULT_GATEWAY_SECRET)
        val conf = ClientConfiguration().withUserAgent(transport.userAgent)
        val s3Client = AmazonS3Client(
            awsCredentials,
            Region.getRegion(DEFAULT_S3_REGION_NAME),
            conf
        )
        s3Client.endpoint = transport.server.url()
        return s3Client
    }

    companion object {

        //    // This does not work with self-signed certificate
        //    public InputStream getThumb(String file) throws SDKException {
        //        GetObjectRequest request = new GetObjectRequest(defaultBucketName, S3Names.PYDIO_S3_THUMBSTORE_PREFIX + file);
        //        try {
        //            return getS3Client().getObject(request).getObjectContent();
        //        } catch (AmazonS3Exception e) {
        //            throw new SDKException("could not get S3 file at " + file, e);
        //        }
        //    }
        fun getCleanPath(slug: String, parent: String, fileName: String): String {
            return if ("/" == parent) {
                getCleanPath(slug, "/$fileName")
            } else {
                getCleanPath(slug, "$parent/$fileName")
            }
        }

        fun getCleanPath(stateID: StateID): String {
            return stateID.path.substring(1)
        }

        private fun getCleanPath(slug: String, file: String): String {
            var path = slug + file
            if (path.contains("//")) {
                // This should not happen anymore
                Log.w(
                    "Legacy",
                    "Found a double slash in $path, this is most probably a bug. Double check and fix"
                )
                Thread.dumpStack()
                path = path.replace("//", "/")
            }
            return path
        }
    }
}

fun getS3Client(transport: CellsTransport, accountID: StateID): AmazonS3Client {

    val chain = AWSCredentialsProviderChain(CellsAuthProvider(transport, accountID))

    // Register the Cells specific signers: we do not yet support the streaming signer on the server side
    SignerFactory.registerSigner(CellsSigner.CELLS_SIGNER_ID, CellsSigner::class.java)
    val conf = ClientConfiguration()
        .withSignerOverride(CellsSigner.CELLS_SIGNER_ID)
        .withUserAgentOverride(transport.userAgent)
    // using the default agent mechanism prefix Cells User Agent
    // with the AWS SDK agent that we do not want to expose
    // .withUserAgent(transport.userAgent)

    // TODO using the constant does not work
    // val region = Region.getRegion(Regions.US_EAST_1.name)
    val region = Region.getRegion("us-east-1")

    Log.e(logTag, "Before getting amazon S3 client: $region")
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
//             .build()
    )

//        val s3Client = AmazonS3Client(chain, Region.getRegion("us-east-1"), conf)

//        Log.e(logTag, "### Listing buckets:")
//        s3Client.listBuckets().forEach {
//            Log.e(logTag, "  - [${it.name}]")
//            Log.d(logTag, "    Now listing inner objects")
//            val objList = s3Client.listObjects(it.name)
//            objList.objectSummaries.forEach {
//                Log.d(logTag, it.toString())
//            }
//        }
//        Log.e(logTag, "### Done listing buckets!")

    return s3Client
}

fun getTransferUtility(
    context: Context?,
    accountService: AccountService,
    accountID: StateID
): TransferUtility? {
    transferUtilities[accountID.id]?.let { return it }
    TransferNetworkLossHandler.getInstance(context)

    val config = TransferUtilityOptions()
    config.transferThreadPoolSize = 3
    config.minimumUploadPartSizeInMB = 10

    val newTU = TransferUtility.builder()
        .context(context)
        .defaultBucket(DEFAULT_BUCKET_NAME)
        .s3Client(
            getS3Client(
                accountService.getTransport(accountID, true) as CellsTransport,
                accountID
            )
        )
        .transferUtilityOptions(config)
        .build()
    transferUtilities[accountID.id] = newTU
    return newTU
}
