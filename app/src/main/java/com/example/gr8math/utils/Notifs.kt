package com.example.gr8math.utils

import android.graphics.Color
import com.example.gr8math.R
import com.google.android.material.bottomnavigation.BottomNavigationView

object Notifs {

    fun updateNotificationBadge(bottomNav: BottomNavigationView) {
        val badge = bottomNav.getOrCreateBadge(R.id.nav_notifications)

        if (NotificationHelper.unreadCount > 0) {
            badge.isVisible = true
            badge.backgroundColor = Color.RED
        } else {
            badge.isVisible = false
        }
    }
}
