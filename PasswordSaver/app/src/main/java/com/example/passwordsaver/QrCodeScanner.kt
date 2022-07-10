package com.example.passwordsaver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException


class QrCodeScanner : AppCompatActivity() {

    private val RESULT_IMPORT_IMAGE = 2

    private val permissionStorage = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.qr_code_scanner)
        supportActionBar?.hide()
        val backCameraButton: ImageView = findViewById(R.id.back)
        val captureCameraButton: ImageView = findViewById(R.id.capture)
        val smallImage: ImageView = findViewById(R.id.smallImage)
        val barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()


        val cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true)
            .build()

        val cameraView = findViewById<SurfaceView>(R.id.surfaceView)

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
                    val intent = Intent()
                    intent.putExtra("result", barcodes?.valueAt(0)?.displayValue)
                    setResult(RESULT_IMPORT_IMAGE, intent)
                    finish()
                }
            }
        })

        backCameraButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        captureCameraButton.setOnClickListener {
            imageViewAnimatedChange(
                this,
                captureCameraButton,
                AppCompatResources.getDrawable(this, R.mipmap.ic_capture_color_foreground)
                    ?.let { it1 ->
                        getBitmapFromDrawable(it1)
                    }
            )
            cameraSource.takePicture(null) {

                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                val myFrame = Frame.Builder().setBitmap(bitmap).build()
                imageViewAnimatedChange(
                    this,
                    smallImage,
                    bitmap,
                    android.R.anim.slide_in_left,
                    android.R.anim.slide_out_right,
                    false
                )
                Handler(Looper.getMainLooper()).postDelayed({
                    imageViewAnimatedChange(
                        this,
                        smallImage,
                        null,
                        android.R.anim.slide_out_right,
                        android.R.anim.slide_in_left
                    )
                }, 5000)
                val barcodes = barcodeDetector.detect(myFrame)
                if (barcodes.size() != 0) {
                    val intent = Intent()
                    intent.putExtra("result", barcodes.valueAt(0).displayValue)
                    setResult(RESULT_IMPORT_IMAGE, intent)
                    finish()
                }
                Toast.makeText(this, "There is no codes here 😂", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getBitmapFromDrawable(drawable: Drawable): Bitmap {
        val bmp = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    private fun imageViewAnimatedChange(
        c: Context?,
        v: ImageView,
        new_image: Bitmap?,
        out: Int = android.R.anim.fade_out,
        In: Int = android.R.anim.fade_in,
        both: Boolean = true
    ) {
        val animOut: Animation = AnimationUtils.loadAnimation(c, out)
        val animIn: Animation = AnimationUtils.loadAnimation(c, In)
        animOut.setAnimationListener(object : AnimationListener {
            override fun onAnimationStart(animation: Animation) {}
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationEnd(animation: Animation) {
                v.setImageBitmap(new_image)
                animIn.setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationRepeat(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) {}
                })
                if (both)
                    v.startAnimation(animIn)
            }
        })
        v.startAnimation(animOut)
    }
}