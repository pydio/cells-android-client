package com.pydio.android.cells.utils;

import android.util.Base64;

import com.pydio.cells.api.CustomEncoder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AndroidCustomEncoder implements CustomEncoder {

    @Override
    public byte[] base64Encode(byte[] data) {
        return Base64.encode(data, Base64.DEFAULT | Base64.NO_WRAP | Base64.NO_PADDING | Base64.NO_CLOSE);
    }

    @Override
    public String base64Encode(String inValue) {
        return new String(base64Encode(inValue.getBytes()));
    }


    @Override
    public byte[] base64Decode(byte[] data) {
        return Base64.decode(data, Base64.DEFAULT | Base64.NO_WRAP | Base64.NO_PADDING | Base64.NO_CLOSE);
    }

    @Override
    public String base64Decode(String s) {
        byte[] data = s.getBytes(StandardCharsets.UTF_8);
        return new String(base64Decode(data), StandardCharsets.UTF_8);
    }

    @Override
    public String utf8Encode(String value) {
        // TODO this method throws an exception in android context but not in the sdk-java, why ?
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected encoding issue", e);
        }
        //return super.utf8Encode(value);
    }

    @Override
    public String utf8Decode(String value) {
        // TODO this method throws an exception in android context but not in the sdk-java, why ?
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unexpected decoding issue", e);
        }
        //return super.utf8Encode(value);
    }

    @Override
    public byte[] getUTF8Bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }
}
