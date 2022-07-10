package com.example.passwordsaver

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class Export : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.show_qr)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.back_menu, menu)
        val extras = intent.extras!!
        val qr = extras.getString("qr")
        val encoder = BarcodeEncoder()
        val bitmap = encoder.encodeBitmap(qr, BarcodeFormat.QR_CODE, 800, 800)
        val qrImage: ImageView = findViewById(R.id.qr)
        qrImage.setImageBitmap(bitmap)
        return super.onCreateOptionsMenu(menu)
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