package com.pydio.android.cells.services;


public class S3Client {
}
//import com.amazonaws.ClientConfiguration;
//import com.amazonaws.HttpMethod;
//import com.amazonaws.auth.BasicAWSCredentials;
//import com.amazonaws.regions.Region;
//import com.amazonaws.services.s3.AmazonS3;
//import com.amazonaws.services.s3.AmazonS3Client;
//import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
//import com.pydio.cells.api.S3Names;
//import com.pydio.cells.api.SDKException;
//import com.pydio.cells.transport.CellsTransport;
//import com.pydio.cells.transport.StateID;
//import com.pydio.cells.utils.Log;
//
//import java.net.URL;
//
//
//
///* Main entry point to communicate with a S3 store */
//public class S3Client implements com.pydio.cells.api.S3Client {
//
//    // private final static String logTag = "S3Client";
//
//    private final static String DEFAULT_BUCKET_NAME = "io";
//    private final static String DEFAULT_GATEWAY_SECRET = "gatewaysecret";
//    private final static String DEFAULT_S3_REGION = "us-east-1";
//
//    private final CellsTransport transport;
//
//    public S3Client(CellsTransport transport) {
//        this.transport = transport;
//    }
//
//    public URL getUploadPreSignedURL(String ws, String folder, String name) throws SDKException {
//        String filename = getCleanPath(ws, folder, name);
//        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(DEFAULT_BUCKET_NAME, filename);
//        request.setMethod(HttpMethod.PUT);
//        request.setContentType(S3Names.S3_CONTENT_TYPE_OCTET_STREAM);
//        request.addRequestParameter(S3Names.S3_TOKEN_KEY, transport.getAccessToken());
//        return getS3Client().generatePresignedUrl(request);
//    }
//
//    public URL getDownloadPreSignedURL(String ws, String file) throws SDKException {
//        String filename = getCleanPath(ws, file);
//        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(DEFAULT_BUCKET_NAME, filename);
//        request.setMethod(HttpMethod.GET);
//        request.addRequestParameter(S3Names.S3_TOKEN_KEY, transport.getAccessToken());
//        return getS3Client().generatePresignedUrl(request);
//    }
//
//    public URL getStreamingPreSignedURL(String slug, String file, String contentType) throws SDKException {
//        String filename = getCleanPath(slug, file);
//        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(DEFAULT_BUCKET_NAME, filename);
//        request.addRequestParameter(S3Names.S3_TOKEN_KEY, transport.getAccessToken());
//        request.addRequestParameter(S3Names.RESPONSE_CONTENT_TYPE, contentType);
//        // request.setMethod(HttpMethod.GET);
//        return getS3Client().generatePresignedUrl(request);
//    }
//
//    // TODO improve this to enable refresh when necessary
//    private AmazonS3 getS3Client() throws SDKException {
//        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(transport.getAccessToken(), DEFAULT_GATEWAY_SECRET);
//        ClientConfiguration conf = new ClientConfiguration().withUserAgent(transport.getUserAgent());
//        AmazonS3 s3 = new AmazonS3Client(awsCredentials, Region.getRegion(DEFAULT_S3_REGION), conf);
//        s3.setEndpoint(transport.getServer().url());
//        // return clientBuilder.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
//        return s3;
//    }
//
//    //    // This does not work with self-signed certificate
////    public InputStream getThumb(String file) throws SDKException {
////        GetObjectRequest request = new GetObjectRequest(defaultBucketName, S3Names.PYDIO_S3_THUMBSTORE_PREFIX + file);
////        try {
////            return getS3Client().getObject(request).getObjectContent();
////        } catch (AmazonS3Exception e) {
////            throw new SDKException("could not get S3 file at " + file, e);
////        }
////    }
//
//    public static String getCleanPath(String slug, String parent, String fileName) {
//        if ("/".equals(parent)) {
//            return getCleanPath(slug, "/".concat(fileName));
//        } else {
//            return getCleanPath(slug, parent.concat("/").concat(fileName));
//        }
//    }
//
//    public static String getCleanPath(StateID stateID) {
//        return stateID.getPath().substring(1);
//    }
//
//
//    private static String getCleanPath(String slug, String file) {
//        String path = slug + file;
//        if (path.contains("//")) {
//            // This should not happen anymore
//            Log.w("Legacy",
//                    "Found a double slash in " + path + ", this is most probably a bug. Double check and fix");
//            Thread.dumpStack();
//            path = path.replace("//", "/");
//        }
//        return path;
//    }
//}
