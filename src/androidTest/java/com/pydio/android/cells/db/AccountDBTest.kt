package com.pydio.android.cells.db

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pydio.android.cells.LoginStatus
import com.pydio.android.cells.db.accounts.AccountDB
import com.pydio.android.cells.db.accounts.AccountDao
import com.pydio.android.cells.db.accounts.RAccount
import com.pydio.android.cells.db.runtime.RuntimeDB
import com.pydio.cells.transport.StateID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.*

/**
 * First draft of DB tests
 */
@RunWith(AndroidJUnit4::class)
class AccountDBTest {

    private lateinit var accountDao: AccountDao
    private var accountDB: AccountDB? = null
    private var runtimeDB: RuntimeDB? = null

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // We don't need to persist to file, all data vanish on close.
        accountDB = Room.inMemoryDatabaseBuilder(context, AccountDB::class.java)
            // avoid exceptions when calling from the main thread.
            .allowMainThreadQueries()
            .build()
        accountDao = accountDB!!.accountDao()

        runtimeDB = Room.inMemoryDatabaseBuilder(context, RuntimeDB::class.java)
            .allowMainThreadQueries().build()

    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        accountDB?.close()
        runtimeDB?.close()
    }

    @Test
    @Throws(Exception::class)
    fun crudAccount() {
        val account = dummyAccount("john", "https://example.com", false)
        accountDao.insert(account)
        var allAccounts = accountDao.getAccounts()
        assertEquals(1, allAccounts.size)

        val account2 = dummyAccount("louisa", "https://other.example.com", true)
        accountDao.insert(account2)
        allAccounts = accountDao.getAccounts()
        assertEquals(2, allAccounts.size)
    }

    private fun dummyAccount(username: String, url: String, skipVerify: Boolean): RAccount {

        val props = Properties()
        props.setProperty(RAccount.KEY_SERVER_LABEL, "Welcome message")
        props.setProperty(RAccount.KEY_WELCOME_MESSAGE, "A dummy server to test")

        return RAccount(
            accountId = StateID(username, url).accountId,
            username = username,
            url = url,
            tlsMode = if (skipVerify) 1 else 0,
            isLegacy = false,
            authStatus = LoginStatus.New.id,
            properties = props,
        )
    }
}
