package com.pydio.android.cells

import androidx.test.platform.app.InstrumentationRegistry
import com.pydio.android.cells.services.NetworkService
import com.pydio.cells.utils.Log
import org.junit.Test
import org.koin.core.component.inject
import org.koin.test.AutoCloseKoinTest

/** Simply insure that the Koin configuration is valid by starting the full android test context */
class ModuleCheckTest : AutoCloseKoinTest() {

    private val logTag = ModuleCheckTest::class.simpleName

    private val networkService by inject<NetworkService>()

    @Test
    fun doNothing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.i(logTag, "Got a context: ${context.packageName}")
        Log.i(logTag, "Network status: ${networkService.networkInfo()?.status}")
//        Log.i(logTag, "Current state: ${CellsApp.instance.getCurrentState()}")
    }

    // Check is implicitly done: the whole App is started
    // and to-be-injected objects have already been instantiated.
    // If there is a problem, an error has already been thrown

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

}
