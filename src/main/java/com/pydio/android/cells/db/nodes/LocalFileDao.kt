package com.pydio.android.cells.db.nodes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LocalFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(localFile: RLocalFile)

    @Update
    fun update(localFile: RLocalFile)

    @Query("DELETE FROM local_files WHERE encoded_state = :stateId")
    fun delete(stateId: String)

    @Query("DELETE FROM local_files WHERE encoded_state = :stateId and type = :type")
    fun delete(stateId: String, type: String)

    @Query("SELECT * FROM local_files WHERE encoded_state = :encodedState and type = :type LIMIT 1")
    fun getFile(encodedState: String, type: String): RLocalFile?

    @Query("SELECT * FROM local_files WHERE encoded_state = :encodedState")
    fun getFiles(encodedState: String): List<RLocalFile>

    @Query("SELECT * FROM local_files WHERE encoded_state like :encodedState || '%'")
    fun getFilesUnder(encodedState: String): List<RLocalFile>

    @Query("DELETE FROM tree_nodes WHERE encoded_state like :encodedState || '%'")
    fun deleteUnder(encodedState: String)
}
