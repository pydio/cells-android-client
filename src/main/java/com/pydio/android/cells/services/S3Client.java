package com.pydio.android.cells.services;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.pydio.cells.api.S3Names;
import com.pydio.cells.api.SDKException;
import com.pydio.cells.transport.CellsTransport;
import com.pydio.cells.utils.Log;
import com.pydio.cells.utils.Str;

import java.net.URL;

/* Main entry point to communicate with a S3 store */
public class S3Client implements com.pydio.cells.api.S3Client {

    private final static String logTag = S3Client.class.getSimpleName();

    private final static String DEFAULT_BUCKET_NAME = "io";
    private final static String DEFAULT_GATEWAY_SECRET = "gatewaysecret";
    private final static String DEFAULT_S3_REGION = "us-east-1";

    private final CellsTransport transport;

    public S3Client(CellsTransport transport) {
        this.transport = transport;
    }

    public URL getUploadPreSignedURL(String ws, String folder, String name) throws SDKException {
        String filename = getCleanPath(ws, folder, name);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(DEFAULT_BUCKET_NAME, filename);
        request.setMethod(HttpMethod.PUT);
        request.setContentType(S3Names.S3_CONTENT_TYPE_OCTET_STREAM);
        request.addRequestParameter(S3Names.S3_TOKEN_KEY, transport.getAccessToken());
        return getS3Client().generatePresignedUrl(request);
    }

    public URL getDownloadPreSignedURL(String ws, String file) throws SDKException {
        String filename = getCleanPath(ws, file);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(DEFAULT_BUCKET_NAME, filename);
        request.setMethod(HttpMethod.GET);
        request.addRequestParameter(S3Names.S3_TOKEN_KEY, transport.getAccessToken());
        return getS3Client().generatePresignedUrl(request);
    }

    public URL getStreamingPreSignedURL(String slug, String file, String contentType) throws SDKException {
        String filename = getCleanPath(slug, file);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(DEFAULT_BUCKET_NAME, filename);
        request.addRequestParameter(S3Names.S3_TOKEN_KEY, transport.getAccessToken());
        request.addRequestParameter(S3Names.RESPONSE_CONTENT_TYPE, contentType);
        // request.setMethod(HttpMethod.GET);
        return getS3Client().generatePresignedUrl(request);
    }

    // TODO improve this to enable refresh when necessary
    private AmazonS3 getS3Client() throws SDKException {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(transport.getAccessToken(), DEFAULT_GATEWAY_SECRET);
        ClientConfiguration conf = new ClientConfiguration().withUserAgent(transport.getUserAgent());
        AmazonS3 s3 = new AmazonS3Client(awsCredentials, Region.getRegion(DEFAULT_S3_REGION), conf);
        s3.setEndpoint(transport.getServer().url());
        // return clientBuilder.withCredentials(new AWSStaticCredentialsProvider(awsCreds)).build();
        return s3;
    }

    //    // This does not work with self-signed certificate
//    public InputStream getThumb(String file) throws SDKException {
//        GetObjectRequest request = new GetObjectRequest(defaultBucketName, S3Names.PYDIO_S3_THUMBSTORE_PREFIX + file);
//        try {
//            return getS3Client().getObject(request).getObjectContent();
//        } catch (AmazonS3Exception e) {
//            throw new SDKException("could not get S3 file at " + file, e);
//        }
//    }

    private String getCleanPath(String slug, String parent, String fileName) {
        if ("/".equals(parent)) {
            return getCleanPath(slug, "/".concat(fileName));
        } else {
            return getCleanPath(slug, parent.concat("/").concat(fileName));
        }
    }

    private String getCleanPath(String slug, String file) {
        String path = slug + file;
        if (path.contains("//")) {
            // This should not happen anymore
            Log.w("Legacy",
                    "Found a double slash in " + path + ", this is most probably a bug. Double check and fix");
            Thread.dumpStack();
            path = path.replace("//", "/");
        }
        return path;
    }


//    // Ugly fork of the com.amazonaws.util.SdkHttpUtils from the Amazon POJO SDK
//    // that is not available in the base Android SDK.
//    // Rather use the upstream class once the dependency issue for S3 has been cleanly solved.
//
//    /**
//     * Regex which matches any of the sequences that we need to fix up after
//     * URLEncoder.encode().
//     */
//    private static final Pattern ENCODED_CHARACTERS_PATTERN;
//
//    static {
//        StringBuilder pattern = new StringBuilder();
//
//        pattern
//                .append(Pattern.quote("+"))
//                .append("|")
//                .append(Pattern.quote("*"))
//                .append("|")
//                .append(Pattern.quote("%7E"))
//                .append("|")
//                .append(Pattern.quote("%2F"));
//
//        ENCODED_CHARACTERS_PATTERN = Pattern.compile(pattern.toString());
//    }
//
//    /**
//     * Encode a string for use in the path of a URL; uses URLEncoder.encode,
//     * (which encodes a string for use in the query portion of a URL), then
//     * applies some postfilters to fix things up per the RFC. Can optionally
//     * handle strings which are meant to encode a path (ie include '/'es
//     * which should NOT be escaped).
//     *
//     * @param value the value to encode
//     * @param path  true if the value is intended to represent a path
//     * @return the encoded value
//     */
//    public static String urlEncode(final String value, final boolean path) {
//        if (value == null) {
//            return "";
//        }
//
//        try {
//            String encoded = URLEncoder.encode(value, "UTF-8");
//
//            Matcher matcher = ENCODED_CHARACTERS_PATTERN.matcher(encoded);
//            StringBuffer buffer = new StringBuffer(encoded.length());
//
//            while (matcher.find()) {
//                String replacement = matcher.group(0);
//
//                if ("+".equals(replacement)) {
//                    replacement = "%20";
//                } else if ("*".equals(replacement)) {
//                    replacement = "%2A";
//                } else if ("%7E".equals(replacement)) {
//                    replacement = "~";
//                } else if (path && "%2F".equals(replacement)) {
//                    replacement = "/";
//                }
//
//                matcher.appendReplacement(buffer, replacement);
//            }
//
//            matcher.appendTail(buffer);
//            return buffer.toString();
//
//        } catch (UnsupportedEncodingException ex) {
//            throw new RuntimeException(ex);
//        }
//    }
//
//    /**
//     * Decode a string for use in the path of a URL; uses URLDecoder.decode,
//     * which decodes a string for use in the query portion of a URL.
//     *
//     * @param value The value to decode
//     * @return The decoded value if parameter is not null, otherwise, null is returned.
//     */
//    public static String urlDecode(final String value) {
//        if (value == null) {
//            return null;
//        }
//
//        try {
//            return URLDecoder.decode(value, "UTF-8");
//
//        } catch (UnsupportedEncodingException ex) {
//            throw new RuntimeException(ex);
//        }
//    }

}
