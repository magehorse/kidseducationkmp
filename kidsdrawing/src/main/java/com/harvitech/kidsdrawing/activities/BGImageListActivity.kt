package com.harvitech.kidsdrawing.activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.harvitech.kidsdrawing.R
import com.harvitech.kidsdrawing.adapters.BGImageAdapter
import com.harvitech.kidsdrawing.common.AdManager
import com.harvitech.kidsdrawing.common.CommonConstants
import com.harvitech.kidsdrawing.common.CommonUtilities
import com.harvitech.kidsdrawing.custom.CustomProgressDialog
import com.harvitech.kidsdrawing.databinding.ActivityBgImageListBinding
import com.harvitech.kidsdrawing.interfaces.AdapterItemCallback
import com.harvitech.kidsdrawing.pojo.BGImageClass
import com.harvitech.kidsdrawing.utils.Utils
import java.util.*

class BGImageListActivity : AppCompatActivity(), AdapterItemCallback, View.OnClickListener {


    private var levelClassArrayList = ArrayList<BGImageClass>()
    private var isEasy = true

    private lateinit var binding: ActivityBgImageListBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() per the enableEdgeToEdge()
        // contract - replaces the deprecated setDecorFitsSystemWindows(false)
        // + statusBarColor combination the Play Console flagged.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        binding = ActivityBgImageListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupHeaderColor()
        initViews()
        loadDataFromIntent()
        AdManager.loadBanner(this, binding.adViewContainer)


    }

    override fun onResume() {
        super.onResume()
    }


    /**
     * Modern Android (targeting SDK 35+) forces edge-to-edge and ignores
     * setDecorFitsSystemWindows(true)/statusBarColor - the header would
     * silently sit behind the status bar and get its back button/title
     * covered no matter what we do here. So instead of fighting that, we
     * lean into it correctly: let the header's own blue background extend
     * up behind the status bar, but push its *content* down by exactly the
     * status bar's height so nothing is ever covered - on every Android
     * version, old or new.
     */
    private fun setupHeaderColor() {
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        ViewCompat.setOnApplyWindowInsetsListener(binding.headerRoot) { view, insets ->
            val statusBarInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(view.paddingLeft, statusBarInset, view.paddingRight, view.paddingBottom)
            insets
        }
    }

    private fun initViews() {
        levelClassArrayList = ArrayList()
        binding.rvBGImageList.layoutManager = GridLayoutManager(
            this,
            2,
            RecyclerView.VERTICAL,
            false
        )
        binding.ivBack.setOnClickListener(this)
        binding.tvTabEasy.setOnClickListener(this)
        binding.tvTabHard.setOnClickListener(this)
    }

    /**
     * This screen is now the direct "select an image to draw" entry point
     * from Home (no splash / no Easy-Hard chooser dialog in between), so it
     * no longer requires an intent extra - it defaults to Easy and lets the
     * child switch to Hard with the tabs at the top.
     */
    private fun loadDataFromIntent() {
        isEasy = intent?.getBooleanExtra(CommonConstants.KeyBGImageType, true) ?: true
        selectTab(isEasy)
    }

    private fun selectTab(easy: Boolean) {
        isEasy = easy
        binding.tvTabEasy.setBackgroundResource(if (easy) R.drawable.bg_bg_image_tab_selected else R.drawable.bg_bg_image_tab_unselected)
        binding.tvTabEasy.setTextColor(ContextCompat.getColor(this, if (easy) R.color.kd_colorWhite else R.color.kd_matchHeaderTheme))
        binding.tvTabHard.setBackgroundResource(if (easy) R.drawable.bg_bg_image_tab_unselected else R.drawable.bg_bg_image_tab_selected)
        binding.tvTabHard.setTextColor(ContextCompat.getColor(this, if (easy) R.color.kd_matchHeaderTheme else R.color.kd_colorWhite))
        LoadImageListAsync(this).execute()
    }

    override fun onClick(v: View?) {
        val id = v!!.id
        if (id == R.id.ivBack) {
            finish()
        } else if (id == R.id.tvTabEasy) {
            if (!isEasy) selectTab(true)
        } else if (id == R.id.tvTabHard) {
            if (isEasy) selectTab(false)
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class LoadImageListAsync(private val context: Context) : AsyncTask<Void, Void, Void>() {

        lateinit var pDialog: ProgressDialog

        override fun onPreExecute() {
            super.onPreExecute()
            pDialog = CustomProgressDialog.showProgress(context)
        }

        override fun doInBackground(vararg params: Void?): Void? {
            if (isEasy) {
                levelClassArrayList = CommonUtilities.getSmallEasyBGImageList(context)
            } else {
                levelClassArrayList = CommonUtilities.getSmallHardBGImageList(context)
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            CustomProgressDialog.hideProgress(pDialog)
            binding.rvBGImageList.adapter = BGImageAdapter(context, levelClassArrayList, this@BGImageListActivity)
        }
    }

    var position = 0
    override fun onAdapterItemClick(mPos: Int) {

        position = mPos
        startEditorActivity()



    }


    private fun startEditorActivity(){
        CommonConstants.IS_CLEAR = true
        CommonConstants.IS_CLEAR_BRUSH = true
        CommonConstants.IS_CLEAR_MAGIC = true
        CommonConstants.IS_CLEAR_PENCIL = true
        CommonConstants.IS_CLEAR_STICKER = true
        CommonConstants.IS_CLEAR_TEXT = true
        CommonConstants.STICKER_COUNT = 0


        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra(CommonConstants.KeyBGImageType, isEasy)
        intent.putExtra(CommonConstants.KeyItemPos, position)
        startActivity(intent)
    }



}