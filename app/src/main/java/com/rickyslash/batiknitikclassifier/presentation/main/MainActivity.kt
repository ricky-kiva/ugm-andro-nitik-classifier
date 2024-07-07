package com.rickyslash.batiknitikclassifier.presentation.main

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.rickyslash.batiknitikclassifier.R
import com.rickyslash.batiknitikclassifier.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy(LazyThreadSafetyMode.NONE) {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupView()
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
        binding.toolbar.setNavigationIcon(R.drawable.ic_canting_red_24)
    }

    private fun showTipsDialog() {
        val tipsMessage = getString(R.string.dialog_message_tips)

        val tipsDialog = AlertDialog.Builder(this, R.style.RedAlertDialog)
            .setTitle(getString(R.string.dialog_title_tips))
            .setMessage(Html.fromHtml(tipsMessage, Html.FROM_HTML_MODE_COMPACT))
            .setPositiveButton(getString(R.string.label_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        tipsDialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                Toast.makeText(this@MainActivity, getString(R.string.easter_dev_web), Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_opt_help -> {
                showTipsDialog()
                true
            }
            else -> true
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q)
            finishAfterTransition() // avoid memory leak in Android Q
        else super.onBackPressed()
    }
}