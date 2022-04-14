package com.pydio.android.cells.db

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pydio.cells.transport.StateID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.accounts.AccountDB
import com.pydio.android.cells.db.accounts.AccountDao
import com.pydio.android.cells.db.accounts.RAccount
import java.io.IOException

/**
 * First draft of DB tests
 */
@RunWith(AndroidJUnit4::class)
class AccountDBTest {

    private lateinit var accountDao: AccountDao
    private lateinit var db: AccountDB

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // We don't need to persist to file, all data vanish on close.
        db = Room.inMemoryDatabaseBuilder(context, AccountDB::class.java)
            // avoid exceptions when calling from the main thread.
            .allowMainThreadQueries()
            .build()
        accountDao = db.accountDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
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
        return RAccount(
            accountID = StateID(username, url).accountId,
            username = username,
            url = url,
            serverLabel = "A dummy server to test",
            tlsMode = if (skipVerify) 1 else 0,
            isLegacy = false,
            welcomeMessage = "Welcome message",
            authStatus = AppNames.AUTH_STATUS_NEW,
        )
    }
}
