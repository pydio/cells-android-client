package com.pydio.android.cells.legacy.db.model;

import com.pydio.cells.api.ErrorCodes;
import com.pydio.cells.api.SDKException;
import com.pydio.cells.openapi.ApiException;

import org.json.JSONObject;

public class ErrorInfo {

    public final static int unknown = 0;
    public final static int internal = 1;
    public final static int cancelled = 2;
    public final static int stats = 3;

    public final static int unsupportedServer = 10;
    public final static int remoteNotFound = 11;
    public final static int notConnected = 12;
    public final static int uploadFailed = 13;

    public final static int deviceRead = 20;
    public final static int deviceWrite = 21;
    public final static int noEnoughSpace = 22;
    public final static int databaseWrite = 23;
    public final static int cacheReading = 24;
    public final static int cacheWriting = 25;

    public final static int authentication = 30;
    public final static int authenticateWithChallenge = 31;
    public final static int connectionFailed = 32;
    public final static int sslError = 33;
    public final static int serverSSLNotVerified = 34;
    public final static int noToken = 35;
    public final static int tokenExpired = 36;

    public final static int unexpectedContent = 40;
    public final static int fileContentLoading = 41;
    public final static int downloadReading = 42;
    public final static int downloadWriting = 43;
    public final static int downloadOnCellular = 44;

    public final static int serviceNotAvailable = 45;

    private final Exception cause;
    private final int type;
    private final String message;

    private ErrorInfo(int type, Exception e) {
        this.type = type;
        this.cause = e;
        message = cause == null ? "Unknown error" : cause.getMessage();

    }

    public ErrorInfo(int type, String message, Exception e) {
        this.type = type;
        this.cause = e;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public Exception getCause() {
        return cause;
    }

    public int getType() {
        return type;
    }

    /**
     * CONSTRUCTORS
     */

    public static ErrorInfo fromSDKException(SDKException e) {
        int type;
        switch (e.getCode()) {
            case ErrorCodes.authentication_required:
                type = authentication;
                break;
            case ErrorCodes.authentication_with_captcha_required:
                type = authenticateWithChallenge;
                break;
            case ErrorCodes.con_failed:
            case ErrorCodes.con_closed:
                type = connectionFailed;
                break;
            case ErrorCodes.ssl_certificate_not_signed:
                type = serverSSLNotVerified;
                break;
            case ErrorCodes.not_found:
                type = remoteNotFound;
                break;
            case ErrorCodes.no_token_available:
            case ErrorCodes.invalid_credentials:
                type = noToken;
                break;
            case ErrorCodes.token_expired:
                type = tokenExpired;
                break;
            default:
                // FIXME
                // TODO: 15/10/2020 parse details message to extract the exact error source
                if (e.getCause() != null) {
                    if (e.getCause() instanceof ApiException) {
                        try {
                            String body = ((ApiException) e.getCause()).getResponseBody();
                            if (body != null) {
                                JSONObject details = new JSONObject();
                                System.out.println(details);
                            }
                        } catch (Exception ignore) {
                            // ex.printStackTrace();
                        }
                    }
                }
                type = internal;
        }
        return new ErrorInfo(type, e);
    }

    public static ErrorInfo fromSDKException(int code, SDKException e) {
        return new ErrorInfo(code, e);
    }

    public static ErrorInfo fromException(Exception e) {
        if (e instanceof SDKException) {
            return fromSDKException((SDKException) e);
        }
        return new ErrorInfo(unknown, e);
    }

    public static ErrorInfo fromException(int code, Exception e) {
        if (e instanceof SDKException) {
            return fromSDKException(code, (SDKException) e);
        }
        return new ErrorInfo(code, e);
    }

    @Deprecated
    public static ErrorInfo fromCode(int code) {
        return new ErrorInfo(code, null);
    }

    /* BOILER PLATE HELPERS */

    public boolean isAuthentication() {
        switch (type) {
            case authentication:
            case authenticateWithChallenge:
            case noToken:
            case tokenExpired:
                return true;
        }
        return false;
    }

    public boolean isConnectionFailed() {
        return type == connectionFailed;
    }

    public boolean isRemoteNotFound() {
        return type == remoteNotFound;
    }

    public boolean isDeviceRead() {
        return type == deviceRead;
    }

    public boolean isDeviceWrite() {
        return type == deviceWrite;
    }

    public boolean isNoEnoughSpace() {
        return type == noEnoughSpace;
    }

    public boolean isCacheReading() {
        return type == cacheReading;
    }

    public boolean isCacheWriting() {
        return type == cacheWriting;
    }

    public boolean isDownloadOnCellular() {
        return type == downloadOnCellular;
    }

    public boolean isInternal() {
        return type == internal;
    }

    public boolean isNotConnected() {
        return type == notConnected;
    }

    public boolean isCancelled() {
        return type == cancelled;
    }

    public boolean isUnexpectedContent() {
        return type == unexpectedContent;
    }

    public boolean isDatabaseWrite() {
        return type == databaseWrite;
    }

    public boolean isUnsupportedServer() {
        return type == unsupportedServer;
    }

    public boolean isSslError() {
        return type == sslError;
    }

    public boolean isServerSSLNotVerified() {
        return type == serverSSLNotVerified;
    }

    public boolean isAuthenticateWithChallenge() {
        return type == authenticateWithChallenge;
    }

    public boolean isFileContentLoading() {
        return type == fileContentLoading;
    }

    public boolean isStats() {
        return type == stats;
    }

    public boolean isDownloadReading() {
        return type == downloadReading;
    }

    public boolean isDownloadWriting() {
        return type == downloadWriting;
    }

    public boolean isNoToken() {
        return type == noToken;
    }

    public boolean isTokenExpired() {
        return type == tokenExpired;
    }

}
