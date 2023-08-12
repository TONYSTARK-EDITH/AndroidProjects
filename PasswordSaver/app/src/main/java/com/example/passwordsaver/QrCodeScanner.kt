package com.example.passwordsaver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException


@Suppress("PrivatePropertyName")
class QrCodeScanner : AppCompatActivity() {

    private val RESULT_FROM_CAM = 2

    private val permissionStorage = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qr_code_scanner)
        supportActionBar?.hide()
        val barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()


        val cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true)
            .build()

        val cameraView = findViewById<SurfaceView>(R.id.surfaceView)
        var totalResult = ""
        val qrStack = ArrayList<Int>()

        cameraView.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("Range")
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    if (ActivityCompat.checkSelfPermission(
                            this@QrCodeScanner,
                            Manifest.permission.CAMERA
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            this@QrCodeScanner,
                            permissionStorage,
                            RESULT_OK
                        )
                        return
                    }
                    cameraSource.start(cameraView.holder)
                } catch (ie: IOException) {
                    return
                }
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })



        barcodeDetector.setProcessor(object : Detector.Processor<Barcode?> {
            override fun release() {}
            override fun receiveDetections(detections: Detector.Detections<Barcode?>?) {
                val barcodes: SparseArray<Barcode?>? = detections!!.detectedItems

                if (barcodes?.size() != 0) {
                    val input = barcodes?.valueAt(0)?.displayValue
                    val idx = input?.get(0)?.digitToInt()
                    val value = input?.substring(1)
                    if (!qrStack.contains(idx)) {
                        totalResult += value
                        if (idx != null) {
                            qrStack.add(idx)
                        }
                        if (idx == 0) {
                            val intent = Intent()
                            intent.putExtra("result", totalResult)
                            setResult(RESULT_FROM_CAM, intent)
                            finish()
                        } else {
                            createToast()
                        }
                    }


                }
            }
        })


    }

    private fun createToast(msg: String = "Scan the next QR 😁") {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}