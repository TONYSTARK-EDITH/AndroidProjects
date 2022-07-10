package com.example.passwordsaver.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "password_data_base")
data class PasswordData(
    @PrimaryKey val username: String,
    @ColumnInfo(name = "password") val password: String

)
