package com.example.gr8math.Services

import android.app.Application
import android.os.Build
import com.example.gr8math.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

object SupabaseService {

    val client: SupabaseClient by lazy {
        // 1. Detect if we are running inside the Unity process
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            "" // Fallback for very old versions
        }

        val isUnityProcess = processName.endsWith(":Unity")

        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Postgrest)

            // 2. CRITICAL: Only install Auth if we are NOT in the Unity process
            // This stops the crash because the Unity process will never try to touch the disk
            if (!isUnityProcess) {
                install(Auth)
            }

            install(Storage)

            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
}