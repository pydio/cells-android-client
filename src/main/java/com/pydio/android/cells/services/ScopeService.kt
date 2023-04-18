package com.pydio.android.cells.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.core.component.KoinComponent

class ScopeService : KoinComponent {

    private val logTag = "ScopeService"
    val appScope = CoroutineScope(SupervisorJob())
}
