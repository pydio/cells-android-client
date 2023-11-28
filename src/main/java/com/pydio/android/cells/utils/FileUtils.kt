package com.pydio.android.cells.utils

import com.pydio.cells.api.SDKException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

//
//fun String.toMD5(): String {
//    val bytes = MessageDigest.getInstance("MD5").digest(this.toByteArray())
//    return bytes.toHex()
//}
//
//fun ByteArray.toHex(): String {
//    return joinToString("") { "%02x".format(it) }
//}

fun computeFileMd5(file: File): String {
    try {
        FileInputStream(file).use { inputStream ->
            val digest = MessageDigest.getInstance("MD5")
            val bytesBuffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(bytesBuffer).also { bytesRead = it } != -1) {
                digest.update(bytesBuffer, 0, bytesRead)
            }
            val hashedBytes = BigInteger(1, digest.digest())
            return hashedBytes.toString(16)
        }
    } catch (ex: NoSuchAlgorithmException) {
        // This should never happen
        throw SDKException(
            "Could not generate hash from file ${file.name}", ex
        )
    } catch (ex: IOException) {
        throw SDKException(
            "Could not generate hash from file ${file.name}", ex
        )
    }
}

fun formatBytesToMB(bytes: Long): String {
    val sizeInMB = bytes / (1024.0 * 1024.0)
    return String.format("%.1f MB", sizeInMB)
}
