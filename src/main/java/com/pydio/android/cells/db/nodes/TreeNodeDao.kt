package com.pydio.android.cells.db.nodes

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.TypeConverters
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.pydio.android.cells.AppNames
import com.pydio.android.cells.db.CellsConverters
import kotlinx.coroutines.flow.Flow

@Dao
@TypeConverters(CellsConverters::class)
interface TreeNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(treeNode: RTreeNode)

    @Update
    fun update(treeNode: RTreeNode)

    @Query("DELETE FROM tree_nodes WHERE encoded_state = :stateId")
    fun delete(stateId: String)

    @Query("DELETE FROM tree_nodes WHERE encoded_state like :stateId || '%'")
    fun deleteUnder(stateId: String)

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :stateId || '%'")
    fun getUnder(stateId: String): List<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state = :encodedState LIMIT 1")
    fun getLiveNode(encodedState: String): LiveData<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state IN (:encodedIds) ")
    fun getLiveNodes(vararg encodedIds: String): LiveData<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state = :encodedState LIMIT 1")
    fun getNode(encodedState: String): RTreeNode?

    @Query("SELECT * FROM tree_nodes WHERE uuid = :uuid")
    fun getNodesByUuid(uuid: String): List<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath ORDER BY :order ")
    fun ls(
        encodedParentStateID: String,
        parentPath: String,
        order: String
    ): LiveData<List<RTreeNode>>

    @RawQuery(observedEntities = [RTreeNode::class])
    fun lsFlow(query: SupportSQLiteQuery): Flow<List<RTreeNode>>

    @RawQuery(observedEntities = [RTreeNode::class])
    fun treeNodeQuery(query: SupportSQLiteQuery): LiveData<List<RTreeNode>>

    @RawQuery(observedEntities = [RTreeNode::class])
    fun liveSearchQuery(query: SupportSQLiteQuery): LiveData<List<RTreeNode>>

    @RawQuery
    fun searchQuery(query: SupportSQLiteQuery): List<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath ORDER BY name")
    fun getNodesForDiff(encodedParentStateID: String, parentPath: String): List<RTreeNode>

    // Quick and rather dirty workaround to provide an empty live data when necessary
    @Query("SELECT * FROM tree_nodes WHERE encoded_state = 'NONE' LIMIT 100")
    fun emptyLiveQuery(): LiveData<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE name like '%' ||  :name || '%' LIMIT 100")
    fun flowQuery(name: String): Flow<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE name like '%' ||  :name || '%' LIMIT 100")
    fun liveQuery(name: String): LiveData<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE name like '%' ||  :name || '%' LIMIT 100")
    fun query(name: String): List<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath AND mime = :mime ORDER BY sort_name")
    fun lsWithMime(
        encodedParentStateID: String,
        parentPath: String,
        mime: String
    ): LiveData<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath AND mime = :mime ORDER BY sort_name")
    fun lsWithMimeFlow(
        encodedParentStateID: String,
        parentPath: String,
        mime: String
    ): Flow<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath AND mime like :mime || '%' ORDER BY sort_name")
    fun lsWithMimeFilterFlow(
        encodedParentStateID: String,
        parentPath: String,
        mime: String
    ): Flow<List<RTreeNode>>


    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath AND mime = :mime ORDER BY sort_name")
    fun listWithMime(
        encodedParentStateID: String,
        parentPath: String,
        mime: String
    ): List<RTreeNode>

    @Query("SELECT * FROM tree_nodes WHERE encoded_state like :encodedParentStateID || '%' AND parent_path = :parentPath AND mime like :mime || '%' ORDER BY sort_name")
    fun lsWithMimeFilter(
        encodedParentStateID: String,
        parentPath: String,
        mime: String
    ): LiveData<List<RTreeNode>>

    @Query("SELECT * FROM tree_nodes WHERE flags & :flag = :flag ORDER BY sort_name")
    fun getBookmarked(flag: Int = AppNames.FLAG_BOOKMARK): LiveData<List<RTreeNode>>
}
