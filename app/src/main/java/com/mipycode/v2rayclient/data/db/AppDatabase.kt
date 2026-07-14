package com.mipycode.v2rayclient.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.mipycode.v2rayclient.data.model.ProtocolType
import com.mipycode.v2rayclient.data.model.ServerConfig

class Converters {
    @TypeConverter
    fun fromProtocol(value: ProtocolType): String = value.name

    @TypeConverter
    fun toProtocol(value: String): ProtocolType = ProtocolType.valueOf(value)
}

@Database(entities = [ServerConfig::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "v2ray_client.db"
                ).build().also { INSTANCE = it }
            }
    }
}
