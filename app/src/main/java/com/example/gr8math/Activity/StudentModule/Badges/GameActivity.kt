package com.example.gr8math.Activity

import android.os.Bundle
import android.util.Log
import com.unity3d.player.UnityPlayerGameActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.gr8math.Data.Repository.BadgeRepository

class GameActivity : UnityPlayerGameActivity() {

    private var currentStudentId: Int = -1
    private val badgeRepository = BadgeRepository()

    // NEW: Track how many things we are currently saving to the database
    private var activeDatabaseSaves = 0
    // NEW: Keep track of whether Unity has asked to close yet
    private var unityWantsToQuit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentStudentId = intent.getIntExtra("student_id", -1)
    }


    fun recordBadgeEarned(badgeId: Int) {
        if (currentStudentId == -1) return
        activeDatabaseSaves++

        lifecycleScope.launch(Dispatchers.IO) {
            val badgeName = badgeRepository.awardGameBadgeFromUnity(currentStudentId, badgeId)

            if (badgeName != null) {
                val file = java.io.File("/data/data/com.example.gr8math/files/pending_badges.txt")
                val existing = if (file.exists()) file.readText() else ""
                val updated = if (existing.isEmpty()) badgeName else "$existing\n$badgeName"
                file.writeText(updated)
                Log.d("GameActivity", "File written to: ${file.absolutePath} — $updated")
            }

            launch(Dispatchers.Main) {
                activeDatabaseSaves--
                checkIfReadyToQuit()
            }
        }
    }

    // Unity calls this to exit the game
    fun quitGame() {
        unityWantsToQuit = true // Unity is done, but Android might still be saving!
        checkIfReadyToQuit()
    }

    private fun checkIfReadyToQuit() {
        // ONLY close the game if Unity is done AND the database has finished saving
        if (unityWantsToQuit && activeDatabaseSaves == 0) {
            runOnUiThread {
                finish()
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }
}