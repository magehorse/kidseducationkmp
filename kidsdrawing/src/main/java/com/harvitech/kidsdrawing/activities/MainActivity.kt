package com.harvitech.kidsdrawing.activities

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import com.harvitech.kidsdrawing.R
import com.harvitech.kidsdrawing.common.AdManager
import com.harvitech.kidsdrawing.common.CommonConstants
import com.harvitech.kidsdrawing.databinding.KdActivityMainBinding
import com.harvitech.kidsdrawing.interfaces.CallbackListener
import com.harvitech.kidsdrawing.mywork.MyWorkActivity
import com.harvitech.kidsdrawing.utils.Utils


class MainActivity : AppCompatActivity(), View.OnClickListener, CallbackListener {
    private lateinit var binding: KdActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = KdActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        AdManager.loadBanner(this, binding.adViewContainer)

        try {
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }


    }



    private fun initViews() {
        binding.ivLetsPlay.setOnClickListener(this)
        binding.ivMyCreation.setOnClickListener(this)
        binding.ivShare.setOnClickListener(this)
        binding.ivRate.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        val id = v!!.id
        if (id == R.id.ivLetsPlay) {
            showEasyHardDialog()
        } else if (id == R.id.ivMyCreation) {
            if (Utils.checkPermission(this)) {
                startActivity(Intent(this, MyWorkActivity::class.java))
            }
        } else if (id == R.id.ivShare) {
            shareApp()
        } else if (id == R.id.ivRate) {
            rateUs()
        }
    }

    private fun showEasyHardDialog() {
        val dialog = Dialog(this)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT));
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val customView = LayoutInflater.from(this).inflate(R.layout.dialog_fragment_level, null)
        dialog.setContentView(customView)
        val ivClose = customView.findViewById(R.id.ivClose) as AppCompatImageView
        ivClose.setOnClickListener {
            dialog.dismiss()
        }
        val ivEasy = customView.findViewById(R.id.ivEasy) as AppCompatImageView
        ivEasy.setOnClickListener {
            dialog.dismiss()
            startBGImageListActivity(true)
        }
        val ivHard = customView.findViewById(R.id.ivHard) as AppCompatImageView
        ivHard.setOnClickListener {
            dialog.dismiss()
            startBGImageListActivity(false)
        }

        dialog.show()
    }

    private fun startBGImageListActivity(isEasy: Boolean) {
        val intent = Intent(this, BGImageListActivity::class.java)
        intent.putExtra(CommonConstants.KeyBGImageType, isEasy)
        startActivity(intent)
    }


    private fun rateUs() {
        val appPackageName = packageName
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store")))
        } catch (e: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }


    private fun shareApp() {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        val link = "https://play.google.com/store/apps/details?id=$packageName"
        shareIntent.putExtra(Intent.EXTRA_TEXT, link)
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, resources.getString(R.string.kd_app_name))
        shareIntent.type = "text/plain"
        startActivity(Intent.createChooser(shareIntent, resources.getString(R.string.kd_app_name)))
    }

    override fun onSuccess() {

    }

    override fun onCancel() {

    }

    override fun onRetry() {

    }
}