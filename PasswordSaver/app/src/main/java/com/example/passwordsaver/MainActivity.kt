package com.example.passwordsaver

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.passwordsaver.database.PasswordData
import com.example.passwordsaver.database.PasswordViewModel
import com.example.passwordsaver.database.PasswordViewModelFactory
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.ArrayList
import kotlin.random.Random


class MainActivity : AppCompatActivity(), PasswordAdapter.OnItemClicked {

    private lateinit var passwordViewModel: PasswordViewModel
    private var secretKey = ""
    private var secretIv = ""
    private val RESULT_IMPORT_IMAGE = 2
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val permissionStorage = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    private fun verifyStoragePermissions(activity: Activity?) {
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                permissionStorage,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        verifyStoragePermissions(this)
        if (!checkFile()) {
            val fileOutputStream = openFileOutput("env", Context.MODE_PRIVATE)
            fileOutputStream.write("".toByteArray())
        }
        var arr = readFile()

        if (arr.size == 0) {
            this.showSecretKey()
        } else {
            arr = readFile()
            secretIv = arr[0]
            secretKey = arr[1]
        }
        val recyclerView = findViewById<RecyclerView>(R.id.recycle)
        val viewModelFactory = PasswordViewModelFactory(application)
        if (intent.extras?.getBoolean("copy") == true) {
            copyText(generatePasswords())
            Toast.makeText(this, "Password Generated", Toast.LENGTH_SHORT).show()
            finish()
        }
        passwordViewModel =
            ViewModelProvider(this, viewModelFactory).get(PasswordViewModel::class.java)
        val linearLayoutManager = LinearLayoutManager(
            this, RecyclerView.VERTICAL, false
        )
        recyclerView.layoutManager = linearLayoutManager
        passwordViewModel.allPasswords.observe(this) {
            val pwd = PasswordAdapter(it, passwordViewModel)
            pwd.setOnClick(this)
            recyclerView.adapter = pwd
        }
        val floatingActionButton: FloatingActionButton = findViewById(R.id.add)
        floatingActionButton.setOnClickListener {
            this.showPasswordAdd()
        }
        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val adapter = recyclerView.adapter as PasswordAdapter
                val passwordData = adapter.getItem(position)
                adapter.removeAt(position)
                Snackbar.make(viewHolder.itemView, "Deleted", Snackbar.LENGTH_LONG).apply {
                    setAction("UNDO") {
                        //pass
                    }
                    setActionTextColor(Color.RED)
                    addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                        override fun onShown(transientBottomBar: Snackbar?) {
                            super.onShown(transientBottomBar)
                        }

                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            if (event == Snackbar.Callback.DISMISS_EVENT_ACTION) {
                                adapter.restore(position, passwordData)
                            } else {
                                adapter.permanentRemove(passwordData, position)
                            }
                            super.onDismissed(transientBottomBar, event)
                        }
                    })
                }.show()

            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                Collections.swap(
                    passwordViewModel.getAllPasswordsList(),
                    fromPosition,
                    toPosition
                )
                recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)
                recyclerView.clearFocus()
                return super.onMove(recyclerView, viewHolder, target)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }


    private fun copyText(text: String) {
        val myClipboard = this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val myClip: ClipData = ClipData.newPlainText("Password", text)
        myClipboard.setPrimaryClip(myClip)
    }

    override fun onItemClick(position: Int, username: String) {
        passwordViewModel.getPassword(username).decryptCBC()?.let { copyText(it) }
        Toast.makeText(
            this,
            "$username password copied",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.Import -> {
                val intent = Intent(this, QrCodeScanner::class.java)
                startForResult.launch(intent)
            }
            R.id.Import_Using_Image -> {
                openGalleryForImage()
            }
            R.id.export -> {
                // Export all the username and passwords
                val lst = passwordViewModel.getAllPasswordsList()
                if (lst.size == 0) {
                    Toast.makeText(this, "There is no passwords to export!!", Toast.LENGTH_SHORT)
                        .show()
                    return false
                }
                var txt = ""
                for (i in 0 until lst.size) {
                    txt += if (i == lst.size - 1) "${lst[i].username}:${lst[i].password}" else "${lst[i].username}:${lst[i].password},"
                }

                val intent = Intent(this, Export::class.java)
                intent.putExtra("qr", txt.encryptCBC())
                startActivity(intent)
            }

            R.id.clear -> {
                showClearDatabaseAlert()
            }

            R.id.generate -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    copyText(generatePasswords())
                } else {
                    Toast.makeText(this, "Error in generating passwords", Toast.LENGTH_SHORT).show()
                    return false
                }

                Toast.makeText(
                    this,
                    "Generated Password and copied to clipboard",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun showClearDatabaseAlert() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Hostile Operation")
            .setCancelable(false)
            .setMessage("Please make sure you are doing the right thing.It can never be undone")
            .setPositiveButton(resources.getText(R.string.yes)) { _, _ -> passwordViewModel.clear() }
            .setNegativeButton(resources.getText(R.string.no)) { _, _ -> return@setNegativeButton }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generatePasswords(): String {
        return getSHA512(
            LocalDateTime.now().toString()
        ).substring(0, 33) + "!@#$%^&*()_-+=\\/<>?~`"[Random.nextInt(0, 16)]
    }

    private fun googleBarcode(myQRCode: Bitmap): String? {
        val barcodeDetector = BarcodeDetector.Builder(this)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()

        val myFrame = Frame.Builder()
            .setBitmap(myQRCode)
            .build()

        val barcodes = barcodeDetector.detect(myFrame)
        if (barcodes.size() != 0)
            return barcodes.valueAt(0).displayValue

        return null
    }


    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startForResult.launch(intent)
    }


    private fun pushImportData(result: String?) {
        if (result != null) {
            val text = result.decryptCBC()?.split(",")
            if (text != null) {
                for (i in text.indices) {
                    val tmp = text[i].split(":")
                    passwordViewModel.insert(PasswordData(tmp[0], tmp[1]))
                }
                Toast.makeText(
                    this,
                    "${text.size} passwords imported successfully",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "There is no codes here 😂",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else {
            Toast.makeText(this, "There was some error in importing image", Toast.LENGTH_LONG)
                .show()
        }

    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val uriPathHelper = URIPathHelper()
                    val path = result.data?.data?.let { uriPathHelper.getPath(this, it) }
                    pushImportData(googleBarcode(BitmapFactory.decodeFile(path)))
                }
                RESULT_IMPORT_IMAGE -> {
                    pushImportData(result.data?.extras?.getString("result"))
                }
                RESULT_CANCELED -> {
                    Toast.makeText(this@MainActivity, "Cancelled", Toast.LENGTH_LONG).show()
                }
            }

        }

    private fun getSHA512(input: String): String {
        val md: MessageDigest = MessageDigest.getInstance("SHA-512")
        val messageDigest = md.digest(input.toByteArray())
        val no = BigInteger(1, messageDigest)
        var hashtext: String = no.toString(16)
        while (hashtext.length < 128) {
            hashtext = "0$hashtext"
        }
        return hashtext
    }

    private fun String.encryptCBC(): String? {
        if (secretIv.isEmpty() || secretKey.isEmpty()) return null
        val iv = IvParameterSpec(secretIv.toByteArray())
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv)
        val crypted = cipher.doFinal(this.toByteArray())
        val encodedByte = Base64.encode(crypted, Base64.DEFAULT)
        return String(encodedByte)
    }

    private fun String.decryptCBC(): String? {
        if (secretIv.isEmpty() || secretKey.isEmpty()) return null
        val decodedByte: ByteArray = Base64.decode(this, Base64.DEFAULT)
        val iv = IvParameterSpec(secretIv.toByteArray())
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv)
        val output = cipher.doFinal(decodedByte)
        return String(output)
    }


    private fun Context.showPasswordAdd() {
        val dialog = Dialog(this)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.add_passwords)
        val username = dialog.findViewById<EditText>(R.id.new_username)
        val password = dialog.findViewById<EditText>(R.id.new_password)
        val add = dialog.findViewById<Button>(R.id.new_add)
        val cancel = dialog.findViewById<Button>(R.id.cancel)

        add.setOnClickListener {
            if (username.text.toString().isEmpty() || username.text.toString().trim()
                    .isEmpty() || password.text.isEmpty()
            ) {
                Toast.makeText(this, "please fill all the fields", Toast.LENGTH_SHORT).show()
            } else {
                password.text.toString().encryptCBC()?.let { it1 ->
                    PasswordData(
                        username.text.toString(),
                        it1
                    )
                }?.let { it2 ->
                    passwordViewModel.insert(
                        it2
                    )
                }
                Toast.makeText(this, "Password Saved", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        cancel.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

    }

    private fun Context.showSecretKey() {
        val dialog = Dialog(this)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.popup)
        val add = dialog.findViewById<Button>(R.id.set)
        val secret = dialog.findViewById<EditText>(R.id.secret)

        add.setOnClickListener {
            if (secret.text.isEmpty()) {
                Toast.makeText(this, "Please fill the secret key", Toast.LENGTH_SHORT).show()
            } else {
                val txt = getSHA512(secret.text.toString())
                Toast.makeText(this, txt, Toast.LENGTH_SHORT).show()
                val fileOutputStream = openFileOutput("env", Context.MODE_PRIVATE)
                fileOutputStream.write(
                    "${txt.subSequence(0, 16)}\n${
                        txt.subSequence(16, 32)
                    }".toByteArray()
                )
                val arr = readFile()
                secretIv = arr[0]
                secretKey = arr[1]
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun checkFile(): Boolean {
        return try {
            openFileInput("env")
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun readFile(): ArrayList<String> {
        val fileInputStream = openFileInput("env")
        val inputStreamReader = InputStreamReader(fileInputStream)
        val bufferedReader = BufferedReader(inputStreamReader)
        val arr: ArrayList<String> = ArrayList()
        var text: String?
        while (run {
                text = bufferedReader.readLine()
                text
            } != null) {
            text?.let { arr.add(it) }
        }
        return arr
    }

}