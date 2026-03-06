package com.vedizL.mobilelabs.data.history

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject

object ActionHistoryStore {
    private const val PREFS_NAME = "mobilelabs_prefs"
    private const val KEY_HISTORY = "action_history"
    private const val MAX_ITEMS = 200
    // simple counter to help generate unique doc IDs per timestamp
    private var cloudDocCounter: Long = 0L
    private fun generateHistoryDocId(timestamp: Long): String {
        val datePart = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date(timestamp))
        val suffix = cloudDocCounter++
        return if (suffix >= 0) "${datePart}_${suffix}" else "${datePart}_${-suffix}"
    }

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

        // Also push to cloud (use anonymous uid if not signed in)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        val cloudRef = FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("history")
        val payload = hashMapOf(
            "timestamp" to event.timestamp,
            "type" to event.type,
            "details" to event.details
        )
        val docId = generateHistoryDocId(event.timestamp)
        cloudRef.document(docId).set(payload)
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

    // Load cloud history (latest 20) for the current user
    fun loadCloud(context: Context, callback: (List<ActionEvent>) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
        // Cloud may be empty if user hasn't used history yet
        val query = FirebaseFirestore.getInstance()
            .collection("users").document(uid).collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
        query.get()
            .addOnSuccessListener { docs ->
                val list = mutableListOf<ActionEvent>()
                for (d in docs) {
                    val ts = d.getLong("timestamp") ?: 0L
                    val type = d.getString("type") ?: ""
                    val details = d.getString("details") ?: ""
                    list.add(ActionEvent(ts, type, details))
                }
                callback(list)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
}
