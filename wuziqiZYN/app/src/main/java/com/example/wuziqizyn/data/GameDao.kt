package com.example.wuziqizyn.data

import androidx.room.*

@Dao
interface GameDao {

    // ---- 写入 ----
    @Insert
    suspend fun insertGame(game: GameEntity): Long

    @Insert
    suspend fun insertMoves(moves: List<MoveEntity>)

    // ---- 查询 ----
    @Query("SELECT * FROM game ORDER BY id DESC")
    suspend fun getAllGames(): List<GameEntity>

    @Query("SELECT * FROM move WHERE gameId = :gameId ORDER BY seq ASC")
    suspend fun getMovesForGame(gameId: Long): List<MoveEntity>
}
