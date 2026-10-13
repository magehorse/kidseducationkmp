package com.kidsplay.kidseducation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.harvitech.kidsdrawing.activities.BGImageListActivity
import com.kidsplay.kidseducation.ads.AndroidAdService
import com.kidsplay.kidseducation.platform.AndroidLinkOpener
import com.kidsplay.kidseducation.platform.AndroidSpeaker
import com.kidsplay.kidseducation.platform.PlatformDeps
import com.russhwolf.settings.SharedPreferencesSettings

class MainActivity : ComponentActivity() {
    private lateinit var speaker: AndroidSpeaker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        speaker = AndroidSpeaker(applicationContext)
        val deps = PlatformDeps(
            speaker = speaker,
            // Same SharedPreferences files the old app used, so updating keeps name, language, sound and progress.
            settings = SharedPreferencesSettings(getSharedPreferences("${packageName}_preferences", MODE_PRIVATE)),
            soundSettings = SharedPreferencesSettings(getSharedPreferences("MyPref", MODE_PRIVATE)),
            lifeSkillsSettings = SharedPreferencesSettings(getSharedPreferences("life_skills_progress", MODE_PRIVATE)),
            links = AndroidLinkOpener(applicationContext),
            ads = AndroidAdService(this),
            openDrawing = { startActivity(Intent(this, BGImageListActivity::class.java)) },
        )
        setContent { App(deps) }
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }
}
