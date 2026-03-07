package com.vedizL.mobilelabs.data.theme

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.vedizL.mobilelabs.data.auth.AuthManager

class ThemeRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveTheme(data: Map<String, Any>) {
        if (AuthManager.isAnonymous()) {
            return
        }

        val userId = AuthManager.getUserId()
        val finalData = data.toMutableMap()
        finalData["updated_at"] = Timestamp.now()
        finalData["version"] = 1
        finalData["email"] = AuthManager.getUserEmail()

        db.collection("users")
            .document(userId)
            .collection("theme")
            .document("settings")
            .set(finalData)
            .await()
    }
}