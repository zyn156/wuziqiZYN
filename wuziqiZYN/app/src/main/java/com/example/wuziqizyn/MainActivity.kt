package com.example.wuziqizyn


import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wuziqizyn.data.AppDatabase
import com.example.wuziqizyn.data.GameEntity
import com.example.wuziqizyn.data.MoveEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), GameBoardView.Listener {

    private lateinit var tvStatus: TextView
    private lateinit var board: GameBoardView
    private lateinit var btnUndo: Button
    private lateinit var btnReset: Button
    private lateinit var btnHistory: Button
    private lateinit var spMode: Spinner

    // 内存里暂存走棋（避免悔棋复杂度）
    private val moveBuffer = mutableListOf<Triple<Int,Int,Int>>() // r,c,player
    private var gameStartTime: Long = System.currentTimeMillis()

    //  DB
    private val db by lazy { AppDatabase.get(this) }
    private val gameDao by lazy { db.gameDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        board = findViewById(R.id.boardView)
        btnUndo = findViewById(R.id.btnUndo)
        btnReset = findViewById(R.id.btnReset)
        btnHistory = findViewById(R.id.btnHistory)
        spMode = findViewById(R.id.spMode)

        board.listener = this

        val modes = listOf("本地双人","人机（我先手）","人机（AI先手）")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modes)
        spMode.adapter = adapter
        spMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                board.mode = when (pos) {
                    0 -> GameBoardView.Mode.LOCAL_TWO_PLAYERS
                    1 -> GameBoardView.Mode.HUMAN_FIRST_AI
                    else -> GameBoardView.Mode.AI_FIRST_HUMAN
                }
                tvStatus.text = statusText(1) + "（" + modes[pos] + "）"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnUndo.setOnClickListener {
            it.isEnabled = false
            board.undo()
            it.postDelayed({ it.isEnabled = true }, 150)
        }
        btnReset.setOnClickListener {
            board.reset() // onReset() 回调里会清空 moveBuffer & 重置时间
        }
        btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        tvStatus.text = statusText(1) + "（本地双人）"
        gameStartTime = System.currentTimeMillis()
    }

    private fun statusText(player: Int): String {
        val who = if (player == 1) "黑棋" else "白棋"
        return "${who}走"
    }

    // ========== GameBoardView.Listener 实现 ==========
    override fun onTurnChanged(player: Int) {
        val modeText = spMode.selectedItem.toString()
        tvStatus.text = statusText(player) + "（$modeText）"
    }

    override fun onWin(winner: Int) {
        val who = if (winner == 1) "黑棋" else "白棋"
        Toast.makeText(this, "$who 胜利！已保存对局。", Toast.LENGTH_SHORT).show()
        val modeText = spMode.selectedItem.toString()
        tvStatus.text = "$who 胜利（$modeText）"

        // 保存到数据库
        val end = System.currentTimeMillis()
        val resultLabel = if (winner == 1) "BLACK_WIN" else "WHITE_WIN"
        val steps = moveBuffer.size
        val firstPlayer = 1
        val boardSize = 15
        val modeLabel = modeText

        val snapshot = moveBuffer.toList()
        lifecycleScope.launch(Dispatchers.IO) {
            val gameId = gameDao.insertGame(
                GameEntity(
                    startedAt = gameStartTime,
                    endedAt = end,
                    result = resultLabel,
                    firstPlayer = firstPlayer,
                    boardSize = boardSize,
                    steps = steps,
                    modeLabel = modeLabel
                )
            )
            val entities = snapshot.mapIndexed { idx, t ->
                MoveEntity(
                    gameId = gameId,
                    seq = idx,
                    r = t.first, c = t.second, player = t.third
                )
            }
            gameDao.insertMoves(entities)
        }
    }

    override fun onModeInfo(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    // 收集走棋
    override fun onMovePlaced(r: Int, c: Int, player: Int, seq: Int) {
        if (seq == 0) gameStartTime = System.currentTimeMillis()
        // 防止越界：如果悔棋后再次走子，seq 位置覆盖后续
        if (seq < moveBuffer.size) {
            moveBuffer.subList(seq, moveBuffer.size).clear()
        }
        moveBuffer.add(Triple(r, c, player))
    }

    // 同步悔棋
    override fun onUndo(r: Int, c: Int, player: Int, newSeq: Int) {
        if (newSeq >= 0 && newSeq < moveBuffer.size) {
            moveBuffer.subList(newSeq, moveBuffer.size).clear()
        }
    }

    // 重开时清空缓存 & 时间
    override fun onReset() {
        moveBuffer.clear()
        gameStartTime = System.currentTimeMillis()
    }
}
