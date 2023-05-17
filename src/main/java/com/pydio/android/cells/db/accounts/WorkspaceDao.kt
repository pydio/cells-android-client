package com.pydio.android.cells.db.accounts

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(workspace: RWorkspace)

    @Update
    fun update(workspace: RWorkspace)

    @Query("SELECT * FROM workspaces WHERE encoded_state = :encodedState")
    fun getWorkspace(encodedState: String): RWorkspace?

    @Query("SELECT * FROM workspaces WHERE encoded_state like :accountId || '%' ORDER BY sort_name")
    fun getWorkspaces(accountId: String): List<RWorkspace>

    @Query("DELETE FROM workspaces WHERE encoded_state = :encodedState")
    fun forgetWorkspace(encodedState: String)

    @Query("DELETE FROM workspaces WHERE encoded_state like :accountID || '%'")
    fun forgetAccount(accountID: String)

    @Query("SELECT * FROM workspaces WHERE encoded_state like :encodedParentStateID || '%' ORDER BY slug")
    fun getWsForDiff(encodedParentStateID: String): List<RWorkspace>

    @Query("SELECT * FROM workspaces WHERE encoded_state LIKE :accountId || '%' AND sort_name LIKE '8%' ORDER BY sort_name")
    fun getCellsFlow(accountId: String): Flow<List<RWorkspace>>

    @Query("SELECT * FROM workspaces WHERE encoded_state LIKE :accountId || '%' AND sort_name NOT LIKE '8%' ORDER BY sort_name")
    fun getNotCellsFlow(accountId: String): Flow<List<RWorkspace>>
}
