package com.example.passwordsaver.database

import android.app.Application
import androidx.lifecycle.ViewModel

class PasswordViewModel(application: Application) : ViewModel() {
    private val db: PasswordDatabase = PasswordDatabase.getInstance(application)
    internal val allPasswords = db.passwordDAO().get()

    fun insert(password: PasswordData) {
        db.passwordDAO().insert(password)
    }

    fun getAllPasswordsList(): MutableList<PasswordData> {
        return db.passwordDAO().getAllList()
    }

    fun getPassword(username: String): String {
        return db.passwordDAO().getPassword(username)
    }

    fun delete(password: PasswordData) {
        db.passwordDAO().delete(password)
    }

    fun clear() {
        db.passwordDAO().clear()
    }


}