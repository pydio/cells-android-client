package com.pydio.android.cells.db

import androidx.annotation.Keep
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.pydio.cells.api.ServerURL
import com.pydio.cells.transport.ServerURLImpl
import java.util.Properties

/** Enable serialization / deserialization to store complex objects in the DB using JSON */
class CellsConverters {
    private val gsonInstance: Gson = GsonBuilder().create()

    @TypeConverter
    @Keep
    fun toProperties(value: String): Properties {
        val newType = TypeToken.get(Properties::class.javaObjectType)
        return gsonInstance.fromJson(value, newType)
    }

    @TypeConverter
    fun fromProperties(meta: Properties): String {
        return gsonInstance.toJson(meta)
    }

    @TypeConverter
    fun fromServerURL(url: ServerURL): String {
        return url.toJson()
    }

    @TypeConverter
    @Keep
    fun toServerURL(value: String): ServerURL {
        return ServerURLImpl.fromJson(value)
    }
}
