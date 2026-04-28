package com.example.gr8math.Utils

import android.util.Log
import com.example.gr8math.Data.Repository.NotificationRepository
import com.example.gr8math.Model.CurrentCourse
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NotificationHelper {

    var unreadCount: Int = 0
    private val repository = NotificationRepository()

    fun fetchUnreadCount(bottomNav: BottomNavigationView? = null) {
        val userId = CurrentCourse.userId
        val courseId = CurrentCourse.courseId
        val role = CurrentCourse.currentRole

        if (userId == 0 || courseId == 0) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val unread = if (role.equals("teacher", ignoreCase = true)) {
                    val result = repository.getTeacherNotifications(userId, courseId)
                    result.getOrNull()?.count { !it.isRead } ?: 0
                } else {
                    val result = repository.getStudentNotifications(userId, courseId)
                    result.getOrNull()?.count { !it.isRead } ?: 0
                }

                unreadCount = unread

                withContext(Dispatchers.Main) {
                    bottomNav?.let { nav ->
                        Notifs.updateNotificationBadge(nav)
                    }
                }
            } catch (e: Exception) {
//                Log.e("NotifHelper", "Error: ${e.message}")
            }
        }
    }
}