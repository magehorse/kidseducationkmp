package com.kidsplay.kidseducation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.harvitech.kidsdrawing.activities.BGImageListActivity
import com.harvitech.kidsdrawing.activities.EditorActivity
import com.harvitech.kidsdrawing.common.CommonConstants
import com.harvitech.kidsdrawing.mywork.MyWorkActivity
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
            openMyDrawings = { startActivity(Intent(this, MyWorkActivity::class.java)) },
            openDrawing = { easy, index ->
                if (index == null) {
                    startActivity(Intent(this, BGImageListActivity::class.java).putExtra(CommonConstants.KeyBGImageType, easy))
                } else {
                    // Same reset the module's own list screen does before opening the editor.
                    CommonConstants.IS_CLEAR = true
                    CommonConstants.IS_CLEAR_BRUSH = true
                    CommonConstants.IS_CLEAR_MAGIC = true
                    CommonConstants.IS_CLEAR_PENCIL = true
                    CommonConstants.IS_CLEAR_STICKER = true
                    CommonConstants.IS_CLEAR_TEXT = true
                    CommonConstants.STICKER_COUNT = 0
                    startActivity(
                        Intent(this, EditorActivity::class.java)
                            .putExtra(CommonConstants.KeyBGImageType, easy)
                            .putExtra(CommonConstants.KeyItemPos, index),
                    )
                }
            },
        )
        setContent { App(deps) }
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }
}
