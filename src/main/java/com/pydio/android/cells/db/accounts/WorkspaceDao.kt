package com.pydio.android.cells.db.accounts

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface WorkspaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(workspace: RWorkspace)

    @Update
    fun update(workspace: RWorkspace)

    @Query("DELETE FROM workspaces WHERE encoded_state = :encodedState")
    fun forgetWorkspace(encodedState: String)

    @Query("SELECT * FROM workspaces WHERE encoded_state = :encodedState")
    fun getWorkspace(encodedState: String): RWorkspace?

    @Query("SELECT * FROM workspaces WHERE encoded_state like :accountId || '%' ORDER BY sort_name")
    fun getWorkspaces(accountId: String): List<RWorkspace>

    @Query("SELECT * FROM workspaces WHERE encoded_state like :accountId || '%' ORDER BY sort_name")
    fun getLiveWorkspaces(accountId: String): LiveData<List<RWorkspace>>

    @Query("SELECT * FROM workspaces WHERE encoded_state like :encodedParentStateID || '%' ORDER BY slug")
    fun getWsForDiff(encodedParentStateID: String): List<RWorkspace>

    @Query("DELETE FROM workspaces WHERE encoded_state like :accountID || '%'")
    fun forgetAccount(accountID: String)
}
