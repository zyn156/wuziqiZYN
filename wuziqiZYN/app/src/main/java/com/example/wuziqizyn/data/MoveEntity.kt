package com.example.wuziqizyn.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "move",
    indices = [Index("gameId")]
)
data class MoveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val gameId: Long,
    val seq: Int,     // 第几手，从0开始
    val r: Int,
    val c: Int,
    val player: Int   // 1 黑 2 白
)
