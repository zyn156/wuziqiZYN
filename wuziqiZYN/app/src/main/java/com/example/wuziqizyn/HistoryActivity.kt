package com.example.wuziqizyn

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wuziqizyn.data.AppDatabase
import com.example.wuziqizyn.data.GameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HistoryActivity : AppCompatActivity() {

    private val db by lazy { AppDatabase.get(this) }
    private val gameDao by lazy { db.gameDao() }
    private lateinit var listView: ListView
    private val items = mutableListOf<Pair<GameEntity, String>>() // entity + 显示文本
    private val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        listView = findViewById(R.id.listView)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())
        listView.adapter = adapter

        lifecycleScope.launch(Dispatchers.IO) {
            val all = gameDao.getAllGames()
            val mapped = all.map {
                val who = if (it.result == "BLACK_WIN") "黑胜" else "白胜"
                val time = sdf.format(Date(it.startedAt))
                it to "[$time] $who  步数:${it.steps}  模式:${it.modeLabel}"
            }
            withContext(Dispatchers.Main) {
                items.clear(); items.addAll(mapped)
                adapter.clear(); adapter.addAll(mapped.map { it.second }); adapter.notifyDataSetChanged()
            }
        }

        listView.setOnItemClickListener { _, _, pos, _ ->
            val gameId = items[pos].first.id
            startActivity(Intent(this, ReplayActivity::class.java).putExtra("gameId", gameId))
        }
    }
}
