package com.example.passwordsaver

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class Export : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.show_qr)
        val extras = intent.extras!!
        val sr = extras.getStringArrayList("qr")
        val nextOrDone: Button = findViewById(R.id.next)
        var idx = 0
        val encoder = BarcodeEncoder()
        var bitmap = encoder.encodeBitmap(sr?.get(idx) ?: "", BarcodeFormat.QR_CODE, 800, 800)
        val qrImage: ImageView = findViewById(R.id.qr)
        imageViewAnimatedChange(this, qrImage, bitmap, nextOrDone)
        nextOrDone.visibility = View.GONE
        nextOrDone.setOnClickListener {
            if (sr != null) {
                if (idx + 1 < sr.size) {
                    ++idx
                    if (idx + 1 >= sr.size) nextOrDone.text = getString(R.string.Done)
                    bitmap = encoder.encodeBitmap(sr[idx], BarcodeFormat.QR_CODE, 800, 800)
                    imageViewAnimatedChange(this, qrImage, bitmap, nextOrDone)
                } else {
                    finish()
                    nextOrDone.text = getString(R.string.Done)
                }
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.back_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun imageViewAnimatedChange(
        c: Context?,
        v: ImageView,
        new_image: Bitmap?,
        nextOrDone: Button,
        out: Int = android.R.anim.slide_out_right,
        In: Int = android.R.anim.slide_in_left,
        both: Boolean = true,

        ) {
        val animOut: Animation = AnimationUtils.loadAnimation(c, out)
        val animIn: Animation = AnimationUtils.loadAnimation(c, In)
        animOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                v.setImageBitmap(new_image)
                animIn.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationRepeat(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {}
                })
                if (both)
                    v.startAnimation(animIn)
                nextOrDone.visibility = View.VISIBLE

            }
        })
        v.startAnimation(animOut)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.back -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}