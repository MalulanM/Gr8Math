package com.example.gr8math.Utils

import android.util.Log
import com.example.gr8math.Model.CurrentCourse
import com.example.gr8math.R
import com.example.gr8math.Services.SupabaseService
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object NotificationHelper {

    var unreadCount: Int = 0

    fun fetchUnreadCount(bottomNav: BottomNavigationView? = null) {
        val userId = CurrentCourse.userId
        if (userId == 0) return

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val count = SupabaseService.client
                    .from("notifications")
                    .select(columns = Columns.list("id")) {
                        filter {
                            eq("user_id", userId)
                            eq("is_read", false)
                        }
                        count(Count.EXACT)
                    }.countOrNull() ?: 0

                unreadCount = count.toInt()

                withContext(Dispatchers.Main) {
                    bottomNav?.let { nav ->
                        Notifs.updateNotificationBadge(nav)
                    }
                }
            } catch (e: Exception) {
                Log.e("NotifHelper", "Error: ${e.message}")
            }
        }
    }
}