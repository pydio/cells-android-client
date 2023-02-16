package com.pydio.android.cells

enum class JobStatus(val id: String) {
    NEW("new"),
    PROCESSING("processing"),
    CANCELLED("cancelled"),
    DONE("done"),
    WARNING("warning"),
    ERROR("error"),
    TIMEOUT("timeout"),
    NO_FILTER("no filter"),
}


enum class NodeAction(val id: String) {
    DOWNLOAD_TO_DEVICE("download_to_device"),
    RENAME("rename"),
    COPY_TO("copy_to"),
    MOVE_TO("move_to"),
    CREATE_SHARE("create_share"),
    MANAGE_SHARE("manage_share"),
    TOGGLE_OFFLINE("toggle_offline"),
    TOGGLE_BOOKMARK("toggle_bookmark"),
    DELETE("delete"),
    DELETE_FOREVER("delete_forever"),
    EMPTY_RECYCLE("empty_recycle"),
    SELECT_TARGET_FOLDER("select_target_folder"),
}
