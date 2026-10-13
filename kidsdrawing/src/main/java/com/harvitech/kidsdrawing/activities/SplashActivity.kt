package com.harvitech.kidsdrawing.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.harvitech.kidsdrawing.R
import com.harvitech.kidsdrawing.common.AdManager
import com.harvitech.kidsdrawing.interfaces.CallbackListener
import com.harvitech.kidsdrawing.utils.Utils

class SplashActivity : AppCompatActivity(), CallbackListener {


    private var isLoaded = false
    private val handler = Handler()
    private val myRunnable = Runnable {
        if (Utils.isNetworkConnected(this@SplashActivity)) {
            if (!isLoaded) {
                startNextActivity(0)
            }
        }
    }

    private fun startNextActivity(time: Long) {
        Handler().postDelayed({
            val mainIntent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(mainIntent)
            finish()
        }, time)

    }

    fun callApi() {
        if (Utils.isNetworkConnected(this)) {
            successCall()
        } else {
            Utils.openInternetDialog(this, this, true)
        }
        handler.postDelayed(myRunnable, 10000)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.kd_activity_splash)
//        supportActionBar!!.hide()
        AdManager.loadBanner(this, findViewById<FrameLayout>(R.id.adViewContainer))
        callApi()

    }

    override fun onSuccess() {
    }

    override fun onCancel() {
    }

    override fun onRetry() {
        callApi()
    }


    private fun successCall() {
            startNextActivity(4000)

    }



    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(myRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(myRunnable)
    }
}