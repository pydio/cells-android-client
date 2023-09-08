package com.pydio.android.cells

import androidx.test.platform.app.InstrumentationRegistry
import com.pydio.android.cells.services.NetworkService
import com.pydio.cells.utils.Log
import org.junit.Test
import org.koin.core.component.inject
import org.koin.test.AutoCloseKoinTest

/** Test that Koin configuration is valid by starting the full android test context */
class ModuleCheckTest : AutoCloseKoinTest() {

    private val logTag = "ModuleCheckTest"

    private val networkService by inject<NetworkService>()

    // Check is implicitly done: the whole App is started
    // and to-be-injected objects have already been instantiated.
    // If there is a problem, an error should be thrown during @Before phase.
    @Test
    fun simplyStartApplicationContext() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.i(logTag, "Got a context: ${context.packageName}")
//        Log.i(logTag, "Network status: ${networkService.networkStatus}")
    }
}

//    @Test
//    fun myModuleCheck() = checkModules {
//        modules(dbTestModule)
//    }

//    @Test
//    fun dryRun() {
//        val context = InstrumentationRegistry.getInstrumentation().targetContext
//        startKoin {
//            printLogger(Level.DEBUG)
//            androidContext(context)
//            modules(dbTestModule)
//        }.checkModules()
//    }
