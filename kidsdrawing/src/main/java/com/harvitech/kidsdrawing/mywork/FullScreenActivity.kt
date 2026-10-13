package com.harvitech.kidsdrawing.mywork

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.harvitech.kidsdrawing.R
import com.harvitech.kidsdrawing.common.AdManager
import com.harvitech.kidsdrawing.common.CommonConstants
import com.harvitech.kidsdrawing.databinding.KdActivityFullScreenBinding
import com.harvitech.kidsdrawing.databinding.KdActivityMainBinding
import com.harvitech.kidsdrawing.utils.FilePath
import com.harvitech.kidsdrawing.utils.Utils
import java.io.File
import java.lang.System.currentTimeMillis

class FullScreenActivity : AppCompatActivity() {


    lateinit var context: Context
    private lateinit var binding: KdActivityFullScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = KdActivityFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        context = this
        initDefine()
        AdManager.loadBanner(this, binding.adViewContainer)

    }

    var localImage=""
    private fun initDefine() {
        var intent = intent

        localImage = intent.getStringExtra(CommonConstants.LOCAL_IMAGE)!!
        Glide.with(context).load(localImage).into(binding.ivSharePreview)

        Log.e("TAG", "initDefine::::Local iMagename :::  $localImage")

        binding.ivBack.setOnClickListener {
            finish()
        }


        binding.ivShare.setOnClickListener {
            val file = File(localImage.replace("file://", ""))
            val shareUri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
            val share = Intent()
            share.action = Intent.ACTION_SEND
            share.type = "image/*"
            val link = "https://play.google.com/store/apps/details?id=" + context.packageName
            share.putExtra(Intent.EXTRA_TEXT, link)
            share.putExtra(Intent.EXTRA_TITLE, link)
            share.putExtra(Intent.EXTRA_STREAM, shareUri)
            startActivity(share)
        }

        /*ivDelete.setOnClickListener {
            val builder1 = AlertDialog.Builder(context)
            builder1.setMessage("Are You Sure To Delete This Image ?")
            builder1.setCancelable(true)
            builder1.setPositiveButton(
                "Yes"
            ) { dialog, id ->
                val file = File(localImage.replace("file://", ""))
                if (file.exists()) {
                    if (file.delete()) {
                        dialog.cancel()
                        finish()
                    }
                }

            }
            builder1.setNegativeButton(
                "No"
            ) { dialog, id -> dialog.cancel() }

            val alert11 = builder1.create()
            alert11.show()

        }*/

        /*ivDownload.setOnClickListener {
            var saveDir: File? = null
            val fileName = currentTimeMillis().toString() + ".jpg"
            if (android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED) {
                saveDir = File(
                    (android.os.Environment.getExternalStorageDirectory()).toString() +
                            File.separator + context.resources.getString(R.string.kd_app_name)
                )
                if (!saveDir.exists()) {
                    saveDir.mkdirs()
                }
            }
            FilePath.copyFile(localImage.replace("file://", ""), saveDir.toString() + File.separator + fileName)

            val file = File(saveDir.toString() + File.separator + fileName)
            addImageGallery(file)
            Toast.makeText(context,"Download successfully to gallery",Toast.LENGTH_SHORT).show()
//            Utils.onClickDownload(context, this)
        }*/

    }



    private fun addImageGallery(file: File) {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.DATA, file.absolutePath)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }
}
