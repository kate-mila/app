package com.mipycode.v2rayclient.data.db

import androidx.room.*
import com.mipycode.v2rayclient.data.model.ServerConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {

    @Query("SELECT * FROM servers ORDER BY isFavorite DESC, addedAt DESC")
    fun observeAll(): Flow<List<ServerConfig>>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getById(id: Long): ServerConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(server: ServerConfig): Long

    @Update
    suspend fun update(server: ServerConfig)

    @Delete
    suspend fun delete(server: ServerConfig)

    @Query("UPDATE servers SET lastPingMs = :pingMs WHERE id = :id")
    suspend fun updatePing(id: Long, pingMs: Long)

    @Query("UPDATE servers SET isFavorite = :fav WHERE id = :id")
    suspend fun updateFavorite(id: Long, fav: Boolean)
}
