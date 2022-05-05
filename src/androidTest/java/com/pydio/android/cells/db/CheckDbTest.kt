package com.pydio.android.cells.db

// import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry
import com.pydio.android.cells.CellsApp
import com.pydio.cells.utils.Log
import org.junit.Test
import org.junit.experimental.categories.Category
import org.koin.test.AutoCloseKoinTest
import org.koin.test.category.CheckModuleTest
import org.koin.test.check.checkModules


//@Category(CheckModuleTest::class)
class ModuleCheckTest : AutoCloseKoinTest() {

    private val logTag = ModuleCheckTest::class.simpleName

    @Test
    fun doNothing() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Log.i(logTag, "Got a context: ${context.packageName}")
        Log.i(logTag, "Current state: ${CellsApp.instance.getCurrentState()}")
    }

//    @Test
//    fun myModuleCheck() = checkModules {
////        val context = InstrumentationRegistry.getInstrumentation().targetContext
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

//@RunWith(AndroidJUnit4::class)
//class CheckDbTest : AutoCloseKoinTest() {
//
//
//}