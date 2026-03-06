package com.vedizL.mobilelabs.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vedizL.mobilelabs.R
import com.vedizL.mobilelabs.data.history.ActionEvent
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(private var items: List<ActionEvent>, private val onClick: ((ActionEvent) -> Unit)? = null) : RecyclerView.Adapter<HistoryAdapter.Holder>() {
    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvTime)
        val tvDetails: TextView = view.findViewById(R.id.tvDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.tvTime.text = sdf.format(item.timestamp)
        holder.tvDetails.text = item.details
        holder.itemView.setOnClickListener { onClick?.invoke(item) }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<ActionEvent>) {
        items = list
        notifyDataSetChanged()
    }
}
