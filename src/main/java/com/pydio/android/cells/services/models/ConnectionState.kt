package com.pydio.android.cells.services.models

import com.pydio.android.cells.LoadingState
import com.pydio.android.cells.ServerConnection

data class ConnectionState(val loading: LoadingState, val serverConnection: ServerConnection)