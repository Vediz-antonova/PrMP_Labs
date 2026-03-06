package com.vedizL.mobilelabs.data.history

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ActionHistoryStore {
    private const val PREFS_NAME = "mobilelabs_prefs"
    private const val KEY_HISTORY = "action_history"
    private const val MAX_ITEMS = 200

    fun log(context: Context, event: ActionEvent) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = try {
            val s = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
            JSONArray(s)
        } catch (e: Exception) {
            JSONArray()
        }
        val obj = JSONObject()
        obj.put("timestamp", event.timestamp)
        obj.put("type", event.type)
        obj.put("details", event.details)
        jsonArray.put(obj)
        // prune to keep history bounded
        val trimmed = if (jsonArray.length() > MAX_ITEMS) {
            val start = jsonArray.length() - MAX_ITEMS
            val newArr = JSONArray()
            for (i in start until jsonArray.length()) {
                newArr.put(jsonArray.get(i))
            }
            newArr
        } else jsonArray
        prefs.edit().putString(KEY_HISTORY, trimmed.toString()).apply()
    }

    fun load(context: Context): List<ActionEvent> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = try {
            JSONArray(prefs.getString(KEY_HISTORY, "[]") ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
        val list = mutableListOf<ActionEvent>()
        for (i in 0 until jsonArray.length()) {
            val o = jsonArray.optJSONObject(i)
            val ts = o?.optLong("timestamp", 0) ?: 0L
            val type = o?.optString("type") ?: ""
            val details = o?.optString("details") ?: ""
            list.add(ActionEvent(ts, type, details))
        }
        return list.sortedBy { it.timestamp }
    }
}
