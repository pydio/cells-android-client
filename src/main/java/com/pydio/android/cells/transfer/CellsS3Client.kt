package com.pydio.android.cells.transfer

import android.util.Log
import com.amazonaws.ClientConfiguration
import com.amazonaws.HttpMethod
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.pydio.cells.api.S3Client
import com.pydio.cells.api.S3Names
import com.pydio.cells.api.SDKException
import com.pydio.cells.transport.CellsTransport
import com.pydio.cells.transport.StateID
import java.net.URL

private const val logTag = "CellsS3Client"

private const val DEFAULT_GATEWAY_SECRET = "gatewaysecret"
private const val DEFAULT_S3_REGION_NAME = "us-east-1"

const val DEFAULT_BUCKET_NAME = "data"

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
                Log.w(logTag, "Found a double slash in $path, this is most probably a bug:")
                Thread.dumpStack()
                path = path.replace("//", "/")
            }
            return path
        }
    }
}
