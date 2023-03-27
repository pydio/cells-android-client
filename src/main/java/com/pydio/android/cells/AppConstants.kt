package com.pydio.android.cells

enum class RemoteType {
    P8, CELLS
}

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

enum class ListType {
    DEFAULT, TRANSFER, JOB
}
