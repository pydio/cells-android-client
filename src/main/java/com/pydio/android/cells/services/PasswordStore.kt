package com.pydio.android.cells.services

import com.pydio.android.cells.db.auth.LegacyCredentialsDao
import com.pydio.android.cells.db.auth.RLegacyCredentials
import com.pydio.cells.api.Store

class PasswordStore(private val dao: LegacyCredentialsDao) : Store<String> {

    override fun put(id: String, password: String) {
        val cred = RLegacyCredentials(accountID = id, password = password)
        if (dao.getCredential(id) == null) {
            dao.insert(cred)
        } else {
            dao.update(cred)
        }
    }

    override fun get(id: String): String? {
        return dao.getCredential(id)?.password
    }

    override fun remove(id: String) {
        dao.forgetPassword(id)
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun getAll(): MutableMap<String, String> {
        val allCreds: MutableMap<String, String> = HashMap()
        for (cred in dao.getAll()) {
            allCreds[cred.accountID] = cred.password
        }
        return allCreds
    }
}
