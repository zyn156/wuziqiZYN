package com.example.wuziqizyn

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class GameBoardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    //接口回调
    interface Listener {
        fun onTurnChanged(player: Int) // 1黑 2白
        fun onWin(winner: Int)         // 1黑 2白
        fun onModeInfo(text: String)
        fun onMovePlaced(r: Int, c: Int, player: Int, seq: Int) // 落子回调
        fun onUndo(r: Int, c: Int, player: Int, newSeq: Int)    // 悔棋回调
        fun onReset()                                              // 重开回调
    }
    var listener: Listener? = null

    // 模式
    enum class Mode {
        LOCAL_TWO_PLAYERS,      // 本地双人
        HUMAN_FIRST_AI,         // 人机：人先手（黑）
        AI_FIRST_HUMAN          // 人机：AI先手（黑）
    }
    var mode: Mode = Mode.LOCAL_TWO_PLAYERS
        set(value) {
            field = value
            reset()
            when (value) {
                Mode.LOCAL_TWO_PLAYERS -> {
                    listener?.onModeInfo("模式：本地双人")
                }
                Mode.HUMAN_FIRST_AI -> {
                    listener?.onModeInfo("模式：人机（你先手执黑）")
                }
                Mode.AI_FIRST_HUMAN -> {
                    listener?.onModeInfo("模式：人机（AI先手执黑）")
                    // AI 先手，直接走一步
                    post { aiMoveIfNeeded() }
                }
            }
        }

    // 棋盘与状态
    private val boardSize = 15
    private val grid = Array(boardSize) { IntArray(boardSize) } // 0空 1黑 2白
    private val moves = mutableListOf<Pair<Int, Int>>() // 落子记录
    private var currentPlayer = 1 // 1黑 2白
    private var gameOver = false

    // 绘制
    private var cellSize = 0f
    private var boardPadding = 32f

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444")
        strokeWidth = 2f
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444")
        style = Paint.Style.FILL
    }
    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        setShadowLayer(6f, 0f, 2f, Color.parseColor("#55000000"))
    }
    private val whiteEdgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#333333")
        style = Paint.Style.STROKE
    }
    private val lastMovePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 3f
    }
    // 复盘模式：禁止触摸
    private var replayLock = false
    fun setReplayLock(lock: Boolean) { replayLock = lock }

    //尺寸
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val size = min(w, h).toFloat()
        cellSize = (size - boardPadding * 2) / (boardSize - 1)
        whiteEdgePaint.strokeWidth = max(2f, cellSize * 0.08f)
    }

    //触摸落子
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (replayLock) return true           // 复盘时禁用
        if (event.action != MotionEvent.ACTION_DOWN) return false
        if (gameOver) return true

        // 人机模式：如果轮到 AI，则禁止触摸
        if (isAITurn()) return true

        val p = screenToGrid(event.x, event.y) ?: return true
        placeIfValid(p.first, p.second, currentPlayer, humanAction = true)
        return true
    }

    private fun isAITurn(): Boolean {
        return when (mode) {
            Mode.LOCAL_TWO_PLAYERS -> false
            Mode.HUMAN_FIRST_AI -> currentPlayer == aiPlayer()
            Mode.AI_FIRST_HUMAN -> currentPlayer == aiPlayer()
        }
    }

    //绘制棋盘
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val start = boardPadding
        val end = boardPadding + cellSize * (boardSize - 1)

        // 网格
        for (i in 0 until boardSize) {
            val pos = start + i * cellSize
            canvas.drawLine(start, pos, end, pos, linePaint)
            canvas.drawLine(pos, start, pos, end, linePaint)
        }

        // 星位（3-3、3-11、11-3、11-11、7-7）
        val stars = arrayOf(
            3 to 3, 3 to 11, 11 to 3, 11 to 11, 7 to 7
        )
        stars.forEach { (r, c) ->
            val (x, y) = gridToScreen(r, c)
            canvas.drawCircle(x, y, cellSize * 0.12f, starPaint)
        }

        // 棋子
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                when (grid[r][c]) {
                    1 -> { // 黑子
                        val (x, y) = gridToScreen(r, c)
                        canvas.drawCircle(x, y, cellSize * 0.45f, blackPaint)
                    }
                    2 -> { // 白子
                        val (x, y) = gridToScreen(r, c)
                        val radius = cellSize * 0.45f
                        canvas.drawCircle(x, y, radius, whitePaint)       // 先填充白色
                        canvas.drawCircle(x, y, radius - whiteEdgePaint.strokeWidth/2f, whiteEdgePaint) // 再描边
                    }
                }
            }
        }

        // 标记最后一步
        moves.lastOrNull()?.let { (r, c) ->
            val (x, y) = gridToScreen(r, c)
            canvas.drawLine(x - cellSize * 0.2f, y, x + cellSize * 0.2f, y, lastMovePaint)
            canvas.drawLine(x, y - cellSize * 0.2f, x, y + cellSize * 0.2f, lastMovePaint)
        }
    }

    // 坐标转换
    private fun screenToGrid(x: Float, y: Float): Pair<Int, Int>? {
        val start = boardPadding
        val end = boardPadding + cellSize * (boardSize - 1)
        if (x < start - cellSize / 2 || x > end + cellSize / 2 ||
            y < start - cellSize / 2 || y > end + cellSize / 2) return null

        val c = ((x - start) / cellSize).roundToInt().coerceIn(0, boardSize - 1)
        val r = ((y - start) / cellSize).roundToInt().coerceIn(0, boardSize - 1)
        return r to c
    }

    private fun gridToScreen(r: Int, c: Int): Pair<Float, Float> {
        val start = boardPadding
        val x = start + c * cellSize
        val y = start + r * cellSize
        return x to y
    }

    // 核心逻辑
    private fun placeIfValid(r: Int, c: Int, player: Int, humanAction: Boolean) {
        if (r !in 0 until boardSize || c !in 0 until boardSize) return
        if (grid[r][c] != 0) return
        grid[r][c] = player
        moves.add(r to c)
        invalidate()
        // 落子回调（用于记录复盘）
        listener?.onMovePlaced(r, c, player, moves.size - 1)

        if (checkWin(r, c, player)) {
            gameOver = true
            listener?.onWin(player)
            return
        }

        switchTurn()
        listener?.onTurnChanged(currentPlayer)


        // 若是人类下的子，且下一手是 AI，则让 AI 走
        if (humanAction) {
            aiMoveIfNeeded()
        }
    }

    private fun switchTurn() {
        currentPlayer = if (currentPlayer == 1) 2 else 1
    }

    //悔棋 / 重置
    fun undo() {
        // 防抖：没有步就不处理
        if (moves.isEmpty()) return
        gameOver = false


        val idx = moves.lastIndex
        if (idx < 0) return
        val (r, c) = moves.removeAt(idx)

        // 清掉最后一步
        if (r in 0 until boardSize && c in 0 until boardSize) {
            grid[r][c] = 0
        }

        // 轮次回退到上一步的玩家
        switchTurn()

        // 通知 & 重绘
        listener?.onTurnChanged(currentPlayer)
        invalidate()
        // 悔棋回调
        val removedPlayer = currentPlayer      // 此时 currentPlayer 等于被撤回的那一步的玩家
        listener?.onUndo(r, c, removedPlayer, moves.size)
    }


    fun reset() {
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) grid[r][c] = 0
        }
        moves.clear()
        currentPlayer = 1
        gameOver = false
        listener?.onTurnChanged(currentPlayer)
        invalidate()
    }

    // 胜负判断（五连）
    private val dirs = arrayOf(
        intArrayOf(1, 0),  // 竖
        intArrayOf(0, 1),  // 横
        intArrayOf(1, 1),  // 撇
        intArrayOf(1, -1)  // 捺
    )

    private fun checkWin(r: Int, c: Int, player: Int): Boolean {
        for (d in dirs) {
            var count = 1
            count += countDir(r, c, d[0], d[1], player)
            count += countDir(r, c, -d[0], -d[1], player)
            if (count >= 5) return true
        }
        return false
    }

    private fun countDir(r: Int, c: Int, dr: Int, dc: Int, player: Int): Int {
        var i = 1
        var cnt = 0
        while (true) {
            val nr = r + dr * i
            val nc = c + dc * i
            if (nr !in 0 until boardSize || nc !in 0 until boardSize) break
            if (grid[nr][nc] == player) {
                cnt++
                i++
            } else break
        }
        return cnt
    }

    // AI 相关
    private fun aiPlayer(): Int =  if (mode == Mode.AI_FIRST_HUMAN || mode == Mode.HUMAN_FIRST_AI) 2 else 0
    private fun humanPlayer(): Int = if (aiPlayer() == 2) 1 else 2

    private fun aiMoveIfNeeded() {
        if (gameOver) return
        if (!isAITurn()) return
        // 延迟，自然
        postDelayed({
            val p = chooseBestMove(aiPlayer(), humanPlayer())
            if (p != null) {
                placeIfValid(p.first, p.second, aiPlayer(), humanAction = false)
            }
        }, 120)
    }

    private fun chooseBestMove(ai: Int, human: Int): Pair<Int, Int>? {
        var best: Pair<Int, Int>? = null
        var bestScore = Int.MIN_VALUE

        val candidates = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until boardSize) {
            for (c in 0 until boardSize) {
                if (grid[r][c] != 0) continue
                if (hasNeighbor(r, c, 2)) { // 半径2有子
                    candidates.add(r to c)
                }
            }
        }

        if (candidates.isEmpty()) {
            val mid = boardSize / 2
            return mid to mid
        }


        for ((r, c) in candidates) {
            grid[r][c] = ai
            val win = checkWin(r, c, ai)
            grid[r][c] = 0
            if (win) return r to c
        }


        for ((r, c) in candidates) {
            grid[r][c] = human
            val humanWin = checkWin(r, c, human)
            grid[r][c] = 0
            if (humanWin) return r to c
        }


        for ((r, c) in candidates) {
            val s = heuristicScore(r, c, ai, human)
            if (s > bestScore) {
                bestScore = s
                best = r to c
            }
        }
        return best ?: candidates.random()
    }

    private fun hasNeighbor(r: Int, c: Int, radius: Int): Boolean {
        for (dr in -radius..radius) {
            for (dc in -radius..radius) {
                if (dr == 0 && dc == 0) continue
                val nr = r + dr
                val nc = c + dc
                if (nr in 0 until boardSize && nc in 0 until boardSize) {
                    if (grid[nr][nc] != 0) return true
                }
            }
        }
        return false
    }

    private fun heuristicScore(r: Int, c: Int, ai: Int, human: Int): Int {
        var score = 0

        // 中心偏好
        val mid = boardSize / 2
        val dist = abs(r - mid) + abs(c - mid)
        score += (100 - dist * 6)

        // 连子潜力
        dirs.forEach { d ->
            score += linePotential(r, c, d[0], d[1], ai) * 10
            score += linePotential(r, c, d[0], d[1], human) * 7
        }
        return score
    }


    private fun linePotential(r: Int, c: Int, dr: Int, dc: Int, player: Int): Int {
        // 假设把子落在 (r,c)
        var left = 0
        var rr = r - dr
        var cc = c - dc
        while (rr in 0 until boardSize && cc in 0 until boardSize && grid[rr][cc] == player) {
            left++; rr -= dr; cc -= dc
        }
        val leftOpen = (rr in 0 until boardSize && cc in 0 until boardSize && grid[rr][cc] == 0)

        var right = 0
        rr = r + dr
        cc = c + dc
        while (rr in 0 until boardSize && cc in 0 until boardSize && grid[rr][cc] == player) {
            right++; rr += dr; cc += dc
        }
        val rightOpen = (rr in 0 until boardSize && cc in 0 until boardSize && grid[rr][cc] == 0)

        val len = left + right + 1
        var base = when (len) {
            5 -> 100000
            4 -> 5000
            3 -> 500
            2 -> 80
            else -> 10
        }
        // 开放度加成
        val open = (if (leftOpen) 1 else 0) + (if (rightOpen) 1 else 0)
        base *= when (open) {
            2 -> 2
            1 -> 1
            else -> 0
        }
        return base
    }
    //复盘：外部渲染

    fun moveCount(): Int = moves.size


    fun renderMovesForReplay(m: List<Triple<Int, Int, Int>>, upTo: Int) {
        for (r in 0 until boardSize) for (c in 0 until boardSize) grid[r][c] = 0
        moves.clear()
        val n = min(upTo, m.size)
        for (i in 0 until n) {
            val (r, c, player) = m[i]
            grid[r][c] = player
            moves.add(r to c)
        }
        currentPlayer = if (n % 2 == 0) 1 else 2
        gameOver = false
        invalidate()
    }
}
