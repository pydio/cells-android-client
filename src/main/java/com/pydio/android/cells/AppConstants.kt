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


enum class CellsActions(val id: String) {
    DOWNLOAD_TO_DEVICE("download_to_device"),
    RENAME("rename"),
}
