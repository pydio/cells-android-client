package com.pydio.android.cells.db.nodes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.pydio.android.cells.db.CellsConverters
import kotlinx.coroutines.flow.Flow

@Dao
@TypeConverters(CellsConverters::class)
interface TreeNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(treeNode: RTreeNode)

    @Update
    fun update(treeNode: RTreeNode)

    @Query("SELECT * FROM tree_nodes WHERE encoded_state = :encodedState LIMIT 1")
    fun getNode(encodedState: String): RTreeNode?

    @Query("SELECT * FROM tree_nodes WHERE uuid = :uuid")
    fun getNodesByUuid(uuid: String): List<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :stateId || '%'")
    fun getUnder(stateId: String): List<RTreeNode>

    @Query("DELETE FROM tree_nodes WHERE encoded_state = :stateId")
    fun delete(stateId: String)

    @Query("DELETE FROM tree_nodes WHERE encoded_state like :stateId || '%'")
    fun deleteUnder(stateId: String)

    @RawQuery
    fun searchQuery(query: SupportSQLiteQuery): List<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE name like '%' ||  :name || '%' LIMIT 100")
    fun query(name: String): List<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath AND mime = :mime ORDER BY sort_name")
    fun listWithMime(
        encodedParentStateID: String,
        parentPath: String,
        mime: String
    ): List<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath ORDER BY name")
    fun getNodesForDiff(encodedParentStateID: String, parentPath: String): List<RTreeNode>

    // Reactive queries
    @RawQuery(observedEntities = [RTreeNode::class])
    fun searchQueryFlow(query: SupportSQLiteQuery): Flow<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath AND mime = :mime ORDER BY sort_name")
    fun lsWithMimeFlow(
        encodedParentStateID: String,
        parentPath: String,
        mime: String
    ): Flow<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE name like '%' ||  :name || '%' LIMIT 100")
    fun simpleQueryFlow(name: String): Flow<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath AND mime like :mime || '%' ORDER BY sort_name")
    fun lsWithMimeFilterFlow(
        encodedParentStateID: String,
        parentPath: String,
        mime: String
    ): Flow<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath AND mime like :mime || '%' ORDER BY sort_name")
    fun lsWithMimeFilter(
        encodedParentStateID: String,
        parentPath: String,
        mime: String
    ): Flow<List<RTreeNode>>
}
