package com.example.datagov.data

import android.content.Context
import android.content.SharedPreferences

class ProjectPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("project_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_NOTIFIED_PROJECT_ID = "last_notified_project_id"
        private const val KEY_LAST_NOTIFIED_TIMESTAMP = "last_notified_timestamp"
    }

    fun getLastNotifiedProjectId(): String? {
        return prefs.getString(KEY_LAST_NOTIFIED_PROJECT_ID, null)
    }

    fun saveLastNotifiedProject(projectId: String, timestamp: Long) {
        prefs.edit().apply {
            putString(KEY_LAST_NOTIFIED_PROJECT_ID, projectId)
            putLong(KEY_LAST_NOTIFIED_TIMESTAMP, timestamp)
            apply()
        }
    }

    fun getLastNotifiedTimestamp(): Long {
        return prefs.getLong(KEY_LAST_NOTIFIED_TIMESTAMP, 0L)
    }
}

