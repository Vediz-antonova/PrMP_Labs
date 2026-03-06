package com.vedizL.mobilelabs.ui.history

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.vedizL.mobilelabs.R
import com.vedizL.mobilelabs.data.history.ActionHistoryStore
import com.vedizL.mobilelabs.data.history.ActionEvent

class HistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val rv = findViewById<RecyclerView>(R.id.rvHistory)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        rv.layoutManager = LinearLayoutManager(this)

        val items = ActionHistoryStore.load(this)
        val adapter = HistoryAdapter(items) { event: ActionEvent ->
            val value = event.details.substringAfterLast("=")
            val result = if (value.isNotEmpty()) value else ""
            if (result.isNotEmpty()) {
                val data = Intent()
                data.putExtra("selected_value", result)
                setResult(Activity.RESULT_OK, data)
                finish()
            }
        }
        rv.adapter = adapter

        // If cloud history exists, load and replace local
        ActionHistoryStore.loadCloud(this) { cloudItems ->
            if (cloudItems.isNotEmpty()) {
                runOnUiThread {
                    adapter.submitList(cloudItems)
                    if (cloudItems.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rv.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rv.visibility = View.VISIBLE
                    }
                }
            }
        }

        if (items.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rv.visibility = View.VISIBLE
        }
    }
}
