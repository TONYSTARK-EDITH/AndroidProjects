package com.example.passwordsaver.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PasswordDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(pwd: PasswordData)

    @Delete
    fun delete(pwd: PasswordData)

    @Query("SELECT password from password_data_base where username = :username")
    fun getPassword(username: String): String

    @Query("SELECT * FROM password_data_base")
    fun getAllList(): MutableList<PasswordData>

    @Query("DELETE FROM password_data_base")
    fun clear()

    @Query("SELECT * FROM password_data_base")
    fun get(): LiveData<MutableList<PasswordData>>

}