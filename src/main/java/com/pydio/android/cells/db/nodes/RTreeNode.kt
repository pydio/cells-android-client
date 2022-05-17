package com.pydio.android.cells.db.nodes

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.Converters
import com.pydio.android.cells.utils.getMimeType
import com.pydio.cells.api.SdkNames
import com.pydio.cells.api.ui.FileNode
import com.pydio.cells.api.ui.WorkspaceNode
import com.pydio.cells.transport.StateID
import com.pydio.cells.utils.Str
import java.util.Properties

@Entity(tableName = "tree_nodes")
@TypeConverters(Converters::class)
data class RTreeNode(

    @PrimaryKey
    @ColumnInfo(name = "encoded_state") val encodedState: String,

    @ColumnInfo(name = "uuid") val uuid: String,

    @ColumnInfo(name = "workspace") val workspace: String,

    @ColumnInfo(name = "parent_path") val parentPath: String,

    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "mime") var mime: String,

    @ColumnInfo(name = "etag") var etag: String?,

    @ColumnInfo(name = "size") var size: Long = -1L,

    @ColumnInfo(name = "remote_mod_ts") var remoteModificationTS: Long,

    @ColumnInfo(name = "last_check_ts") var lastCheckTS: Long = 0L,

    @ColumnInfo(name = "local_mod_ts") var localModificationTS: Long = 0L,

    @ColumnInfo(name = "local_mod_status") var localModificationStatus: String? = null,

    @ColumnInfo(name = "thumb") var thumbFilename: String? = null,

    // Can be: none, cache, offline, external
    @ColumnInfo(name = "local_file_type") var localFileType: String = AppNames.LOCAL_FILE_TYPE_NONE,

    // When necessary, we store the full path to the
    // relevant local resource somewhere in the external storage.
    @ColumnInfo(name = "localPath") var localFilePath: String? = null,

    // We store all the well known properties that we use
    @ColumnInfo(name = "properties") val properties: Properties,

    // Arbitrary Key - Values to locally store meta exposed by the remote server
    // (being Cells or a Legacy P8)
    @ColumnInfo(name = "meta") val meta: Properties,

    // In the SDK Java layer, we compute a hash of the meta returned by the remote server
    // to ease later diff processing
    @ColumnInfo(name = "meta_hash") val metaHash: Int,

    // Default order column to simply display folders before files before the recycle
    @ColumnInfo(name = "sort_name") var sortName: String? = null,

    // Ease query against a given characteristic of the nodes (bookmarked, shared...)
    @ColumnInfo(name = "flags") var flags: Int = 0,

    ) {

    fun getStateID(): StateID {
        return StateID.fromId(encodedState)
    }

    fun isFolder(): Boolean {
        return isFolderFromMime(mime)
    }

    fun isFile(): Boolean {
        return !isFolder()
    }

    fun isInRecycle(): Boolean {
        return parentPath.startsWith("/${SdkNames.RECYCLE_BIN_NAME}")
    }

    fun isRecycle(): Boolean {
        return name == SdkNames.RECYCLE_BIN_NAME
    }

    /** Returns the updated flags (why not?) */
    private fun setFlag(flag: Int, value: Boolean): Int {
        // TODO smelly code
        if (isFlag(flag)) {
            if (!value) {
                // removeFlag(flag)
                flags = flags and flag.inv()
            }
        } else if (value) {
            flags = flags or flag
        }
        return flags
    }

    private fun isFlag(flag: Int): Boolean {
        return flags and flag == flag
    }

    fun toFileNode(): FileNode {
        // TODO double check: we might drop some info that we have missed on first draft implementation
        //   Rather directly use the properties
        val fn = FileNode()
        fn.setProperty(SdkNames.NODE_PROPERTY_UID, uuid)
        fn.setProperty(SdkNames.NODE_PROPERTY_ETAG, etag)
        fn.setProperty(SdkNames.NODE_PROPERTY_MTIME, "$remoteModificationTS")
        fn.setProperty(SdkNames.NODE_PROPERTY_PATH, getStateID().path)
        fn.setProperty(SdkNames.NODE_PROPERTY_WORKSPACE_SLUG, workspace)
        fn.setProperty(SdkNames.NODE_PROPERTY_FILENAME, name)
        fn.setProperty(SdkNames.NODE_PROPERTY_IS_FILE, "${isFile()}")
        fn.setProperty(SdkNames.NODE_PROPERTY_MIME, mime)
        fn.setProperty(SdkNames.NODE_PROPERTY_BYTESIZE, "$size")
        return fn
    }

    fun isContentEquals(
        newItem: RTreeNode
    ): Boolean {
        var same = remoteModificationTS == newItem.remoteModificationTS
                && localModificationTS == newItem.localModificationTS
                && flags == newItem.flags

        if (same && newItem.thumbFilename != null) {
            same = newItem.thumbFilename.equals(thumbFilename)
        }

        // With Room: we should get equality based on equality of each fields (column) for free
        // (RTreeNode is a @Data class). But this doesn't work for now, so we rather only check:
        // remote modification timestamp and thumb filename.

        // More logs to investigate
        if (!same) {
            Log.d(logTag, "Found new content for $encodedState")
            Log.d(
                logTag, "Old TS: ${remoteModificationTS}, " +
                        "new TS: ${newItem.remoteModificationTS}"
            )
            Log.d(
                logTag, "Local Old TS: ${localModificationTS}, " +
                        "new TS: ${newItem.localModificationTS}"
            )
            Log.d(
                logTag, "Old thumb: ${thumbFilename}, " +
                        "new thumb: ${newItem.thumbFilename}"
            )
            Log.d(
                logTag, "Old flags: ${flags.showFlags()}, " +
                        "new flags: ${newItem.flags.showFlags()}"
            )
        }
        return same
    }

    companion object {
        private val logTag = RTreeNode::class.simpleName

        fun fromFileNode(stateID: StateID, fileNode: FileNode): RTreeNode {
            Log.d(logTag, "... fromFileNode $stateID")
//            Log.w(logTag, "  - WS: ${fileNode.workspace}")
//            Log.w(logTag, "  - Path: ${fileNode.path}")
//            Log.w(logTag, "  - Label: ${fileNode.name}")
            val childStateID = // Retrieve the account from the passed state
                StateID.fromId(stateID.accountId)
                    // Construct the path from file node info
                    .withPath("/${fileNode.workspace}${fileNode.path}")
            Log.d(logTag, "  - encodedState: ${childStateID.id}")

            try {
                val node = RTreeNode(
                    encodedState = childStateID.id,
                    workspace = childStateID.workspace,
                    parentPath = childStateID.parentFile ?: "",
                    name = childStateID.fileName ?: run {
                        Log.e(logTag, "Using slug instead of filename")
                        childStateID.workspace
                    },
                    uuid = fileNode.id,
                    etag = fileNode.eTag,
                    mime = fileNode.mimeType,
                    size = fileNode.size,
                    remoteModificationTS = fileNode.lastModified,
                    properties = fileNode.properties,
                    meta = fileNode.meta ?: Properties(),
                    metaHash = fileNode.metaHashCode
                )

                // Share and offline cache values are rather handled in the NodeService directly
                node.setBookmarked(fileNode.isBookmark)

                // Use Android library to precise MimeType when possible
                if (SdkNames.NODE_MIME_DEFAULT == node.mime) {
                    node.mime = getMimeType(node.name, SdkNames.NODE_MIME_DEFAULT)
                }

                node.sortName = when (node.mime) {
                    SdkNames.NODE_MIME_WS_ROOT -> "1_${node.name}"
                    SdkNames.NODE_MIME_FOLDER -> "3_${node.name}"
                    SdkNames.NODE_MIME_RECYCLE -> "8_${node.name}"
                    else -> "5_${node.name}"
                }
                return node

            } catch (e: java.lang.Exception) {
                Log.e(logTag, "could not create RTreeNode for ${childStateID}: ${e.message}")
                throw e
            }
        }

        fun fromWorkspaceNode(stateID: StateID, node: WorkspaceNode): RTreeNode {
            try {
                val currSortName = when (node.workspaceType) {
                    SdkNames.WS_TYPE_PERSONAL -> "1_2_${node.name}"
                    SdkNames.WS_TYPE_CELL -> "1_8_${node.name}"
                    else -> "1_5_${node.name}"
                }

                val nodeUuid = if (Str.notEmpty(node.id)) node.id else node.slug

                // We force the slash at the end of the encoded state to ease later handling
                val storedID = // Retrieve the account from the passed state
                    StateID.fromId(stateID.accountId).withPath("/${node.slug}")

                return RTreeNode(
                    encodedState = storedID.id,
                    workspace = storedID.workspace,
                    parentPath = "",
                    name = node.name,
                    uuid = nodeUuid,
                    etag = "",
                    mime = SdkNames.NODE_MIME_WS_ROOT,
                    size = 0L,
                    remoteModificationTS = 0,
                    properties = node.properties,
                    // FIXME
                    meta = Properties(),
                    // TODO manage this
                    metaHash = 0,
                    sortName = currSortName,
                )
            } catch (e: java.lang.Exception) {
                Log.e(
                    logTag, "could not create RTreeNode for " +
                            "ws root ${node.slug} at ${stateID}: ${e.message}"
                )
                throw e
            }
        }

        fun isFolderFromMime(mime: String): Boolean {
            return mime == SdkNames.NODE_MIME_FOLDER
                    || mime == SdkNames.NODE_MIME_WS_ROOT
                    || mime == SdkNames.NODE_MIME_RECYCLE
        }
    }

    // Boiler plate shortcuts

    fun isBookmarked(): Boolean {
        return isFlag(AppNames.FLAG_BOOKMARK)
    }

    fun setBookmarked(value: Boolean): Int {
        return setFlag(AppNames.FLAG_BOOKMARK, value)
    }

    fun isShared(): Boolean {
        return isFlag(AppNames.FLAG_SHARE)
    }

    fun getShareAddress(): String? {
        return properties.getProperty(SdkNames.NODE_PROPERTY_SHARE_LINK, null)
    }

    fun setShared(isShared: Boolean, linkURL: String?) {
        setFlag(AppNames.FLAG_SHARE, isShared)
        if (isShared) {
            properties.setProperty(SdkNames.NODE_PROPERTY_SHARE_LINK, linkURL)
        } else {
            properties.remove(SdkNames.NODE_PROPERTY_SHARE_LINK)
        }
    }

    fun isOfflineRoot(): Boolean {
        return isFlag(AppNames.FLAG_OFFLINE)
    }

    fun setOfflineRoot(value: Boolean): Int {
        return setFlag(AppNames.FLAG_OFFLINE, value)
    }
}

// Only 3 flags are defined (TODO use bit shifting and constants)
fun Int.showFlags(): String =
    Integer.toBinaryString(this).padStart(3, '0')

// Prints the full range of all possible flags
fun Int.debugAsString(): String =
    Integer.toBinaryString(this).padStart(Int.SIZE_BITS, '0')
