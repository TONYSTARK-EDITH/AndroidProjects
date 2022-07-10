package com.example.passwordsaver.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PasswordData::class], version = 1, exportSchema = false)
abstract class PasswordDatabase : RoomDatabase() {

    abstract fun passwordDAO(): PasswordDAO

    companion object {
        private var INSTANCE: PasswordDatabase? = null

        fun getInstance(context: Context): PasswordDatabase {
            var instance = INSTANCE
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PasswordDatabase::class.java,
                    "password_data_base"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
            }
            return instance
        }

    }


}