package com.pydio.android.cells.db.nodes

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.TypeConverters
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.Converters
import com.pydio.android.cells.utils.currentTimestamp
import com.pydio.cells.transport.StateID
import java.io.File

@Entity(
    tableName = "local_files",
    primaryKeys = [
        "encoded_state",
        "type"
    ],
)
@TypeConverters(Converters::class)
data class RLocalFile(

    @ColumnInfo(name = "encoded_state") val encodedState: String,

    // Thumb, Preview, Cache, Offline
    @ColumnInfo(name = "type") val type: String,

    // Might be the file (for thumbs, preview) or the rel path from base dir (real files)
    // e.g. common-files/test/my-image.jpg
    @ColumnInfo(name = "file") val file: String,

    // The e-tag of the **main** file: it is used to detect when the file is probably out-dated
    // and thus that we need to retrigger the download of the corresponding file.
    @ColumnInfo(name = "etag") var etag: String?,

    @ColumnInfo(name = "size") var size: Long = -1L,

    @ColumnInfo(name = "remote_mod_ts") var remoteTS: Long,

    @ColumnInfo(name = "local_mod_ts") var localTS: Long = -1L,
) {

    companion object {
        private val logTag = RLocalFile::class.simpleName

        fun fromFile(
            stateID: StateID,
            type: String,
            file: File,
            eTag: String?,
            remoteTS: Long,
        ): RLocalFile {
            val filename = if (type == AppNames.LOCAL_FILE_TYPE_FILE) {
                stateID.path.substring(1) // we remove the leading / for easier later use
            } else {
                file.name
            }

            return RLocalFile(
                encodedState = stateID.id,
                type = type,
                file = filename,
                etag = eTag,
                size = file.length(),
                remoteTS = remoteTS,
                localTS = currentTimestamp()
            )
        }
    }
}
