package com.example.passwordsaver

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Base64
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
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.passwordsaver.database.PasswordData
import com.example.passwordsaver.database.PasswordViewModel
import com.example.passwordsaver.database.PasswordViewModelFactory
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random


@Suppress("PrivatePropertyName")
class MainActivity : AppCompatActivity(), PasswordAdapter.OnItemClicked {

    private lateinit var passwordViewModel: PasswordViewModel
    private var secretKey = ""
    private var secretIv = ""
    private val RESULT_FROM_CAM = 2
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val BAR_CODE_SIZE = 1500
    private val EXPORT_PATH = "/storage/emulated/0/Download/PasswordSaverExport"
    private val permissionStorage = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )
    private lateinit var recyclerView: RecyclerView

    private lateinit var contentResolver: ContentResolver

    private lateinit var bottomAppBar: BottomAppBar

    private lateinit var floatingActionButton: FloatingActionButton

    private fun verifyStoragePermissions(activity: Activity?) {
        val permission = ActivityCompat.checkSelfPermission(
            activity!!, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, permissionStorage, REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bottomAppBar = findViewById(R.id.bottomAppBar)
        bottomAppBar.setOnMenuItemClickListener {
            menuOptions(it)
        }
        verifyStoragePermissions(this)
        checkFolder()
        if (!checkFile()) {
            val fileOutputStream = openFileOutput("env", Context.MODE_PRIVATE)
            fileOutputStream.write("".toByteArray())
        }

        contentResolver = applicationContext.contentResolver
        var arr = readFile()
        if (arr.size == 0) {
            this.showSecretKey()
        } else {
            arr = readFile()
            secretIv = arr[0]
            secretKey = arr[1]
        }
        recyclerView = findViewById(R.id.recycle)
        val viewModelFactory = PasswordViewModelFactory(application)
        if (intent.extras?.getBoolean("copy") == true) {
            copyText(generatePasswords())
            Toast.makeText(this, "Password Generated", Toast.LENGTH_SHORT).show()
            finish()
        }
        passwordViewModel = ViewModelProvider(this, viewModelFactory)[PasswordViewModel::class.java]
        val linearLayoutManager = LinearLayoutManager(
            this, RecyclerView.VERTICAL, false
        )
        recyclerView.layoutManager = linearLayoutManager
        passwordViewModel.allPasswords.observe(this) {
            val pwd = PasswordAdapter(it, passwordViewModel)
            pwd.setOnClick(this)
            recyclerView.adapter = pwd
        }
        floatingActionButton = findViewById(R.id.add)
        floatingActionButton.setOnClickListener {
            this.showPasswordAdd()
        }
        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val adapter = recyclerView.adapter as PasswordAdapter
                val passwordData = adapter.getItem(position)
                adapter.removeAt(position)
                Snackbar.make(viewHolder.itemView, "Deleted", Snackbar.LENGTH_LONG)
                    .setAnchorView(floatingActionButton).apply {
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
                    passwordViewModel.getAllPasswordsList(), fromPosition, toPosition
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
            this, "$username password copied", Toast.LENGTH_SHORT
        ).show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun menuOptions(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.Import -> {
                val items = arrayOf("Camera", "File")
                createMenuDialogs(
                    items,
                    this::importOkFunction,
                    resources.getString(R.string.ImportFrom),
                    resources.getString(R.string.Import)
                )
            }

            R.id.export -> {
                // Export all the username and passwords
                val items = arrayOf("Qr Code", "File")
                createMenuDialogs(
                    items,
                    this::exportOkFunction,
                    resources.getString(R.string.ExportTo),
                    resources.getString(R.string.export)
                )
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
                    this, "Generated Password and copied to clipboard", Toast.LENGTH_LONG
                ).show()
            }

            R.id.share -> {
                val file = File("$EXPORT_PATH/export.pas")
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
                val today = LocalDate.now()
                val lastModified =
                    Date(file.lastModified()).toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDate()
                val period = Period.between(today, lastModified)
                if (!file.exists() || period.days > 1) file.writeBytes(
                    "${
                        getSHA512(secretKey).subSequence(
                            0, 5
                        )
                    }${exportPwdHelper()}".toByteArray()
                )
                val uri = FileProvider.getUriForFile(
                    this, this.applicationContext.packageName + ".provider", file
                )
                val intent = Intent(Intent.ACTION_SEND)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun importOkFunction(checkedItem: Int) {
        when (checkedItem) {
            0 -> {
                val intent = Intent(this, QrCodeScanner::class.java)
                startForResult.launch(intent)
            }

            1 -> {
                openFileExplorer()
            }
        }
    }

    private fun exportPwdHelper(): String {
        val lst = passwordViewModel.getAllPasswordsList()
        if (lst.size == 0) {
            Toast.makeText(this, "There is no passwords to export!!", Toast.LENGTH_SHORT).show()
            return ""
        }
        var txt = ""
        for (i in 0 until lst.size) {
            txt += if (i != lst.size - 1) "${lst[i].username}:${lst[i].password}," else "${lst[i].username}:${lst[i].password}"
        }
        return txt.encryptCBC()!!
    }

    private fun exportHelper() {
        val encrypted = exportPwdHelper()
        if (encrypted.isNotEmpty() && encrypted.isNotBlank()) {
            val list = ArrayList<String>()
            var idx = 0
            var count = encrypted.length / BAR_CODE_SIZE
            var end: Int
            while (idx < encrypted.length) {
                end = if (idx + BAR_CODE_SIZE > encrypted.length) encrypted.length
                else idx + BAR_CODE_SIZE
                val s = "${count--}${encrypted.subSequence(idx, end)}"
                list.add(s)
                idx += BAR_CODE_SIZE
            }
            val intent = Intent(this, Export::class.java)
            intent.putStringArrayListExtra("qr", list)
            startActivity(intent)
        }
    }

    private fun exportOkFunction(checkedItem: Int) {
        when (checkedItem) {
            0 -> {
                exportHelper()
            }

            1 -> {
                try {
                    val encryptedPasswords = exportPwdHelper()
                    if (encryptedPasswords.isNotEmpty() && encryptedPasswords.isNotBlank()) {
                        val exportFile = File("$EXPORT_PATH/export.pas")
                        exportFile.writeBytes(
                            "${
                                getSHA512(secretKey).subSequence(
                                    0, 5
                                )
                            }$encryptedPasswords".toByteArray()
                        )
                        Snackbar.make(
                            recyclerView, "Passwords Exported", Snackbar.LENGTH_LONG
                        ).setAnchorView(floatingActionButton).apply {
                            setAction("OPEN") {
                                val intent = Intent(Intent.ACTION_VIEW)
                                val myDir: Uri = FileProvider.getUriForFile(
                                    context,
                                    context.applicationContext.packageName + ".provider",
                                    File(EXPORT_PATH)
                                )
                                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                intent.setDataAndType(
                                    myDir, DocumentsContract.Document.MIME_TYPE_DIR
                                )
                                context.startActivity(intent)
                            }
                            setActionTextColor(Color.RED)
                        }.show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "There was some error in exporting", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    private fun createMenuDialogs(
        items: Array<String>,
        okFunction: (checkedItem: Int) -> Unit,
        title: String,
        positiveBtn: String
    ) {
        var checkedItem = 0
        MaterialAlertDialogBuilder(this).setTitle(title)
            .setNeutralButton(resources.getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }.setPositiveButton(positiveBtn) { dialog, _ ->
                okFunction(checkedItem)
                dialog.dismiss()
            }.setSingleChoiceItems(items, checkedItem) { _, which ->
                checkedItem = which
            }.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun askForPasswordForDeletion() {
        val dialog = Dialog(this)
        dialog.setTitle("Password Required")
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.request_pwd)

        val secret = dialog.findViewById<EditText>(R.id.secret)
        val cancel: Button = dialog.findViewById(R.id.no)
        val delete: Button = dialog.findViewById(R.id.yes)
        cancel.setOnClickListener {
            dialog.dismiss()
        }
        delete.setOnClickListener {
            val text: String = secret.text.toString()
            if (text.isEmpty() || text.isBlank()) {
                Toast.makeText(
                    this, "Enter Secret Key", Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val txt = getSHA512(text).subSequence(0, 32)
            if (txt == secretIv + secretKey) {
                try {
                    val exportFile = File("$EXPORT_PATH/db_backup.pas")
                    exportFile.writeBytes(
                        "${
                            getSHA512(secretKey).subSequence(
                                0, 5
                            )
                        }${exportPwdHelper()}".toByteArray()
                    )
                    this.scheduleForDeletion(exportFile.toPath())
                    Toast.makeText(
                        this,
                        "Passwords has been backed up to db_backup.pas ! It will be only for 3 hours",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    passwordViewModel.clear()
                } catch (e: Exception) {
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "The secret key is wrong", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showClearDatabaseAlert() {
        MaterialAlertDialogBuilder(this).setTitle("Hostile Operation").setCancelable(false)
            .setMessage("Please make sure you are doing the right thing.It can never be undone")
            .setPositiveButton(resources.getText(R.string.yes)) { _, _ -> askForPasswordForDeletion() }
            .setNegativeButton(resources.getText(R.string.no)) { _, _ -> return@setNegativeButton }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun generatePasswords(): String {
        return getSHA512(
            LocalDateTime.now().toString()
        ).substring(0, 33) + "!@#$%^&*()_-+=\\/<>?~`"[Random.nextInt(0, 16)]
    }


    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
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
                    this, "${text.size} passwords imported successfully", Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this, "There is no codes here 😂", Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(this, "There is no codes here 😂", Toast.LENGTH_LONG).show()
        }

    }


    @Throws(IOException::class)
    private fun readTextFromUri(uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    stringBuilder.append(line)
                    line = reader.readLine()
                }
            }
        }
        return stringBuilder.toString()
    }

    private fun Uri.getName(context: Context): String? {
        val returnCursor = context.contentResolver.query(this, null, null, null, null)
        val nameIndex = returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor?.moveToFirst()
        val fileName = nameIndex?.let { returnCursor.getString(it) }
        returnCursor?.close()
        return fileName
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleForDeletion(path: Path, delay: Long = 3L) {
        val futures: MutableMap<Path, ScheduledFuture<*>> = HashMap<Path, ScheduledFuture<*>>()
        futures[path] =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors()).schedule({
                try {
                    Files.delete(path)
                } catch (e: IOException) {
                    // failed to delete
                    Toast.makeText(this, "Error deleting backup file", Toast.LENGTH_SHORT).show()
                }
            }, delay, TimeUnit.HOURS)
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            when (result.resultCode) {
                Activity.RESULT_OK -> {
                    val uri = result.data?.data
                    val fileArray = uri?.getName(this)?.split(".")
                    val extension = fileArray?.get(fileArray.size - 1)

                    if (uri != null && extension == "pas") {
                        val fullString = readTextFromUri(uri)
                        val secretKeyCheck = fullString.subSequence(0, 5)
                        if (secretKeyCheck == getSHA512(secretKey).subSequence(0, 5)) {
                            pushImportData(fullString.substring(5))
                        } else {
                            Toast.makeText(
                                this, "This file is not supported for your KEY", Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(this, "Please upload the .pas file 🤬", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

                RESULT_FROM_CAM -> {
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
        val encrypted = cipher.doFinal(this.toByteArray())
        val encodedByte = Base64.encode(encrypted, Base64.DEFAULT)
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
                        username.text.toString(), it1
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

    private fun checkFolder() {
        val appDirectory = File(EXPORT_PATH)
        if (appDirectory.isDirectory && !appDirectory.exists()) {
            appDirectory.mkdirs()
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