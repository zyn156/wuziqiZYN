package com.example.wuziqizyn.data


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAt: Long,          // 开局时间
    val endedAt: Long,            // 终局时间
    val result: String,           // "BLACK_WIN" / "WHITE_WIN"
    val firstPlayer: Int,         // 1 黑 2 白
    val boardSize: Int,           // 15
    val steps: Int,               // 总步数
    val modeLabel: String         // "本地双人"/"人机（我先手）"/"人机（AI先手）"
)
