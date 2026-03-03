package com.vedizL.mobilelabs.data.theme

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ThemeRepository {
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

    suspend fun saveTheme(data: Map<String, Any>) {
        val finalData = data.toMutableMap()
        finalData["updated_at"] = Timestamp.now()
        finalData["version"] = 1

        db.collection("users")
            .document(userId)
            .collection("theme")
            .document("settings")
            .set(finalData)
            .await()
    }
}