package com.example.gr8math.utils

import android.util.Log
import com.example.gr8math.api.ConnectURL
import com.example.gr8math.dataObject.CurrentCourse
import com.example.gr8math.adapter.TeacherNotificationResponse
import com.google.android.material.bottomnavigation.BottomNavigationView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object NotificationHelper {

    var unreadCount: Int = 0

    fun fetchUnreadCount(bottomNav: BottomNavigationView? = null) {
        ConnectURL.api.getTeacherNotifications(CurrentCourse.userId, CurrentCourse.courseId)
            .enqueue(object : Callback<TeacherNotificationResponse> {
                override fun onResponse(
                    call: Call<TeacherNotificationResponse>,
                    response: Response<TeacherNotificationResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        unreadCount = response.body()!!.notifications.count { !it.is_read }

                        // Update badge if bottomNav is passed
                        bottomNav?.let { nav ->
                            Notifs.updateNotificationBadge(nav)
                        }
                    }
                }

                override fun onFailure(call: Call<TeacherNotificationResponse>, t: Throwable) {
                    Log.e("NotifHelper", "Failed to fetch notifications: ${t.localizedMessage}")
                }
            })
    }
}
