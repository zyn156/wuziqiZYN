package com.example.wuziqizyn

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wuziqizyn.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReplayActivity : AppCompatActivity() {

    private lateinit var board: GameBoardView
    private lateinit var tvInfo: TextView
    private lateinit var seek: SeekBar
    private lateinit var btnStart: Button
    private lateinit var btnPrev: Button
    private lateinit var btnPlay: Button
    private lateinit var btnNext: Button
    private lateinit var btnEnd: Button

    private val db by lazy { AppDatabase.get(this) }
    private val gameDao by lazy { db.gameDao() }

    private var moves: List<Triple<Int,Int,Int>> = emptyList() // r,c,player
    private var playJob: Job? = null
    private var cur = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_replay)

        board = findViewById(R.id.replayBoard)
        tvInfo = findViewById(R.id.tvInfo)
        seek = findViewById(R.id.seek)
        btnStart = findViewById(R.id.btnStart)
        btnPrev = findViewById(R.id.btnPrev)
        btnPlay = findViewById(R.id.btnPlay)
        btnNext = findViewById(R.id.btnNext)
        btnEnd = findViewById(R.id.btnEnd)

        board.setReplayLock(true) // ★ 禁用触摸

        val gameId = intent.getLongExtra("gameId", -1L)
        if (gameId == -1L) { finish(); return }

        lifecycleScope.launch(Dispatchers.IO) {
            val game = gameDao.getAllGames().firstOrNull { it.id == gameId }
            val m = gameDao.getMovesForGame(gameId)
            moves = m.map { Triple(it.r, it.c, it.player) }
            withContext(Dispatchers.Main) {
                tvInfo.text = "复盘  |  ${if (game?.result == "BLACK_WIN") "黑胜" else "白胜"}  |  步数:${game?.steps ?: moves.size}"
                seek.max = moves.size
                cur = 0
                render()
            }
        }

        seek.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(s: SeekBar?, progress: Int, fromUser: Boolean) {
                cur = progress
                render()
            }
            override fun onStartTrackingTouch(s: SeekBar?) { stopPlay() }
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        btnStart.setOnClickListener { stopPlay(); cur = 0; render() }
        btnEnd.setOnClickListener { stopPlay(); cur = moves.size; render() }
        btnPrev.setOnClickListener { stopPlay(); if (cur > 0) { cur--; render() } }
        btnNext.setOnClickListener { stopPlay(); if (cur < moves.size) { cur++; render() } }
        btnPlay.setOnClickListener { togglePlay() }
    }

    private fun render() {
        board.renderMovesForReplay(moves, cur)
        seek.progress = cur
    }

    private fun togglePlay() {
        if (playJob != null) { stopPlay(); return }
        playJob = lifecycleScope.launch {
            while (cur < moves.size) {
                cur++
                render()
                delay(400) // 每手间隔
            }
            stopPlay()
        }
        btnPlay.text = "⏸"
    }

    private fun stopPlay() {
        playJob?.cancel(); playJob = null
        btnPlay.text = "▶"
    }
}
